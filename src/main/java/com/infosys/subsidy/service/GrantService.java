package com.infosys.subsidy.service;

import com.infosys.subsidy.dto.*;
import com.infosys.subsidy.entity.*;
import com.infosys.subsidy.enums.*;
import com.infosys.subsidy.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class GrantService {

    private final GrantDisbursementRepository grantDisbursementRepository;
    private final ApplicationRepository applicationRepository;
    private final SchemeRepository schemeRepository;
    private final UserRepository userRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    
    private final DisbursementPlanRepository planRepository;
    private final DisbursementMilestoneRepository milestoneRepository;
    private final FundReleaseRepository fundReleaseRepository;
    private final MilestoneComplianceService complianceService;

    public GrantService(GrantDisbursementRepository grantDisbursementRepository,
                        ApplicationRepository applicationRepository,
                        SchemeRepository schemeRepository,
                        UserRepository userRepository,
                        BeneficiaryRepository beneficiaryRepository,
                        DisbursementPlanRepository planRepository,
                        DisbursementMilestoneRepository milestoneRepository,
                        FundReleaseRepository fundReleaseRepository,
                        MilestoneComplianceService complianceService) {
        this.grantDisbursementRepository = grantDisbursementRepository;
        this.applicationRepository = applicationRepository;
        this.schemeRepository = schemeRepository;
        this.userRepository = userRepository;
        this.beneficiaryRepository = beneficiaryRepository;
        this.planRepository = planRepository;
        this.milestoneRepository = milestoneRepository;
        this.fundReleaseRepository = fundReleaseRepository;
        this.complianceService = complianceService;
    }

    // LEGACY ENDPOINT METHOD
    @Transactional
    public GrantDisbursement disburseGrant(Long applicationId, String officerEmail) {
        User officer = userRepository.findByEmail(officerEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Officer not found"));

        if (officer.getRole() != UserRole.GRANT_OFFICER && officer.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only Grant Officers can disburse funds.");
        }

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));

        if (application.getStatus() != ApplicationStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Application must be APPROVED to disburse funds. Current status: " + application.getStatus());
        }

        if (grantDisbursementRepository.existsByApplicationId(applicationId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Funds have already been disbursed for this application.");
        }

        Scheme scheme = schemeRepository.findByIdWithLock(application.getSchemeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Scheme not found"));

        BigDecimal grantAmount = scheme.getGrantAmount();
        BigDecimal remainingBudget = scheme.getRemainingBudget();

        if (remainingBudget.compareTo(grantAmount) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient scheme budget.");
        }

        application.setStatus(ApplicationStatus.GRANT_DISBURSED);
        applicationRepository.save(application);

        scheme.setDisbursedAmount(scheme.getDisbursedAmount().add(grantAmount));
        schemeRepository.save(scheme);

        GrantDisbursement disbursement = new GrantDisbursement();
        disbursement.setApplicationId(applicationId);
        disbursement.setSchemeId(scheme.getId());
        disbursement.setBeneficiaryId(application.getBeneficiaryId());
        disbursement.setGrantAmount(grantAmount);
        disbursement.setDisbursedBy(officer.getId());
        disbursement.setDisbursedAt(LocalDateTime.now());
        disbursement.setTransactionReference("TRX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());

        return grantDisbursementRepository.save(disbursement);
    }
    
    // NEW STAGED DISBURSEMENT LOGIC
    @Transactional
    public DisbursementPlan createDisbursementPlan(Long applicationId, PlanRequestDTO request, String officerEmail) {
        User officer = userRepository.findByEmail(officerEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Officer not found"));

        if (officer.getRole() != UserRole.GRANT_OFFICER && officer.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only Grant Officers can configure plans.");
        }

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));

        if (application.getStatus() != ApplicationStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Application must be APPROVED.");
        }

        if (planRepository.existsByApplicationId(applicationId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An active disbursement plan already exists for this application.");
        }

        Scheme scheme = schemeRepository.findById(application.getSchemeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Scheme not found"));

        BigDecimal requestedTotal = request.getMilestones().stream()
                .map(MilestoneRequestDTO::getScheduledAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        if (requestedTotal.compareTo(scheme.getGrantAmount()) != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Plan total must exactly equal approved grant.");
        }

        if (request.getMilestones().size() == 3) {
            BigDecimal approved = scheme.getGrantAmount();
            BigDecimal expectedStage1 = approved.multiply(new BigDecimal("0.40")).setScale(2, java.math.RoundingMode.HALF_UP).stripTrailingZeros();
            BigDecimal expectedStage2 = approved.multiply(new BigDecimal("0.30")).setScale(2, java.math.RoundingMode.HALF_UP).stripTrailingZeros();
            BigDecimal expectedStage3 = approved.subtract(expectedStage1).subtract(expectedStage2);

            MilestoneRequestDTO m1 = request.getMilestones().get(0);
            MilestoneRequestDTO m2 = request.getMilestones().get(1);
            MilestoneRequestDTO m3 = request.getMilestones().get(2);

            boolean amountsMatch = m1.getScheduledAmount().compareTo(expectedStage1) == 0 &&
                                   m2.getScheduledAmount().compareTo(expectedStage2) == 0 &&
                                   m3.getScheduledAmount().compareTo(expectedStage3) == 0;
                                   
            boolean offsetsMatch = (m1.getDueDateOffsetDays() != null && m1.getDueDateOffsetDays() == 10) &&
                                   (m2.getDueDateOffsetDays() != null && m2.getDueDateOffsetDays() == 30) &&
                                   (m3.getDueDateOffsetDays() != null && m3.getDueDateOffsetDays() == 60);

            if (!amountsMatch || !offsetsMatch) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Submitted configuration diverges from the required standard plan of 40/30/30 with 10/30/60 offsets.");
            }
            if (m1.getComplianceType() != MilestoneComplianceType.DOCUMENTATION ||
                m2.getComplianceType() != MilestoneComplianceType.UTILIZATION_PROOF ||
                m3.getComplianceType() != MilestoneComplianceType.NONE) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Submitted plan compliance types diverge from the standard requirement.");
            }
        }


        DisbursementPlan plan = new DisbursementPlan();
        plan.setApplicationId(applicationId);
        plan.setTotalAmount(scheme.getGrantAmount());
        plan.setStatus(PlanStatus.ACTIVE);
        plan.setCreatedBy(officer.getId());
        plan.setCreatedAt(LocalDateTime.now());

        List<DisbursementMilestone> entities = new ArrayList<>();
        LocalDateTime baseDate = application.getVerificationCompletedAt();
        if (baseDate == null) {
            baseDate = application.getApplicationDate();
        }
        if (baseDate == null) {
            baseDate = LocalDateTime.now();
        }

        for (MilestoneRequestDTO mDto : request.getMilestones()) {
            DisbursementMilestone milestone = new DisbursementMilestone();
            milestone.setPlan(plan);
            milestone.setMilestoneNumber(mDto.getMilestoneNumber());
            milestone.setMilestoneName(mDto.getMilestoneName());
            milestone.setDescription(mDto.getDescription());
            milestone.setComplianceType(mDto.getComplianceType());
            milestone.setScheduledAmount(mDto.getScheduledAmount());
            
            Integer offsetDays = mDto.getDueDateOffsetDays() != null ? mDto.getDueDateOffsetDays() : 0;
            milestone.setDueDateOffsetDays(offsetDays);
            milestone.setDueDate(baseDate.plusDays(offsetDays));
            milestone.setStatus(MilestoneStatus.PENDING);
            entities.add(milestone);
        }

        plan.setMilestones(entities);
        DisbursementPlan savedPlan = planRepository.save(plan);

        application.setStatus(ApplicationStatus.PLAN_CONFIGURED);
        applicationRepository.save(application);

        // Auto-check docs
        for (DisbursementMilestone m : savedPlan.getMilestones()) {
            if (m.getComplianceType() == MilestoneComplianceType.DOCUMENTATION) {
                complianceService.evaluateDocumentationCompliance(m, applicationId, scheme.getId());
            }
        }

        return savedPlan;
    }

    @Transactional
    public void nukePlanAndResetApplication(Long applicationId) {
        fundReleaseRepository.deleteAll(fundReleaseRepository.findByApplicationIdOrderByReleasedAtAsc(applicationId));
        planRepository.findByApplicationId(applicationId).ifPresent(plan -> {
            milestoneRepository.deleteAll(plan.getMilestones());
            planRepository.delete(plan);
        });
        Application app = applicationRepository.findById(applicationId).orElseThrow();
        app.setStatus(ApplicationStatus.APPROVED);
        applicationRepository.save(app);
    }

    @Transactional
    public FundRelease releaseMilestone(Long applicationId, Long milestoneId, String officerEmail) {
        User officer = userRepository.findByEmail(officerEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Officer not found"));

        if (officer.getRole() != UserRole.GRANT_OFFICER && officer.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only Grant Officers can release funds.");
        }

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));

        DisbursementPlan plan = planRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found"));

        if (plan.getStatus() == PlanStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Plan is already completed.");
        }

        DisbursementMilestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Milestone not found"));

        if (!milestone.getPlan().getId().equals(plan.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Milestone does not belong to application plan.");
        }

        List<DisbursementMilestone> sortedMilestones = plan.getMilestones().stream()
                .sorted(java.util.Comparator.comparing(DisbursementMilestone::getMilestoneNumber))
                .collect(Collectors.toList());

        for (DisbursementMilestone m : sortedMilestones) {
            if (m.getMilestoneNumber() < milestone.getMilestoneNumber()) {
                if (m.getStatus() != MilestoneStatus.RELEASED) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Previous milestone must be completed and released before the final milestone can be released.");
                }
            }
        }

        if (milestone.getStatus() == MilestoneStatus.RELEASED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Milestone is already released.");
        }
        
        if (milestone.getComplianceType() != MilestoneComplianceType.NONE && milestone.getStatus() != MilestoneStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Milestone compliance must be COMPLETED before release. Current status: " + milestone.getStatus());
        }

        if (fundReleaseRepository.existsByMilestoneId(milestoneId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate release blocked.");
        }

        List<FundRelease> priorReleases = fundReleaseRepository.findByApplicationIdOrderByReleasedAtAsc(applicationId);
        BigDecimal currentReleasedTotal = priorReleases.stream().map(FundRelease::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        if (currentReleasedTotal.add(milestone.getScheduledAmount()).compareTo(plan.getTotalAmount()) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Release would exceed approved grant.");
        }

        Scheme scheme = schemeRepository.findByIdWithLock(application.getSchemeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Scheme not found"));

        BigDecimal remainingBudget = scheme.getRemainingBudget();
        if (remainingBudget.compareTo(milestone.getScheduledAmount()) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient scheme budget.");
        }

        // Apply release
        scheme.setDisbursedAmount(scheme.getDisbursedAmount().add(milestone.getScheduledAmount()));
        schemeRepository.save(scheme);

        milestone.setStatus(MilestoneStatus.RELEASED);
        milestoneRepository.save(milestone);

        FundRelease release = new FundRelease();
        release.setMilestoneId(milestoneId);
        release.setApplicationId(applicationId);
        release.setAmount(milestone.getScheduledAmount());
        release.setReleasedBy(officer.getId());
        release.setReleasedAt(LocalDateTime.now());
        release.setStatus("SUCCESS");
        release.setTransactionReference("FR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        release.setRemarks("Milestone fund released");
        FundRelease savedRelease = fundReleaseRepository.save(release);

        BigDecimal newReleasedTotal = currentReleasedTotal.add(milestone.getScheduledAmount());
        if (newReleasedTotal.compareTo(plan.getTotalAmount()) == 0) {
            application.setStatus(ApplicationStatus.FULLY_DISBURSED);
            plan.setStatus(PlanStatus.COMPLETED);
            plan.setCompletedAt(LocalDateTime.now());
            planRepository.save(plan);
        } else {
            application.setStatus(ApplicationStatus.PARTIALLY_DISBURSED);
        }
        applicationRepository.save(application);

        return savedRelease;
    }

    @Transactional(readOnly = true)
    public List<FundRelease> getAllFundReleases() {
        return fundReleaseRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<com.infosys.subsidy.dto.RegionalAnalyticsDTO> getRegionalAnalytics() {
        List<com.infosys.subsidy.entity.Application> allApps = applicationRepository.findAll();
        List<com.infosys.subsidy.entity.Beneficiary> allBens = beneficiaryRepository.findAll();
        List<com.infosys.subsidy.entity.DisbursementPlan> allPlans = planRepository.findAll();
        List<com.infosys.subsidy.entity.FundRelease> allFRs = fundReleaseRepository.findAll();
        List<com.infosys.subsidy.entity.Scheme> allSchemes = schemeRepository.findAll();

        java.util.Map<Long, java.math.BigDecimal> schemeGrantMap = new java.util.HashMap<>();
        for (com.infosys.subsidy.entity.Scheme s : allSchemes) {
            schemeGrantMap.put(s.getId(), s.getGrantAmount());
        }

        java.util.Map<Long, String> benRegionMap = new java.util.HashMap<>();
        for (com.infosys.subsidy.entity.Beneficiary b : allBens) {
            benRegionMap.put(b.getId(), b.getRegion() != null && !b.getRegion().trim().isEmpty() ? b.getRegion() : "Unknown / Not Specified");
        }

        java.util.Map<Long, com.infosys.subsidy.entity.DisbursementPlan> planMap = new java.util.HashMap<>();
        for (com.infosys.subsidy.entity.DisbursementPlan p : allPlans) {
            planMap.put(p.getApplicationId(), p);
        }

        java.util.Map<Long, java.math.BigDecimal> appReleasedMap = new java.util.HashMap<>();
        for (com.infosys.subsidy.entity.FundRelease fr : allFRs) {
            if ("SUCCESS".equals(fr.getStatus())) {
                appReleasedMap.put(fr.getApplicationId(), appReleasedMap.getOrDefault(fr.getApplicationId(), java.math.BigDecimal.ZERO).add(fr.getAmount()));
            }
        }
        
        java.util.Map<String, com.infosys.subsidy.dto.RegionalAnalyticsDTO> regionStats = new java.util.HashMap<>();

        for (com.infosys.subsidy.entity.Application app : allApps) {
            if (app.getStatus() == com.infosys.subsidy.enums.ApplicationStatus.REJECTED || 
                app.getStatus() == com.infosys.subsidy.enums.ApplicationStatus.SUBMITTED ||
                app.getStatus() == com.infosys.subsidy.enums.ApplicationStatus.DOCUMENTS_PENDING ||
                app.getStatus() == com.infosys.subsidy.enums.ApplicationStatus.ELIGIBLE ||
                app.getStatus() == com.infosys.subsidy.enums.ApplicationStatus.NOT_ELIGIBLE ||
                app.getStatus() == com.infosys.subsidy.enums.ApplicationStatus.PENDING_VERIFICATION ||
                app.getStatus() == com.infosys.subsidy.enums.ApplicationStatus.UNDER_VERIFICATION ||
                app.getStatus() == com.infosys.subsidy.enums.ApplicationStatus.ESCALATED ||
                app.getStatus() == com.infosys.subsidy.enums.ApplicationStatus.RETURNED_TO_APPLICANT) {
                continue;
            }

            String region = benRegionMap.getOrDefault(app.getBeneficiaryId(), "Unknown / Not Specified");
            com.infosys.subsidy.dto.RegionalAnalyticsDTO dto = regionStats.computeIfAbsent(region, r -> {
                com.infosys.subsidy.dto.RegionalAnalyticsDTO newDto = new com.infosys.subsidy.dto.RegionalAnalyticsDTO();
                newDto.setRegion(r);
                return newDto;
            });

            dto.incrementApplicationCount();

            com.infosys.subsidy.entity.DisbursementPlan plan = planMap.get(app.getId());
            if (plan != null) {
                dto.addApprovedAmount(plan.getTotalAmount());
                dto.addPlannedAmount(plan.getTotalAmount());
            } else {
                dto.addApprovedAmount(schemeGrantMap.getOrDefault(app.getSchemeId(), java.math.BigDecimal.ZERO));
            }

            java.math.BigDecimal released = appReleasedMap.getOrDefault(app.getId(), java.math.BigDecimal.ZERO);
            dto.addReleasedAmount(released);

            switch(app.getStatus()) {
                case FULLY_DISBURSED: dto.incrementFullyDisbursedCount(); break;
                case PARTIALLY_DISBURSED: dto.incrementPartiallyDisbursedCount(); break;
                case NON_COMPLIANT: dto.incrementNonCompliantCount(); break;
                case MILESTONE_OVERDUE: dto.incrementOverdueCount(); break;
                default: dto.incrementPendingCount(); break;
            }
        }

        List<com.infosys.subsidy.dto.RegionalAnalyticsDTO> resultList = new java.util.ArrayList<>(regionStats.values());
        for (com.infosys.subsidy.dto.RegionalAnalyticsDTO result : resultList) {
            result.calculateDerived(); // Auto calc remaining & utilization
        }
        
        resultList.sort((a, b) -> b.getApprovedAmount().compareTo(a.getApprovedAmount()));
        return resultList;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getNonCompliantMilestones() {
        List<DisbursementMilestone> nonCompliant = milestoneRepository.findAll().stream()
                .filter(m -> m.getStatus() == MilestoneStatus.NON_COMPLIANT)
                .collect(Collectors.toList());
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (DisbursementMilestone m : nonCompliant) {
            DisbursementPlan p = m.getPlan();
            Application app = applicationRepository.findById(p.getApplicationId()).orElse(null);
            if (app == null) continue;
            Beneficiary b = beneficiaryRepository.findById(app.getBeneficiaryId()).orElse(null);
            Scheme s = schemeRepository.findById(app.getSchemeId()).orElse(null);
            
            Map<String, Object> map = new HashMap<>();
            map.put("applicationId", app.getId());
            map.put("applicationReference", String.format("APP-%04d", app.getId()));
            map.put("beneficiaryName", b != null ? b.getName() : "Unknown");
            map.put("schemeName", s != null ? s.getSchemeName() : "Unknown");
            map.put("milestoneId", m.getId());
            map.put("milestoneName", m.getMilestoneName());
            map.put("milestoneNumber", m.getMilestoneNumber());
            map.put("amount", m.getScheduledAmount());
            map.put("status", m.getStatus().name());
            map.put("remarks", m.getRemarks() != null ? m.getRemarks() : "No remarks provided");
            map.put("reviewedAt", m.getComplianceVerifiedAt() != null ? m.getComplianceVerifiedAt().toString() : LocalDateTime.now().toString());
            result.add(map);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPendingComplianceMilestones() {
        List<DisbursementMilestone> pending = milestoneRepository.findAll().stream()
                .filter(m -> m.getStatus() == MilestoneStatus.COMPLIANCE_SUBMITTED 
                          || m.getStatus() == MilestoneStatus.UNDER_REVIEW 
                          || ((m.getStatus() == MilestoneStatus.PENDING || m.getStatus() == MilestoneStatus.BLOCKED) && m.getComplianceType() == com.infosys.subsidy.enums.MilestoneComplianceType.DOCUMENTATION))
                .collect(Collectors.toList());
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (DisbursementMilestone m : pending) {
            DisbursementPlan p = m.getPlan();
            Application app = applicationRepository.findById(p.getApplicationId()).orElse(null);
            if (app == null) continue;
            Beneficiary b = beneficiaryRepository.findById(app.getBeneficiaryId()).orElse(null);
            Scheme s = schemeRepository.findById(app.getSchemeId()).orElse(null);
            
            Map<String, Object> map = new HashMap<>();
            map.put("applicationId", app.getId());
            map.put("applicationReference", String.format("APP-%04d", app.getId()));
            map.put("beneficiaryName", b != null ? b.getName() : "Unknown");
            map.put("schemeName", s != null ? s.getSchemeName() : "Unknown");
            map.put("milestoneId", m.getId());
            map.put("milestoneName", m.getMilestoneName());
            map.put("milestoneNumber", m.getMilestoneNumber());
            map.put("amount", m.getScheduledAmount());
            map.put("status", m.getStatus().name());
            map.put("submittedAt", m.getUpdatedAt() != null ? m.getUpdatedAt().toString() : LocalDateTime.now().toString());
            result.add(map);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public PlanResponseDTO getDisbursementPlan(Long applicationId) {
        DisbursementPlan plan = planRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found"));
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));

        List<FundRelease> releases = fundReleaseRepository.findByApplicationIdOrderByReleasedAtAsc(applicationId);
        BigDecimal releasedTotal = releases.stream().map(FundRelease::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        PlanResponseDTO dto = new PlanResponseDTO();
        dto.setApplicationId(applicationId);
        dto.setApprovedAmount(plan.getTotalAmount());
        dto.setPlannedAmount(plan.getTotalAmount());
        dto.setReleasedAmount(releasedTotal);
        dto.setRemainingAmount(plan.getTotalAmount().subtract(releasedTotal));
        dto.setStatus(application.getStatus());

        List<MilestoneResponseDTO> milestoneDTOs = plan.getMilestones().stream().map(m -> {
            MilestoneResponseDTO mdto = new MilestoneResponseDTO();
            mdto.setId(m.getId());
            mdto.setNumber(m.getMilestoneNumber());
            mdto.setName(m.getMilestoneName());
            mdto.setDescription(m.getDescription());
            mdto.setAmount(m.getScheduledAmount());
            mdto.setDueDate(m.getDueDate());
            
            // Strong fallback check to ensure NON_COMPLIANT state never gets masked
            MilestoneStatus finalStatus = m.getStatus();
            if (m.getComplianceType() == com.infosys.subsidy.enums.MilestoneComplianceType.UTILIZATION_PROOF) {
                List<com.infosys.subsidy.entity.MilestoneComplianceEvidence> allEv = complianceService.getEvidenceForMilestone(m.getId());
                boolean hasRejected = allEv.stream().anyMatch(ev -> "REJECTED".equalsIgnoreCase(ev.getStatus()));
                boolean hasPendingOrVerified = allEv.stream().anyMatch(ev -> 
                    "SUBMITTED".equalsIgnoreCase(ev.getStatus()) || 
                    "UNDER_REVIEW".equalsIgnoreCase(ev.getStatus()) || 
                    "VERIFIED".equalsIgnoreCase(ev.getStatus()) ||
                    "COMPLETED".equalsIgnoreCase(ev.getStatus())
                );
                
                // If there's a rejection, BUT no new/active submissions, force it to NON_COMPLIANT.
                if (hasRejected && !hasPendingOrVerified) {
                    finalStatus = MilestoneStatus.NON_COMPLIANT;
                }
            }
            mdto.setStatus(finalStatus);
            
            mdto.setComplianceType(m.getComplianceType());
            mdto.setComplianceVerifiedAt(m.getComplianceVerifiedAt());
            mdto.setRemarks(m.getRemarks());
            return mdto;
        }).collect(Collectors.toList());

        dto.setMilestones(milestoneDTOs);
        return dto;
    }

    @Transactional(readOnly = true)
    public List<PlanSummaryDTO> getAllDisbursementPlans() {
        List<DisbursementPlan> plans = planRepository.findAll();
        List<PlanSummaryDTO> summaries = new ArrayList<>();
        
        for (DisbursementPlan plan : plans) {
            Application application = applicationRepository.findById(plan.getApplicationId()).orElse(null);
            if (application == null) continue;
            
            Scheme scheme = schemeRepository.findById(application.getSchemeId()).orElse(null);
            Beneficiary beneficiary = beneficiaryRepository.findById(application.getBeneficiaryId()).orElse(null);
            
            List<FundRelease> releases = fundReleaseRepository.findByApplicationIdOrderByReleasedAtAsc(application.getId());
            BigDecimal releasedTotal = releases.stream().map(FundRelease::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            
            PlanSummaryDTO dto = new PlanSummaryDTO();
            dto.setApplicationId(application.getId());
            dto.setApplicationReference(String.format("APP-%04d", application.getId()));
            dto.setBeneficiaryName(beneficiary != null ? beneficiary.getName() : "Unknown");
            dto.setSchemeName(scheme != null ? scheme.getSchemeName() : "Unknown");
            dto.setSchemeCode(scheme != null ? scheme.getSchemeCode() : "Unknown");
            
            dto.setApprovedAmount(plan.getTotalAmount());
            dto.setPlannedAmount(plan.getTotalAmount());
            dto.setReleasedAmount(releasedTotal);
            dto.setRemainingAmount(plan.getTotalAmount().subtract(releasedTotal));
            dto.setStatus(application.getStatus());
            
            int total = plan.getMilestones().size();
            int completed = 0;
            int pending = 0;
            int overdue = 0;
            
            DisbursementMilestone pendingNext = null;
            
            for (DisbursementMilestone m : plan.getMilestones()) {
                if (m.getStatus() == MilestoneStatus.COMPLETED || m.getStatus() == MilestoneStatus.RELEASED) {
                    completed++;
                } else if (m.getStatus() == MilestoneStatus.OVERDUE || m.getStatus() == MilestoneStatus.NON_COMPLIANT) {
                    overdue++;
                    if (pendingNext == null) pendingNext = m;
                } else {
                    pending++;
                    if (pendingNext == null) pendingNext = m;
                }
            }
            
            dto.setMilestoneCount(total);
            dto.setCompletedMilestones(completed);
            dto.setPendingMilestones(pending);
            dto.setOverdueMilestones(overdue);
            
            if (pendingNext != null) {
                dto.setNextMilestone(pendingNext.getMilestoneName());
                dto.setNextMilestoneDueDate(pendingNext.getDueDate());
            } else {
                dto.setNextMilestone("None");
                dto.setNextMilestoneDueDate(null);
            }
            
            int progress = (plan.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) ?
                    releasedTotal.multiply(new BigDecimal(100)).divide(plan.getTotalAmount(), java.math.RoundingMode.HALF_UP).intValue() : 0;
            dto.setProgressPercentage(progress);
            
            summaries.add(dto);
        }
        
        return summaries;
    }
    
    // EXISTING UTILITY METHODS
    @Transactional(readOnly = true)
    public java.util.List<OfficerQueueItemDTO> getPendingGrants() {
        java.util.List<Application> pending = applicationRepository.findByStatusIn(java.util.Arrays.asList(
            ApplicationStatus.APPROVED,
            ApplicationStatus.PLAN_CONFIGURED,
            ApplicationStatus.MILESTONE_PENDING,
            ApplicationStatus.PARTIALLY_DISBURSED,
            ApplicationStatus.MILESTONE_OVERDUE,
            ApplicationStatus.NON_COMPLIANT
        ));
        return pending.stream().map(app -> {
            String beneficiaryName = beneficiaryRepository.findById(app.getBeneficiaryId())
                    .map(Beneficiary::getName).orElse("Unknown Beneficiary");
            String schemeName = schemeRepository.findById(app.getSchemeId())
                    .map(Scheme::getSchemeName).orElse("Unknown Scheme");
            
            return new OfficerQueueItemDTO(
                    app.getId(), app.getApplicationDate(), app.getStatus(), 
                    app.getCurrentVerificationLevel(), app.getVerificationRoute(), 
                    app.getEligibilityScore(), beneficiaryName, schemeName, 
                    app.getVerificationAssignedAt(), app.getVerificationDueDate(), app.getRemarks());
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ApplicationDetailDTO getGrantApplicationDetails(Long applicationId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
        Beneficiary beneficiary = beneficiaryRepository.findById(application.getBeneficiaryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Beneficiary not found"));
        Scheme scheme = schemeRepository.findById(application.getSchemeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Scheme not found"));

        ApplicationDetailDTO dto = new ApplicationDetailDTO();
        dto.setApplicationId(application.getId());
        dto.setStatus(application.getStatus());
        dto.setCurrentVerificationLevel(application.getCurrentVerificationLevel());
        dto.setVerificationRoute(application.getVerificationRoute());
        dto.setEligibilityScore(application.getEligibilityScore());
        dto.setApplicationDate(application.getApplicationDate());
        
        dto.setBeneficiaryName(beneficiary.getName());
        dto.setMobileNumber(beneficiary.getMobileNumber());
        dto.setEmail(beneficiary.getEmail());
        String maskedAadhaar = "XXXX-XXXX-" + beneficiary.getAadhaarNumber().substring(Math.max(0, beneficiary.getAadhaarNumber().length() - 4));
        dto.setMaskedAadhaar(maskedAadhaar);
        
        dto.setBankAccountHolderName(beneficiary.getBankAccountHolderName());
        dto.setBankName(beneficiary.getBankName());
        dto.setBankAccountNumber(beneficiary.getBankAccountNumber());
        dto.setBankIfscCode(beneficiary.getBankIfscCode());
        dto.setBankAccountType(beneficiary.getBankAccountType());
        
        dto.setSchemeName(scheme.getSchemeName());
        dto.setSchemeCode(scheme.getSchemeCode());
        return dto;
    }
}
