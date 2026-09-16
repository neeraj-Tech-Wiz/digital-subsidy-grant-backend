package com.infosys.subsidy.service;

import com.infosys.subsidy.dto.DocumentUploadResponse;
import com.infosys.subsidy.dto.DocumentVerificationRequest;
import com.infosys.subsidy.entity.Application;
import com.infosys.subsidy.entity.ApplicationDocument;
import com.infosys.subsidy.entity.Beneficiary;
import com.infosys.subsidy.entity.SchemeRequiredDocument;
import com.infosys.subsidy.entity.User;
import com.infosys.subsidy.enums.DocumentStatus;
import com.infosys.subsidy.enums.DocumentType;
import com.infosys.subsidy.enums.UserRole;
import com.infosys.subsidy.enums.VerificationLevel;
import com.infosys.subsidy.repository.ApplicationDocumentRepository;
import com.infosys.subsidy.repository.ApplicationRepository;
import com.infosys.subsidy.repository.BeneficiaryRepository;
import com.infosys.subsidy.repository.SchemeRequiredDocumentRepository;
import com.infosys.subsidy.repository.UserRepository;
import com.infosys.subsidy.repository.VerificationHistoryRepository;
import com.infosys.subsidy.entity.VerificationHistory;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ApplicationDocumentService {

    private final ApplicationDocumentRepository applicationDocumentRepository;
    private final ApplicationRepository applicationRepository;
    private final SchemeRequiredDocumentRepository schemeRequiredDocumentRepository;
    private final DocumentStorageService documentStorageService;
    private final UserRepository userRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final VerificationHistoryRepository verificationHistoryRepository;

    public ApplicationDocumentService(
            ApplicationDocumentRepository applicationDocumentRepository,
            ApplicationRepository applicationRepository,
            SchemeRequiredDocumentRepository schemeRequiredDocumentRepository,
            DocumentStorageService documentStorageService,
            UserRepository userRepository,
            BeneficiaryRepository beneficiaryRepository,
            VerificationHistoryRepository verificationHistoryRepository) {

        this.applicationDocumentRepository = applicationDocumentRepository;
        this.applicationRepository = applicationRepository;
        this.schemeRequiredDocumentRepository = schemeRequiredDocumentRepository;
        this.documentStorageService = documentStorageService;
        this.userRepository = userRepository;
        this.beneficiaryRepository = beneficiaryRepository;
        this.verificationHistoryRepository = verificationHistoryRepository;
    }


    // ============================================================
    // HELPER — Verify application ownership for BENEFICIARY users.
    //
    // OLD: compared beneficiary.email == user.email (fragile)
    // NEW: uses findByUserId() to get the proper beneficiary
    //      and compares beneficiary.id == application.beneficiaryId
    //
    // Returns 403 Forbidden if ownership check fails.
    // Non-beneficiary roles (officers, admin) are not restricted here
    // since the Security layer enforces their access.
    // ============================================================

    private void validateBeneficiaryOwnership(Application application) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Authenticated user not found"));

        if (user.getRole() == UserRole.BENEFICIARY) {
            // Use the proper user_id-based lookup instead of email matching
            Beneficiary beneficiary = beneficiaryRepository
                    .findByUserId(user.getId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Beneficiary profile not found. Please complete your profile first."));

            if (!beneficiary.getId().equals(application.getBeneficiaryId())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "You are not authorized to access this application.");
            }
        }
    }


    // ============================================================
    // UPLOAD DOCUMENT
    //
    // Ownership verified via user_id-based lookup.
    // Only the owner beneficiary can upload.
    // ============================================================

    @Transactional
    public DocumentUploadResponse uploadDocument(
            Long applicationId,
            DocumentType documentType,
            MultipartFile file) {

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Application not found with ID: " + applicationId));

        validateBeneficiaryOwnership(application);

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null
                || (!contentType.equals("application/pdf")
                && !contentType.equals("image/jpeg")
                && !contentType.equals("image/png"))) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only allowed file types are PDF, JPG, JPEG, PNG");
        }

        // Validate if document type is configured for this scheme
        List<SchemeRequiredDocument> schemeDocs =
                schemeRequiredDocumentRepository.findBySchemeIdAndActiveTrue(application.getSchemeId());
        Optional<SchemeRequiredDocument> matchedConfig = schemeDocs.stream()
                .filter(doc -> doc.getDocumentType() == documentType)
                .findFirst();

        if (matchedConfig.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Document type " + documentType + " is not configured for this scheme");
        }

        // Check if there's already an active uploaded document of same type
        Optional<ApplicationDocument> existingDoc =
                applicationDocumentRepository.findTopByApplicationIdAndDocumentTypeOrderByUploadedAtDesc(
                        applicationId, documentType);

        if (existingDoc.isPresent()) {
            ApplicationDocument doc = existingDoc.get();
            if (doc.getDocumentStatus() == DocumentStatus.VERIFIED
                    || doc.getDocumentStatus() == DocumentStatus.UNDER_VERIFICATION
                    || doc.getDocumentStatus() == DocumentStatus.UPLOADED) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "An active document of type " + documentType + " is already uploaded");
            }
        }

        String filePath = documentStorageService.storeFile(file);

        ApplicationDocument document = new ApplicationDocument();
        document.setApplication(application);
        document.setDocumentType(documentType);
        document.setDocumentName(matchedConfig.get().getDocumentName());
        document.setOriginalFileName(file.getOriginalFilename());
        document.setFilePath(filePath);
        document.setContentType(contentType);
        document.setFileSize(file.getSize());
        document.setDocumentStatus(DocumentStatus.UPLOADED);

        document = applicationDocumentRepository.save(document);

        return new DocumentUploadResponse(
                document.getId(),
                application.getId(),
                document.getDocumentType(),
                document.getDocumentName(),
                document.getOriginalFileName(),
                document.getDocumentStatus(),
                "Document uploaded successfully"
        );
    }


    // ============================================================
    // GET APPLICATION DOCUMENTS
    //
    // Ownership verified: only the owning beneficiary or officers
    // can view documents.
    // ============================================================

    public List<ApplicationDocument> getApplicationDocuments(Long applicationId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Application not found with ID: " + applicationId));
        validateBeneficiaryOwnership(application);
        return applicationDocumentRepository.findByApplicationId(applicationId);
    }


    // ============================================================
    // VERIFY DOCUMENT (Officer action)
    //
    // Ownership check not applied — officers can access any application.
    // Role and level restrictions are enforced here.
    // ============================================================

    @Transactional
    public ApplicationDocument verifyDocument(
            Long applicationId,
            Long documentId,
            DocumentVerificationRequest request) {

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Application not found with ID: " + applicationId));

        ApplicationDocument document = applicationDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Document not found with ID: " + documentId));

        if (!document.getApplication().getId().equals(applicationId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Document does not belong to the specified application.");
        }
        
        if (application.getStatus() != com.infosys.subsidy.enums.ApplicationStatus.PENDING_VERIFICATION) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Application is not currently pending verification.");
        }

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User officer = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Authenticated user not found"));

        if (!officer.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Officer account is deactivated");
        }

        VerificationLevel currentLevel = application.getCurrentVerificationLevel();
        if (currentLevel == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Application has no active verification level.");
        }

        switch (currentLevel) {
            case LEVEL_1:
                if (officer.getRole() != UserRole.LEVEL_1_OFFICER)
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized for LEVEL_1");
                break;
            case LEVEL_2:
                if (officer.getRole() != UserRole.LEVEL_2_OFFICER)
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized for LEVEL_2");
                break;
            case LEVEL_3:
                if (officer.getRole() != UserRole.LEVEL_3_OFFICER)
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized for LEVEL_3");
                break;
            case FINAL_APPROVAL:
                if (officer.getRole() != UserRole.FINAL_APPROVAL_OFFICER)
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized for FINAL_APPROVAL");
                break;
        }

        if (document.getDocumentStatus() == DocumentStatus.VERIFIED || document.getDocumentStatus() == DocumentStatus.REJECTED) {
            // Already processed
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Document is already fully processed: " + document.getDocumentStatus());
        }

        if ("VERIFIED".equalsIgnoreCase(request.getAction())) {
            document.setDocumentStatus(DocumentStatus.VERIFIED);
        } else if ("REJECTED".equalsIgnoreCase(request.getAction())) {
            document.setDocumentStatus(DocumentStatus.REJECTED);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid verify action requested.");
        }

        document.setRemarks(request.getRemarks());
        document.setVerifiedAt(LocalDateTime.now());
        document.setVerifiedBy(officer.getId());

        ApplicationDocument savedDoc = applicationDocumentRepository.save(document);

        VerificationHistory history = new VerificationHistory();
        history.setApplicationId(application.getId());
        history.setOfficerId(officer.getId());
        history.setOfficerName(officer.getName());
        history.setOfficerRole(officer.getRole().name());
        history.setVerificationLevel(currentLevel.name());
        
        if ("VERIFIED".equalsIgnoreCase(request.getAction())) {
            history.setAction("DOCUMENT_VERIFIED");
            history.setRemarks("Verified " + document.getDocumentName() + ": " + (request.getRemarks() != null ? request.getRemarks() : "No remarks"));
        } else {
            history.setAction("DOCUMENT_REJECTED");
            history.setRemarks("Rejected " + document.getDocumentName() + ": " + (request.getRemarks() != null ? request.getRemarks() : "No remarks"));
        }
        history.setActionTimestamp(LocalDateTime.now());
        verificationHistoryRepository.save(history);

        return savedDoc;
    }
    
    // ============================================================
    // STREAM DOWNLOAD DOCUMENT
    // ============================================================

    public org.springframework.core.io.Resource downloadDocument(Long documentId) {
        ApplicationDocument document = applicationDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Document not found with ID: " + documentId));

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Authenticated user not found"));

        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is deactivated");
        }

        if (user.getRole() == UserRole.BENEFICIARY) {
             validateBeneficiaryOwnership(document.getApplication());
        }

        return documentStorageService.loadFileAsResource(document.getFilePath());
    }
}
