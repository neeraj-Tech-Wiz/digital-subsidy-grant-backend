package com.infosys.subsidy.service;

import com.infosys.subsidy.entity.ApplicationDocument;
import com.infosys.subsidy.entity.SchemeRequiredDocument;
import com.infosys.subsidy.enums.DocumentStatus;
import com.infosys.subsidy.enums.DocumentType;
import com.infosys.subsidy.repository.ApplicationDocumentRepository;
import com.infosys.subsidy.repository.SchemeRequiredDocumentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DocumentValidationService {

    private final SchemeRequiredDocumentRepository schemeRequiredDocumentRepository;
    private final ApplicationDocumentRepository applicationDocumentRepository;

    public DocumentValidationService(SchemeRequiredDocumentRepository schemeRequiredDocumentRepository,
                                     ApplicationDocumentRepository applicationDocumentRepository) {
        this.schemeRequiredDocumentRepository = schemeRequiredDocumentRepository;
        this.applicationDocumentRepository = applicationDocumentRepository;
    }

    /**
     * Checks if all mandatory documents are at least uploaded for the given application.
     */
    public boolean validateMandatoryDocumentsUploaded(Long schemeId, Long applicationId) {
        List<DocumentType> mandatoryTypes = getMandatoryDocumentTypes(schemeId);
        List<DocumentType> uploadedTypes = getApplicationDocumentTypes(applicationId);

        return uploadedTypes.containsAll(mandatoryTypes);
    }

    /**
     * Checks if all mandatory documents have been VERIFIED for the given application.
     */
    public boolean validateMandatoryDocumentsVerified(Long schemeId, Long applicationId) {
        List<DocumentType> mandatoryTypes = getMandatoryDocumentTypes(schemeId);
        
        List<ApplicationDocument> applicationDocuments = applicationDocumentRepository.findByApplicationId(applicationId);

        for (DocumentType type : mandatoryTypes) {
            boolean isVerified = applicationDocuments.stream()
                    .anyMatch(doc -> doc.getDocumentType() == type && 
                                   doc.getDocumentStatus() == DocumentStatus.VERIFIED);
            if (!isVerified) {
                return false;
            }
        }
        return true;
    }

    /**
     * Identifies which mandatory documents are missing or not uploaded.
     */
    public List<DocumentType> getMissingMandatoryDocuments(Long schemeId, Long applicationId) {
        List<DocumentType> mandatoryTypes = getMandatoryDocumentTypes(schemeId);
        List<DocumentType> uploadedTypes = getApplicationDocumentTypes(applicationId);

        return mandatoryTypes.stream()
                .filter(type -> !uploadedTypes.contains(type))
                .collect(Collectors.toList());
    }

    /**
     * Identifies which mandatory documents are not yet verified.
     */
    public List<DocumentType> getUnverifiedMandatoryDocuments(Long schemeId, Long applicationId) {
        List<DocumentType> mandatoryTypes = getMandatoryDocumentTypes(schemeId);
        List<ApplicationDocument> documents = applicationDocumentRepository.findByApplicationId(applicationId);

        return mandatoryTypes.stream()
                .filter(type -> documents.stream()
                        .noneMatch(doc -> doc.getDocumentType() == type && doc.getDocumentStatus() == DocumentStatus.VERIFIED))
                .collect(Collectors.toList());
    }

    private List<DocumentType> getMandatoryDocumentTypes(Long schemeId) {
        return schemeRequiredDocumentRepository.findBySchemeIdAndActiveTrue(schemeId)
                .stream()
                .filter(SchemeRequiredDocument::isMandatory)
                .map(SchemeRequiredDocument::getDocumentType)
                .collect(Collectors.toList());
    }

    private List<DocumentType> getApplicationDocumentTypes(Long applicationId) {
        return applicationDocumentRepository.findByApplicationId(applicationId)
                .stream()
                // exclude rejected/reupload required if considering "currently valid" uploads, 
                // but UPLOADED, UNDER_VERIFICATION, VERIFIED count as uploaded.
                .filter(doc -> doc.getDocumentStatus() != DocumentStatus.REJECTED && 
                             doc.getDocumentStatus() != DocumentStatus.REUPLOAD_REQUIRED)
                .map(ApplicationDocument::getDocumentType)
                .collect(Collectors.toList());
    }
}
