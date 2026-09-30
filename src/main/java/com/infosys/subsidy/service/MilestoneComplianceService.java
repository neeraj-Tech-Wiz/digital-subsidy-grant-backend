package com.infosys.subsidy.service;

import com.infosys.subsidy.entity.ApplicationDocument;
import com.infosys.subsidy.entity.DisbursementMilestone;
import com.infosys.subsidy.entity.SchemeRequiredDocument;
import com.infosys.subsidy.entity.User;
import com.infosys.subsidy.enums.DocumentStatus;
import com.infosys.subsidy.enums.MilestoneComplianceType;
import com.infosys.subsidy.enums.MilestoneStatus;
import com.infosys.subsidy.enums.UserRole;
import com.infosys.subsidy.repository.ApplicationDocumentRepository;
import com.infosys.subsidy.repository.DisbursementMilestoneRepository;
import com.infosys.subsidy.repository.SchemeRequiredDocumentRepository;
import com.infosys.subsidy.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MilestoneComplianceService {

    private final DisbursementMilestoneRepository milestoneRepository;
    private final ApplicationDocumentRepository applicationDocumentRepository;
    private final SchemeRequiredDocumentRepository schemeRequiredDocumentRepository;
    private final UserRepository userRepository;
    private final DocumentStorageService documentStorageService;
    private final com.infosys.subsidy.repository.MilestoneComplianceEvidenceRepository evidenceRepository;
    private final com.infosys.subsidy.repository.BeneficiaryRepository beneficiaryRepository;
    private final com.infosys.subsidy.repository.ApplicationRepository applicationRepository;

    public MilestoneComplianceService(DisbursementMilestoneRepository milestoneRepository,
                                      ApplicationDocumentRepository applicationDocumentRepository,
                                      SchemeRequiredDocumentRepository schemeRequiredDocumentRepository,
                                      UserRepository userRepository,
                                      DocumentStorageService documentStorageService,
                                      com.infosys.subsidy.repository.MilestoneComplianceEvidenceRepository evidenceRepository,
                                      com.infosys.subsidy.repository.BeneficiaryRepository beneficiaryRepository,
                                      com.infosys.subsidy.repository.ApplicationRepository applicationRepository) {
        this.milestoneRepository = milestoneRepository;
        this.applicationDocumentRepository = applicationDocumentRepository;
        this.schemeRequiredDocumentRepository = schemeRequiredDocumentRepository;
        this.userRepository = userRepository;
        this.documentStorageService = documentStorageService;
        this.evidenceRepository = evidenceRepository;
        this.beneficiaryRepository = beneficiaryRepository;
        this.applicationRepository = applicationRepository;
    }

    @Transactional
    public void evaluateDocumentationCompliance(DisbursementMilestone milestone, Long applicationId, Long schemeId) {
        if (milestone.getComplianceType() != MilestoneComplianceType.DOCUMENTATION) {
            return;
        }

        List<SchemeRequiredDocument> requiredDocs = schemeRequiredDocumentRepository.findBySchemeIdAndActiveTrue(schemeId);
        List<ApplicationDocument> appDocs = applicationDocumentRepository.findByApplicationId(applicationId);

        boolean allVerified = true;
        String blockReason = "Documentation compliance blocked. Some mandatory documents are missing or not verified.";

        for (SchemeRequiredDocument requiredDoc : requiredDocs) {
            if (!requiredDoc.isMandatory()) continue;

            Optional<ApplicationDocument> match = appDocs.stream()
                    .filter(doc -> doc.getDocumentType() == requiredDoc.getDocumentType())
                    .findFirst();

            if (match.isEmpty() || match.get().getDocumentStatus() != DocumentStatus.VERIFIED) {
                allVerified = false;
                break;
            }
        }

        if (allVerified) {
            // Additional Bank Details Check for Stage 1 Disbursement
            com.infosys.subsidy.entity.Application application = applicationRepository.findById(applicationId).orElse(null);
            if (application != null) {
                com.infosys.subsidy.entity.Beneficiary beneficiary = beneficiaryRepository.findById(application.getBeneficiaryId()).orElse(null);
                if (beneficiary != null) {
                    Optional<ApplicationDocument> passbookDoc = appDocs.stream()
                        .filter(doc -> doc.getDocumentType() == com.infosys.subsidy.enums.DocumentType.BANK_PASSBOOK)
                        .findFirst();
                        
                    boolean bankDetailsValid = beneficiary.getBankAccountNumber() != null && !beneficiary.getBankAccountNumber().isEmpty()
                            && beneficiary.getBankIfscCode() != null && !beneficiary.getBankIfscCode().isEmpty()
                            && beneficiary.getBankAccountHolderName() != null && !beneficiary.getBankAccountHolderName().isEmpty();
                            
                    if (passbookDoc.isEmpty() || passbookDoc.get().getDocumentStatus() != DocumentStatus.VERIFIED) {
                        allVerified = false;
                        blockReason = "Stage 1 Blocked - Verified Bank Passbook document is missing.";
                    } else if (!bankDetailsValid) {
                        allVerified = false;
                        blockReason = "Stage 1 Blocked - Initial Banking Details are incomplete.";
                    }
                } else {
                    allVerified = false;
                    blockReason = "Beneficiary profile not found.";
                }
            }
        }

        if (allVerified) {
            milestone.setStatus(MilestoneStatus.COMPLETED);
            milestone.setComplianceVerifiedAt(LocalDateTime.now());
            milestone.setRemarks("Auto-verified from existing application documentation and banking verification.");
        } else {
            milestone.setStatus(MilestoneStatus.BLOCKED);
            milestone.setRemarks(blockReason);
        }

        milestoneRepository.save(milestone);
    }

    @Transactional
    public DisbursementMilestone submitComplianceEvidenceFiles(Long milestoneId, List<MultipartFile> files, List<String> evidenceTypes, String remarks, String submitterEmail) {
        User submitter = userRepository.findByEmail(submitterEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        DisbursementMilestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Milestone not found"));
                
        // BENEFICIARY OWNERSHIP VALIDATION
        if (submitter.getRole() == UserRole.BENEFICIARY) {
            com.infosys.subsidy.entity.Beneficiary beneficiary = beneficiaryRepository.findByUserId(submitter.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Beneficiary profile not found."));
            com.infosys.subsidy.entity.Application application = applicationRepository.findById(milestone.getPlan().getApplicationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found."));
            if (!beneficiary.getId().equals(application.getBeneficiaryId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this application and its milestones.");
            }
        }

        if (milestone.getComplianceType() != MilestoneComplianceType.UTILIZATION_PROOF) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only UTILIZATION_PROOF accepts file evidence.");
        }

        if (files == null || files.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No files provided.");
        }

        if (evidenceTypes == null || evidenceTypes.size() != files.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Evidence types must match files length.");
        }

        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            String type = evidenceTypes.get(i);
            String filePath = documentStorageService.storeFile(file);

            com.infosys.subsidy.entity.MilestoneComplianceEvidence ev = new com.infosys.subsidy.entity.MilestoneComplianceEvidence();
            ev.setMilestoneId(milestoneId);
            ev.setApplicationId(milestone.getPlan().getApplicationId());
            ev.setEvidenceType(type);
            ev.setFilePath(filePath);
            ev.setOriginalFileName(org.springframework.util.StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "document"));
            ev.setContentType(file.getContentType());
            ev.setFileSize(file.getSize());
            ev.setSubmittedBy(submitter.getId());
            ev.setSubmittedAt(LocalDateTime.now());
            ev.setStatus("SUBMITTED");
            evidenceRepository.save(ev);
        }

        milestone.setStatus(MilestoneStatus.COMPLIANCE_SUBMITTED);
        if (remarks != null && !remarks.isEmpty()) {
            milestone.setRemarks(remarks);
        }
        return milestoneRepository.save(milestone);
    }
    
    @Transactional
    public DisbursementMilestone submitComplianceEvidence(Long milestoneId, String remarks, String submitterEmail) {
        DisbursementMilestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Milestone not found"));

        if (milestone.getComplianceType() == MilestoneComplianceType.DOCUMENTATION) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Documentation compliance is read automatically.");
        }

        milestone.setStatus(MilestoneStatus.COMPLIANCE_SUBMITTED);
        milestone.setRemarks(remarks);
        return milestoneRepository.save(milestone);
    }

    @Transactional
    public DisbursementMilestone verifyCompliance(Long milestoneId, boolean approved, String remarks, String verifierEmail) {
        User verifier = userRepository.findByEmail(verifierEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        if (verifier.getRole() != UserRole.ADMIN && verifier.getRole() != UserRole.GRANT_OFFICER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized to verify compliance.");
        }

        DisbursementMilestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Milestone not found"));

        if (milestone.getComplianceType() == MilestoneComplianceType.DOCUMENTATION) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Documentation compliance cannot be manually verified.");
        }

        if (approved && milestone.getComplianceType() == MilestoneComplianceType.UTILIZATION_PROOF) {
            long evidenceCount = evidenceRepository.countByMilestoneIdAndStatus(milestoneId, "SUBMITTED");
            if (evidenceCount == 0) {
                // If there isn't a row with SUBMITTED check UNDER_REVIEW/COMPLETED maybe? Just general count.
                List<com.infosys.subsidy.entity.MilestoneComplianceEvidence> allEv = evidenceRepository.findByMilestoneId(milestoneId);
                if (allEv.isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Utilization proof evidence is required before compliance can be approved.");
                }
            }
        }

        if (approved) {
            milestone.setStatus(MilestoneStatus.COMPLETED);
            milestone.setComplianceVerifiedAt(LocalDateTime.now());
            milestone.setComplianceVerifiedBy(verifier.getId());
        } else {
            if (remarks == null || remarks.trim().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Remarks are mandatory when marking evidence as NON_COMPLIANT.");
            }
            milestone.setStatus(MilestoneStatus.NON_COMPLIANT);
        }

        if (remarks != null && !remarks.isEmpty()) {
            milestone.setRemarks(remarks);
        }

        return milestoneRepository.save(milestone);
    }

    public List<com.infosys.subsidy.entity.MilestoneComplianceEvidence> getEvidenceForMilestone(Long milestoneId) {
        return evidenceRepository.findByMilestoneId(milestoneId);
    }

    public com.infosys.subsidy.entity.MilestoneComplianceEvidence getEvidenceById(Long id) {
        return evidenceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evidence not found"));
    }

    @Transactional
    public com.infosys.subsidy.entity.MilestoneComplianceEvidence reviewEvidence(Long milestoneId, Long evidenceId, String action, String remarks, String verifierEmail) {
        User verifier = userRepository.findByEmail(verifierEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        if (verifier.getRole() != UserRole.ADMIN && verifier.getRole() != UserRole.GRANT_OFFICER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized to verify evidence.");
        }

        DisbursementMilestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Milestone not found"));

        com.infosys.subsidy.entity.MilestoneComplianceEvidence evidence = evidenceRepository.findById(evidenceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evidence not found"));

        if (!evidence.getMilestoneId().equals(milestoneId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Evidence does not belong to this milestone.");
        }

        if ("VERIFIED".equalsIgnoreCase(action) || "COMPLETED".equalsIgnoreCase(action)) {
            evidence.setStatus("VERIFIED");
        } else if ("REJECTED".equalsIgnoreCase(action) || "NON_COMPLIANT".equalsIgnoreCase(action)) {
            if (remarks == null || remarks.trim().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Remarks are mandatory for rejection.");
            }
            evidence.setStatus("REJECTED");
            milestone.setStatus(MilestoneStatus.NON_COMPLIANT);
            milestone.setRemarks(remarks);
            milestoneRepository.save(milestone);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid action.");
        }

        evidence.setRemarks(remarks);
        evidence.setReviewedBy(verifier.getId());
        evidence.setReviewedAt(LocalDateTime.now());

        com.infosys.subsidy.entity.MilestoneComplianceEvidence savedEvidence = evidenceRepository.save(evidence);

        // Auto-approve milestone if all evidences are verified!
        if ("VERIFIED".equalsIgnoreCase(action) || "COMPLETED".equalsIgnoreCase(action)) {
            java.util.List<com.infosys.subsidy.entity.MilestoneComplianceEvidence> allEvidences = evidenceRepository.findByMilestoneId(milestoneId);
            int totalEvidence = allEvidences.size();
            long verifiedEvidence = evidenceRepository.countByMilestoneIdAndStatus(milestoneId, "VERIFIED");
            
            if (totalEvidence > 0 && totalEvidence == verifiedEvidence) {
                milestone.setStatus(MilestoneStatus.COMPLETED);
                milestone.setComplianceVerifiedAt(LocalDateTime.now());
                milestone.setComplianceVerifiedBy(verifier.getId());
                milestone.setRemarks("Auto-verified: all submitted evidence documents were verified.");
                milestoneRepository.save(milestone);
            }
        }

        return savedEvidence;
    }
}
