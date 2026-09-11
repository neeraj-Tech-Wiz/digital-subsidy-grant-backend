package com.infosys.subsidy.dto;

import com.infosys.subsidy.enums.DocumentType;
import com.infosys.subsidy.enums.DocumentStatus;

public class ApplicationDocumentReviewDTO {
    private Long documentId;
    private DocumentType documentType;
    private String originalFileName;
    private DocumentStatus documentStatus;

    public ApplicationDocumentReviewDTO() {}

    public ApplicationDocumentReviewDTO(Long documentId, DocumentType documentType, String originalFileName, DocumentStatus documentStatus) {
        this.documentId = documentId;
        this.documentType = documentType;
        this.originalFileName = originalFileName;
        this.documentStatus = documentStatus;
    }

    public Long getDocumentId() {
        return documentId;
    }
    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }
    public DocumentType getDocumentType() {
        return documentType;
    }
    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }
    public String getOriginalFileName() {
        return originalFileName;
    }
    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }
    public DocumentStatus getDocumentStatus() {
        return documentStatus;
    }
    public void setDocumentStatus(DocumentStatus documentStatus) {
        this.documentStatus = documentStatus;
    }
}
