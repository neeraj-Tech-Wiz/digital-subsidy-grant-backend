package com.infosys.subsidy.dto;

import com.infosys.subsidy.enums.DocumentStatus;
import com.infosys.subsidy.enums.DocumentType;

public class DocumentUploadResponse {

    private Long id;
    private Long applicationId;
    private DocumentType documentType;
    private String documentName;
    private String originalFileName;
    private DocumentStatus status;
    private String message;

    public DocumentUploadResponse() {}

    public DocumentUploadResponse(Long id, Long applicationId, DocumentType documentType, String documentName, String originalFileName, DocumentStatus status, String message) {
        this.id = id;
        this.applicationId = applicationId;
        this.documentType = documentType;
        this.documentName = documentName;
        this.originalFileName = originalFileName;
        this.status = status;
        this.message = message;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }

    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public void setStatus(DocumentStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
