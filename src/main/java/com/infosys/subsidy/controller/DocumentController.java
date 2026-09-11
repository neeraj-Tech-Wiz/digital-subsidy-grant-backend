package com.infosys.subsidy.controller;

import com.infosys.subsidy.entity.ApplicationDocument;
import com.infosys.subsidy.enums.DocumentType;
import com.infosys.subsidy.service.ApplicationDocumentService;
import com.infosys.subsidy.dto.DocumentUploadResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.nio.file.Files;

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

    @GetMapping("/{documentId}")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long documentId) {
        Resource resource = documentService.downloadDocument(documentId);
        
        String contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        try {
            contentType = Files.probeContentType(resource.getFile().toPath());
        } catch (Exception e) {
            // fallback
        }
        
        if (contentType == null) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
