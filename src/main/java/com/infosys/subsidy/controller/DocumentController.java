package com.infosys.subsidy.controller;

import com.infosys.subsidy.dto.DocumentUploadResponse;
import com.infosys.subsidy.dto.DocumentVerificationRequest;
import com.infosys.subsidy.entity.ApplicationDocument;
import com.infosys.subsidy.enums.DocumentType;
import com.infosys.subsidy.service.ApplicationDocumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = "*") // Or existing specific origins
public class DocumentController {

    private final ApplicationDocumentService documentService;

    public DocumentController(ApplicationDocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping("/upload/{applicationId}/{documentType}")
    public ResponseEntity<DocumentUploadResponse> uploadDocument(
            @PathVariable Long applicationId,
            @PathVariable DocumentType documentType,
            @RequestParam("file") MultipartFile file) {
        
        DocumentUploadResponse response = documentService.uploadDocument(applicationId, documentType, file);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/application/{applicationId}")
    public ResponseEntity<List<ApplicationDocument>> getApplicationDocuments(@PathVariable Long applicationId) {
        List<ApplicationDocument> documents = documentService.getApplicationDocuments(applicationId);
        return ResponseEntity.ok(documents);
    }

    @PostMapping("/verify/{documentId}")
    public ResponseEntity<ApplicationDocument> verifyDocument(
            @PathVariable Long documentId,
            @RequestBody DocumentVerificationRequest request) {
        
        ApplicationDocument document = documentService.verifyDocument(documentId, request);
        return ResponseEntity.ok(document);
    }
}
