package com.infosys.subsidy.service;

import com.infosys.subsidy.dto.ChatbotRequest;
import com.infosys.subsidy.dto.ChatbotResponse;
import com.infosys.subsidy.entity.*;
import com.infosys.subsidy.enums.ApplicationStatus;
import com.infosys.subsidy.enums.SchemeStatus;
import com.infosys.subsidy.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ChatbotService {

    private final UserRepository userRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final SchemeRepository schemeRepository;
    private final ApplicationRepository applicationRepository;
    private final SchemeRequiredDocumentRepository documentRepository;
    private final DisbursementPlanRepository planRepository;
    private final FundReleaseRepository fundReleaseRepository;

    private static class ConversationContext {
        public String pendingIntent;
        public String selectedEntityId;
        public long lastInteractionTime;

        public ConversationContext(String pendingIntent) {
            this.pendingIntent = pendingIntent;
            this.lastInteractionTime = System.currentTimeMillis();
        }
    }

    private final Map<String, ConversationContext> activeSessions = new ConcurrentHashMap<>();
    private static final long SESSION_TIMEOUT_MS = 5 * 60 * 1000; // 5 minutes

    public ChatbotService(
            UserRepository userRepository,
            BeneficiaryRepository beneficiaryRepository,
            SchemeRepository schemeRepository,
            ApplicationRepository applicationRepository,
            SchemeRequiredDocumentRepository documentRepository,
            DisbursementPlanRepository planRepository,
            FundReleaseRepository fundReleaseRepository) {
        this.userRepository = userRepository;
        this.beneficiaryRepository = beneficiaryRepository;
        this.schemeRepository = schemeRepository;
        this.applicationRepository = applicationRepository;
        this.documentRepository = documentRepository;
        this.planRepository = planRepository;
        this.fundReleaseRepository = fundReleaseRepository;
    }

    private void cleanupSessions() {
        long now = System.currentTimeMillis();
        activeSessions.entrySet().removeIf(entry -> (now - entry.getValue().lastInteractionTime) > SESSION_TIMEOUT_MS);
        if (activeSessions.size() > 1000) {
            activeSessions.clear(); // Safe eviction if overloaded
        }
    }

    @Transactional(readOnly = true)
    public ChatbotResponse processMessage(ChatbotRequest request, String email) {
        cleanupSessions();

        String rawMsg = request.getMessage().trim();
        String msg = rawMsg.toLowerCase().replaceAll("\\s+", " "); // normalize space

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return new ChatbotResponse("Your user account could not be found.");
        }
        Beneficiary beneficiary = beneficiaryRepository.findByUserId(user.getId()).orElse(null);
        if (beneficiary == null) {
            return new ChatbotResponse("Your beneficiary profile is incomplete. Please complete your profile to continue.", "PROFILE_MISSING");
        }

        ConversationContext ctx = activeSessions.getOrDefault(email, null);

        // 1. FAQ Intent (no context needed)
        ChatbotResponse faqReply = handleFAQ(msg);
        if (faqReply != null) {
            activeSessions.remove(email); // Clear context
            return faqReply;
        }

        // Entities extracted
        String appNum = extractApplicationNumber(rawMsg);
        Scheme matchedScheme = findSchemeInMessage(rawMsg);

        // 2. Resolve via Context if pending
        if (ctx != null) {
             if ("ELIGIBILITY_AND_DOCUMENTS".equals(ctx.pendingIntent) || "ELIGIBILITY".equals(ctx.pendingIntent) || "DOCUMENTS".equals(ctx.pendingIntent)) {
                if (matchedScheme != null) {
                    activeSessions.remove(email);
                    if ("ELIGIBILITY_AND_DOCUMENTS".equals(ctx.pendingIntent)) return handleEligibilityAndDocuments(matchedScheme);
                    if ("ELIGIBILITY".equals(ctx.pendingIntent)) return handleEligibility(matchedScheme);
                    if ("DOCUMENTS".equals(ctx.pendingIntent)) return handleDocuments(matchedScheme);
                }
            } else if ("MY_APPLICATION_DETAILS".equals(ctx.pendingIntent) || "MILESTONES_DETAILS".equals(ctx.pendingIntent)) {
                if (appNum != null) {
                    activeSessions.remove(email);
                    if ("MY_APPLICATION_DETAILS".equals(ctx.pendingIntent)) return handleSpecificApplication(appNum, beneficiary);
                    if ("MILESTONES_DETAILS".equals(ctx.pendingIntent)) return handleSpecificMilestone(appNum, beneficiary);
                }
            }
        }

        // 3. New Intents Detection
        if (matchesRegex(msg, ".*(my application|show my applications|status of|what happened to|is my application|which applications have i submitted|why was my application).*")) {
            return routeApplicationStatus(appNum, beneficiary, email);
        }

        if (matchesRegex(msg, ".*(milestone|grant|release|installments|fund|money|disbursement|utilization).*")) {
            return routeMilestones(appNum, beneficiary, email);
        }

        if (matchesRegex(msg, ".*(both|eligibility and document|document and eligibility|documents and eligibility|eligibility and documents|i want to know both|show both).*") || (ctx != null && "both".equals(msg))) {
            if (matchedScheme == null) {
                activeSessions.put(email, new ConversationContext("ELIGIBILITY_AND_DOCUMENTS"));
                return new ChatbotResponse("Which scheme would you like to check? Please specify the scheme name or code.", "CLARIFY_SCHEME");
            }
            return handleEligibilityAndDocuments(matchedScheme);
        }

        if (matchesRegex(msg, ".*(eligibility|criteria|rules|qualify|conditions).*")) {
            if (matchedScheme == null) {
                activeSessions.put(email, new ConversationContext("ELIGIBILITY"));
                return new ChatbotResponse("Which scheme's eligibility criteria would you like to know about? Please specify the scheme name.", "CLARIFY_SCHEME");
            }
            return handleEligibility(matchedScheme);
        }

        if (matchesRegex(msg, ".*(document|upload|proof|require|missing).*")) {
            if (matchedScheme == null) {
                activeSessions.put(email, new ConversationContext("DOCUMENTS"));
                return new ChatbotResponse("Which scheme's required documents would you like to know about? Please specify the scheme name.", "CLARIFY_SCHEME");
            }
            return handleDocuments(matchedScheme);
        }

        if (matchesRegex(msg, ".*(available|programs|what schemes).*")) {
            return handleSchemes();
        }
        
        // 4. Standalone entity matched (Implicit summary requested)
        if (appNum != null) {
            return handleSpecificApplication(appNum, beneficiary);
        }

        if (matchedScheme != null) {
            List<Scheme> allMatches = schemeRepository.findByStatus(SchemeStatus.ACTIVE).stream().filter(s -> msg.contains(s.getSchemeName().toLowerCase()) || (s.getSchemeCode() != null && msg.contains(s.getSchemeCode().toLowerCase()))).collect(Collectors.toList());
            if (allMatches.size() > 1 && matchedScheme.getSchemeCode() != null) {
                 return new ChatbotResponse("I found multiple schemes matching your request. Please specify using the exact Scheme Code (e.g., SCH-XXXXX).", "SCHEME_MULTIPLE");
            }
            
            return new ChatbotResponse("The scheme **" + matchedScheme.getSchemeName() + "** provides subsidies up to ₹" + matchedScheme.getGrantAmount() + ". " +
                    "Applicable for " + (matchedScheme.getApplicableRegion() != null ? matchedScheme.getApplicableRegion() : "all regions") + ". " +
                    "Would you like to know about its 'eligibility criteria', 'required documents', or 'both'?", "SCHEME_INFO");
        }

        return new ChatbotResponse("I'm sorry, I couldn't understand your question. I can help with information about available schemes, your application status, eligibility criteria, required documents, and grant milestones. Please try rephrasing your question.", "FALLBACK");
    }

    private boolean matchesRegex(String text, String regex) {
        return Pattern.compile(regex).matcher(text).matches();
    }

    private String extractApplicationNumber(String msg) {
        Matcher m = Pattern.compile("(?i)APP-\\d+").matcher(msg);
        if (m.find()) return m.group().toUpperCase();
        return null;
    }

    private Scheme findSchemeInMessage(String msg) {
        List<Scheme> activeSchemes = schemeRepository.findByStatus(SchemeStatus.ACTIVE);
        String loweredArgs = msg.toLowerCase();
        for (Scheme s : activeSchemes) {
            if ((s.getSchemeName() != null && loweredArgs.contains(s.getSchemeName().toLowerCase())) ||
                (s.getSchemeCode() != null && loweredArgs.contains(s.getSchemeCode().toLowerCase()))) {
                return s;
            }
        }
        return null;
    }

    private ChatbotResponse handleFAQ(String msg) {
        if (msg.contains("how to apply") || msg.contains("how do i apply")) {
            return new ChatbotResponse("To apply, go to the 'Available Schemes' page, select the scheme you qualify for, and click 'Apply'. You will be guided through an eligibility check and document upload wizard.", "FAQ");
        }
        if (msg.contains("contact") || msg.contains("support")) {
            return new ChatbotResponse("For specialized support, please email support@digitalsubsidy.gov.in or contact the local grant office in your region.", "FAQ");
        }
        if (msg.contains("login") || msg.contains("register")) {
            return new ChatbotResponse("You are already logged in to the dashboard! To manage your profile, use the top-right menu.", "FAQ");
        }
        return null;
    }

    private ChatbotResponse routeApplicationStatus(String appNum, Beneficiary beneficiary, String email) {
        if (appNum != null) {
            return handleSpecificApplication(appNum, beneficiary);
        }
        List<Application> apps = applicationRepository.findByBeneficiaryId(beneficiary.getId());
        if (apps.isEmpty()) {
            return new ChatbotResponse("You haven't applied to any schemes yet. Check the 'Available Schemes' tab to get started.", "MY_APPLICATION_EMPTY");
        }
        if (apps.size() == 1) {
            return formatApplicationResponse(apps.get(0));
        }

        // Return summary of all mapped applications
        StringBuilder sb = new StringBuilder("You have " + apps.size() + " applications:\n");
        for (Application a : apps) {
            String scName = schemeRepository.findById(a.getSchemeId()).map(Scheme::getSchemeName).orElse("Unknown Scheme");
            sb.append(String.format("• %s for %s (%s)\n", "APP-" + String.format("%05d", a.getId()), scName, mapReadableStatus(a.getStatus())));
        }
        sb.append("\nWhich application would you like to know more about? Details like APP-XXXXX.");
        activeSessions.put(email, new ConversationContext("MY_APPLICATION_DETAILS"));
        
        return new ChatbotResponse(sb.toString().trim(), "MY_APPLICATION_MULTIPLE");
    }

    private ChatbotResponse handleSpecificApplication(String appNum, Beneficiary beneficiary) {
        try {
            Long id = Long.parseLong(appNum.replace("APP-", ""));
            Application app = applicationRepository.findById(id).orElse(null);
            
            if (app == null || !app.getBeneficiaryId().equals(beneficiary.getId())) {
                return new ChatbotResponse(appNum + " could not be found in your applications. Please check the number.", "APPLICATION_NOT_FOUND");
            }
            return formatApplicationResponse(app);
        } catch (Exception e) {
            return new ChatbotResponse("Couldn't process application number " + appNum + ". Please contact support.", "ERROR");
        }
    }

    private String mapReadableStatus(ApplicationStatus status) {
        switch (status) {
            case SUBMITTED: return "Submitted";
            case DOCUMENTS_PENDING: return "Pending Documents";
            case ELIGIBLE: return "Eligible";
            case NOT_ELIGIBLE: return "Not Eligible";
            case PENDING_VERIFICATION:
            case UNDER_VERIFICATION: 
            case ESCALATED: return "Under Verification";
            case APPROVED: return "Approved";
            case REJECTED: return "Rejected";
            case RETURNED_TO_APPLICANT: return "Returned for Corrections";
            case GRANT_DISBURSED:
            case PLAN_CONFIGURED: return "Processing Grant";
            case MILESTONE_PENDING: return "Milestone Pending";
            case PARTIALLY_DISBURSED: return "Partially Disbursed";
            case FULLY_DISBURSED: return "Fully Disbursed";
            default: return status.name();
        }
    }

    private ChatbotResponse formatApplicationResponse(Application a) {
        String scName = schemeRepository.findById(a.getSchemeId()).map(Scheme::getSchemeName).orElse("Unknown Scheme");
        String readableStatus = mapReadableStatus(a.getStatus());
        String appNum = "APP-" + String.format("%05d", a.getId());

        StringBuilder reply = new StringBuilder("Details for **" + appNum + "** (" + scName + "):\n");
        reply.append("• Status: ").append(readableStatus).append("\n");
        
        if (a.getEligibilityScore() != null) {
            reply.append("• Eligibility Score: ").append(a.getEligibilityScore()).append("/100\n");
        }
        
        if (a.getStatus() == ApplicationStatus.NOT_ELIGIBLE || a.getStatus() == ApplicationStatus.REJECTED) {
            if (a.getRemarks() != null && !a.getRemarks().isBlank()) {
                reply.append("• Reason: ").append(a.getRemarks()).append("\n");
            }
        }
        
        if (a.getStatus() == ApplicationStatus.FULLY_DISBURSED) {
            reply.append("• Funds have been fully disbursed. Ask about your 'milestones' to see disbursement details.\n");
        } else if (a.getStatus() == ApplicationStatus.PARTIALLY_DISBURSED) {
            DisbursementPlan plan = planRepository.findByApplicationId(a.getId()).orElse(null);
            if (plan != null) {
                List<FundRelease> releases = fundReleaseRepository.findByApplicationIdOrderByReleasedAtAsc(a.getId());
                BigDecimal totalReleased = releases.stream().map(FundRelease::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                reply.append("• Scheduled Grant: ₹").append(plan.getTotalAmount()).append("\n");
                reply.append("• Released Funds: ₹").append(totalReleased).append("\n");
            }
            reply.append("• Disbursement is currently partial. Ask about your 'milestones' for more details.\n");
        }
        return new ChatbotResponse(reply.toString().trim(), "MY_APPLICATION");
    }

    private ChatbotResponse routeMilestones(String appNum, Beneficiary beneficiary, String email) {
        if (appNum != null) {
            return handleSpecificMilestone(appNum, beneficiary);
        }
        
        List<Application> apps = applicationRepository.findByBeneficiaryId(beneficiary.getId()).stream()
                .filter(a -> Arrays.asList(ApplicationStatus.APPROVED, ApplicationStatus.GRANT_DISBURSED, 
                                           ApplicationStatus.PLAN_CONFIGURED, ApplicationStatus.PARTIALLY_DISBURSED, 
                                           ApplicationStatus.FULLY_DISBURSED)
                                   .contains(a.getStatus()))
                .collect(Collectors.toList());

        if (apps.isEmpty()) {
            return new ChatbotResponse("No grant milestones were found. You don't have any fully approved or active grants.", "NO_MILESTONES");
        }

        if (apps.size() == 1) {
            return handleSpecificMilestone("APP-" + String.format("%05d", apps.get(0).getId()), beneficiary);
        }

        // Multiple approved apps
        StringBuilder sb = new StringBuilder("You have milestones for " + apps.size() + " applicable applications:\n");
        for (Application a : apps) {
            String scName = schemeRepository.findById(a.getSchemeId()).map(Scheme::getSchemeName).orElse("Unknown Scheme");
            sb.append(String.format("• %s for %s\n", "APP-" + String.format("%05d", a.getId()), scName));
        }
        sb.append("\nWhich application's grant milestones would you like to know about? Mention the APP-XXXXX.");
        activeSessions.put(email, new ConversationContext("MILESTONES_DETAILS"));
        
        return new ChatbotResponse(sb.toString().trim(), "MILESTONE_MULTIPLE");
    }

    private ChatbotResponse handleSpecificMilestone(String appNum, Beneficiary beneficiary) {
        try {
            Long id = Long.parseLong(appNum.replace("APP-", ""));
            Application app = applicationRepository.findById(id).orElse(null);
            
            if (app == null || !app.getBeneficiaryId().equals(beneficiary.getId())) {
                return new ChatbotResponse(appNum + " could not be found in your applications. Please check the number.", "APPLICATION_NOT_FOUND");
            }
            
            DisbursementPlan plan = planRepository.findByApplicationId(app.getId()).orElse(null);
            if (plan == null) {
                return new ChatbotResponse("Your application " + appNum + " doesn't have a generated disbursement plan yet. Milestones are created after grant officer processing.", "NO_PLAN");
            }
            
            List<FundRelease> releases = fundReleaseRepository.findByApplicationIdOrderByReleasedAtAsc(app.getId());
            BigDecimal totalReleased = releases.stream().map(FundRelease::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            
            StringBuilder reply = new StringBuilder("Grant milestones for **" + appNum + "**:\n\n");
            reply.append("• Total scheduled: ₹").append(plan.getTotalAmount()).append("\n");
            reply.append("• Total released: ₹").append(totalReleased).append("\n");
            
            BigDecimal remaining = plan.getTotalAmount().subtract(totalReleased);
            if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                reply.append("• Remaining not yet released: ₹").append(remaining).append("\n");
            } else if (remaining.compareTo(BigDecimal.ZERO) < 0) {
                reply.append("• Notice: The total released amount exceeds the scheduled plan. Please contact your grant officer.\n");
            }
            reply.append("\n");

            if (app.getStatus() == ApplicationStatus.FULLY_DISBURSED) {
                reply.append("🎉 All milestones have been completed and funds fully disbursed for this application.\n");
            }

            List<DisbursementMilestone> sortedMilestones = new ArrayList<>(plan.getMilestones());
            sortedMilestones.sort(Comparator.comparingInt(DisbursementMilestone::getMilestoneNumber));

            for (DisbursementMilestone m : sortedMilestones) {
                String statusStr = m.getStatus() == com.infosys.subsidy.enums.MilestoneStatus.COMPLIANCE_SUBMITTED 
                                   ? "Compliance submitted" 
                                   : m.getStatus().name().charAt(0) + m.getStatus().name().substring(1).toLowerCase().replace("_", " ");
                
                reply.append(String.format("%d. Stage %d — %s: ₹%s — %s", m.getMilestoneNumber(), m.getMilestoneNumber(), m.getMilestoneName(), m.getScheduledAmount(), statusStr));
                
                if (m.getStatus() == com.infosys.subsidy.enums.MilestoneStatus.DUE || m.getStatus() == com.infosys.subsidy.enums.MilestoneStatus.PENDING) {
                    if (m.getComplianceType() != null) {
                        String req = m.getComplianceType().name();
                        if (!"NONE".equalsIgnoreCase(req) && !req.isBlank()) {
                            reply.append("\n  ↳ Action Required: Submit ").append(req);
                        }
                    }
                }
                reply.append("\n");
            }

            return new ChatbotResponse(reply.toString().trim(), "MILESTONES");
            
        } catch (Exception e) {
            return new ChatbotResponse("Couldn't process milestone check for " + appNum + ". Please try again.", "ERROR");
        }
    }

    private ChatbotResponse handleSchemes() {
        List<Scheme> activeSchemes = schemeRepository.findByStatus(SchemeStatus.ACTIVE);
        if (activeSchemes.isEmpty()) {
            return new ChatbotResponse("Currently, there are no active schemes accepting applications.");
        }
        
        String schemeNames = activeSchemes.stream().map(Scheme::getSchemeName).collect(Collectors.joining("\n• "));
        return new ChatbotResponse("Here are the currently active schemes:\n• " + schemeNames + "\nLet me know if you want to know about the eligibility or documents for a specific scheme.", "SCHEMES");
    }

    private ChatbotResponse handleEligibilityAndDocuments(Scheme matchedScheme) {
        ChatbotResponse eligResponse = handleEligibility(matchedScheme);
        ChatbotResponse docResponse = handleDocuments(matchedScheme);

        StringBuilder sb = new StringBuilder();
        sb.append(eligResponse.getReply()).append("\n\n").append(docResponse.getReply());
        
        return new ChatbotResponse(sb.toString().trim(), "ELIGIBILITY_AND_DOCUMENTS");
    }

    private String translateOperator(com.infosys.subsidy.enums.Operator operator, String expectedVal) {
        if (operator == null) return "must match";
        switch (operator) {
            case GREATER_THAN_EQUAL: return "must be at least";
            case LESS_THAN_EQUAL: return "must not exceed";
            case GREATER_THAN: return "must be strictly greater than";
            case LESS_THAN: return "must be strictly less than";
            case IN: return "must be one of";
            case EQUAL: 
                if ("true".equalsIgnoreCase(expectedVal)) return "must be confirmed";
                if ("false".equalsIgnoreCase(expectedVal)) return "must not be present";
                return "must be";
            default: return "must match";
        }
    }

    private String formatExpectedValue(String value, String criterionName) {
        if (value == null || value.isBlank()) return value;
        String lowerName = criterionName != null ? criterionName.toLowerCase() : "";
        
        if (lowerName.contains("income") || lowerName.contains("grant") || lowerName.contains("revenue") || lowerName.contains("amount") || lowerName.contains("cost") || lowerName.contains("price") || lowerName.contains("worth") || lowerName.contains("salary")) {
            try {
                if (value.matches("\\d+(\\.\\d+)?")) {
                    BigDecimal num = new BigDecimal(value);
                    java.text.NumberFormat format = java.text.NumberFormat.getCurrencyInstance(Locale.of("en", "IN"));
                    String formatted = format.format(num).replace(".00", "");
                    // Ensure the symbol is tightly packed without extra spaces which can happen in Java localized currencies
                    return formatted.replaceAll("(?<=\\D)\\s*", "");
                }
            } catch (Exception ignored) {}
        }
        
        if (lowerName.contains("percentage") || lowerName.contains("rate") || lowerName.contains("ratio")) {
            try {
                if (value.matches("\\d+(\\.\\d+)?")) {
                    return value + "%";
                }
            } catch (Exception ignored) {}
        }
        
        return value;
    }

    private ChatbotResponse handleEligibility(Scheme matchedScheme) {
        List<EligibilityCriteria> criteria = matchedScheme.getCriteriaList();
        if (criteria == null || criteria.isEmpty()) {
            return new ChatbotResponse("There are no specific eligibility rules for **" + matchedScheme.getSchemeName() + "**.");
        }
        
        StringBuilder reply = new StringBuilder("The eligibility criteria for **" + matchedScheme.getSchemeName() + "** include:\n");
        for (EligibilityCriteria c : criteria) {
            String mandText = c.isMandatory() ? "(Mandatory)" : "(Optional)";
            String readableOp = translateOperator(c.getOperator(), c.getExpectedValue());
            String formattedValue = formatExpectedValue(c.getExpectedValue(), c.getCriterionName());
            
            reply.append("• ").append(c.getCriterionName()).append(" ").append(readableOp).append(" ").append(formattedValue).append(" ").append(mandText).append("\n");
        }
        
        return new ChatbotResponse(reply.toString().trim(), "ELIGIBILITY");
    }

    private ChatbotResponse handleDocuments(Scheme matchedScheme) {
        List<SchemeRequiredDocument> docs = documentRepository.findBySchemeIdAndActiveTrue(matchedScheme.getId());
        if (docs == null || docs.isEmpty()) {
            return new ChatbotResponse("There are no required documents explicitly specified for " + matchedScheme.getSchemeName() + ".");
        }
        
        String mand = docs.stream().filter(SchemeRequiredDocument::isMandatory).map(SchemeRequiredDocument::getDocumentName).collect(Collectors.joining(", "));
        String opt = docs.stream().filter(d -> !d.isMandatory()).map(SchemeRequiredDocument::getDocumentName).collect(Collectors.joining(", "));
        
        StringBuilder reply = new StringBuilder("Documents for **" + matchedScheme.getSchemeName() + "**:\n");
        if (!mand.isEmpty()) {
            reply.append("• **Mandatory**: ").append(mand).append("\n");
        }
        if (!opt.isEmpty()) {
            reply.append("• **Optional**: ").append(opt).append("\n");
        }
        return new ChatbotResponse(reply.toString().trim(), "DOCUMENTS");
    }
}
