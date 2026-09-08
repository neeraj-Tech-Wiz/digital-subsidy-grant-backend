package com.infosys.subsidy.service;

import com.infosys.subsidy.dto.DocumentUploadResponse;
import com.infosys.subsidy.dto.DocumentVerificationRequest;
import com.infosys.subsidy.entity.Application;
import com.infosys.subsidy.entity.ApplicationDocument;
import com.infosys.subsidy.entity.SchemeRequiredDocument;
import com.infosys.subsidy.enums.DocumentStatus;
import com.infosys.subsidy.enums.DocumentType;
import com.infosys.subsidy.repository.ApplicationDocumentRepository;
import com.infosys.subsidy.repository.ApplicationRepository;
import com.infosys.subsidy.repository.SchemeRequiredDocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;

import com.infosys.subsidy.entity.User;
import com.infosys.subsidy.entity.Beneficiary;
import com.infosys.subsidy.enums.UserRole;
import com.infosys.subsidy.enums.VerificationLevel;
import com.infosys.subsidy.repository.UserRepository;
import com.infosys.subsidy.repository.BeneficiaryRepository;

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

    public ApplicationDocumentService(ApplicationDocumentRepository applicationDocumentRepository,
                                      ApplicationRepository applicationRepository,
                                      SchemeRequiredDocumentRepository schemeRequiredDocumentRepository,
                                      DocumentStorageService documentStorageService,
                                      UserRepository userRepository,
                                      BeneficiaryRepository beneficiaryRepository) {
        this.applicationDocumentRepository = applicationDocumentRepository;
        this.applicationRepository = applicationRepository;
        this.schemeRequiredDocumentRepository = schemeRequiredDocumentRepository;
        this.documentStorageService = documentStorageService;
        this.userRepository = userRepository;
        this.beneficiaryRepository = beneficiaryRepository;
    }

    private void validateBeneficiaryOwnership(Application application) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Authenticated user not found"));
        if (user.getRole() == UserRole.BENEFICIARY) {
            Beneficiary beneficiary = beneficiaryRepository.findById(application.getBeneficiaryId())
                    .orElseThrow(() -> new RuntimeException("Beneficiary not found"));
            if (!beneficiary.getEmail().equalsIgnoreCase(user.getEmail())) {
                throw new RuntimeException("Unauthorized access to application documents");
            }
        }
    }

    @Transactional
    public DocumentUploadResponse uploadDocument(Long applicationId, DocumentType documentType, MultipartFile file) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found with ID: " + applicationId));
        
        validateBeneficiaryOwnership(application);

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("application/pdf") && !contentType.equals("image/jpeg") && !contentType.equals("image/png"))) {
            throw new RuntimeException("Only allowed file types are PDF, JPG, JPEG, PNG");
        }

        // Validate if document type is configured for this scheme
        List<SchemeRequiredDocument> schemeDocs = schemeRequiredDocumentRepository.findBySchemeIdAndActiveTrue(application.getSchemeId());
        Optional<SchemeRequiredDocument> matchedConfig = schemeDocs.stream()
                .filter(doc -> doc.getDocumentType() == documentType)
                .findFirst();

        if (matchedConfig.isEmpty()) {
            throw new RuntimeException("Document type " + documentType + " is not configured for this scheme");
        }

        // Check if there's already an active uploaded document of same type
        Optional<ApplicationDocument> existingDoc = applicationDocumentRepository.findTopByApplicationIdAndDocumentTypeOrderByUploadedAtDesc(applicationId, documentType);
        
        DocumentStatus status = DocumentStatus.UPLOADED;
        
        if (existingDoc.isPresent()) {
            ApplicationDocument doc = existingDoc.get();
            if (doc.getDocumentStatus() == DocumentStatus.VERIFIED || doc.getDocumentStatus() == DocumentStatus.UNDER_VERIFICATION || doc.getDocumentStatus() == DocumentStatus.UPLOADED) {
                throw new RuntimeException("An active document of type " + documentType + " is already uploaded");
            }
            // If REUPLOAD_REQUIRED or REJECTED, we allow upload
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
        document.setDocumentStatus(status);

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

    public List<ApplicationDocument> getApplicationDocuments(Long applicationId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found with ID: " + applicationId));
        validateBeneficiaryOwnership(application);
        return applicationDocumentRepository.findByApplicationId(applicationId);
    }

    @Transactional
    public ApplicationDocument verifyDocument(Long documentId, DocumentVerificationRequest request) {
        ApplicationDocument document = applicationDocumentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found with ID: " + documentId));

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User officer = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Authenticated user not found"));
        
        if (!officer.isActive()) {
            throw new RuntimeException("Officer account is deactivated");
        }

        VerificationLevel currentLevel = document.getApplication().getCurrentVerificationLevel();
        if (currentLevel == null) {
            throw new RuntimeException("Cannot verify documents. Application has no active verification level.");
        }

        switch (currentLevel) {
            case LEVEL_1: if (officer.getRole() != UserRole.LEVEL_1_OFFICER) throw new RuntimeException("Unauthorized for LEVEL_1"); break;
            case LEVEL_2: if (officer.getRole() != UserRole.LEVEL_2_OFFICER) throw new RuntimeException("Unauthorized for LEVEL_2"); break;
            case LEVEL_3: if (officer.getRole() != UserRole.LEVEL_3_OFFICER) throw new RuntimeException("Unauthorized for LEVEL_3"); break;
            case FINAL_APPROVAL: if (officer.getRole() != UserRole.FINAL_APPROVAL_OFFICER) throw new RuntimeException("Unauthorized for FINAL_APPROVAL"); break;
        }

        if (document.getDocumentStatus() == DocumentStatus.VERIFIED) {
            throw new RuntimeException("Document is already verified");
        }

        if (request.isVerified()) {
            document.setDocumentStatus(DocumentStatus.VERIFIED);
        } else if (request.isReuploadRequired()) {
            document.setDocumentStatus(DocumentStatus.REUPLOAD_REQUIRED);
        } else {
            document.setDocumentStatus(DocumentStatus.REJECTED);
        }

        document.setRemarks(request.getRemarks());
        document.setVerifiedAt(LocalDateTime.now());
        document.setVerifiedBy(officer.getId());

        return applicationDocumentRepository.save(document);
    }
}
