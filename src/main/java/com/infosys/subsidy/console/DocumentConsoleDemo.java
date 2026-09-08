package com.infosys.subsidy.console;

import com.infosys.subsidy.dto.DocumentVerificationRequest;
import com.infosys.subsidy.entity.Application;
import com.infosys.subsidy.entity.ApplicationDocument;
import com.infosys.subsidy.entity.SchemeRequiredDocument;
import com.infosys.subsidy.enums.DocumentType;
import com.infosys.subsidy.repository.ApplicationRepository;
import com.infosys.subsidy.repository.SchemeRequiredDocumentRepository;
import com.infosys.subsidy.service.ApplicationDocumentService;
import com.infosys.subsidy.service.ApplicationService;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;
import java.util.Scanner;

@Component
public class DocumentConsoleDemo {

    private final SchemeRequiredDocumentRepository schemeRequiredDocumentRepository;
    private final ApplicationDocumentService applicationDocumentService;
    private final ApplicationService applicationService;
    private final ApplicationRepository applicationRepository;

    public DocumentConsoleDemo(SchemeRequiredDocumentRepository schemeRequiredDocumentRepository,
                               ApplicationDocumentService applicationDocumentService,
                               ApplicationService applicationService,
                               ApplicationRepository applicationRepository) {
        this.schemeRequiredDocumentRepository = schemeRequiredDocumentRepository;
        this.applicationDocumentService = applicationDocumentService;
        this.applicationService = applicationService;
        this.applicationRepository = applicationRepository;
    }

    public void showDocumentMenu(Scanner scanner) {
        while (true) {
            System.out.println("\n========================================");
            System.out.println("DOCUMENT MANAGEMENT MENU");
            System.out.println("========================================");
            System.out.println("1. Configure required documents for Scheme");
            System.out.println("2. View Scheme Required Documents");
            System.out.println("3. Upload Application Document (Mock)");
            System.out.println("4. View Application Documents");
            System.out.println("5. Validate and Submit Application Docs");
            System.out.println("6. Verify Document");
            System.out.println("7. Reject Document");
            System.out.println("8. Request Document Re-upload");
            System.out.println("9. View Complete Application Workflow (Status)");
            System.out.println("0. Return to Main Menu");
            System.out.println("========================================");
            System.out.print("Enter your choice: ");

            String input = scanner.nextLine();
            int choice;
            try {
                choice = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid choice.");
                continue;
            }

            switch (choice) {
                case 1 -> configureSchemeDocuments(scanner);
                case 2 -> viewSchemeDocuments(scanner);
                case 3 -> uploadApplicationDocument(scanner);
                case 4 -> viewApplicationDocuments(scanner);
                case 5 -> validateAndSubmitAppDocs(scanner);
                case 6 -> verifyDocumentAction(scanner, "VERIFY");
                case 7 -> verifyDocumentAction(scanner, "REJECT");
                case 8 -> verifyDocumentAction(scanner, "REUPLOAD");
                case 9 -> viewApplicationWorkflow(scanner);
                case 0 -> { return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private void configureSchemeDocuments(Scanner scanner) {
        System.out.print("Enter Scheme ID: ");
        Long schemeId = Long.parseLong(scanner.nextLine());

        System.out.print("Enter Document Type (e.g. AADHAAR_CARD, INCOME_CERTIFICATE): ");
        DocumentType type = DocumentType.valueOf(scanner.nextLine().toUpperCase());
        
        System.out.print("Enter Document Name (e.g. Aadhaar Card): ");
        String name = scanner.nextLine();
        
        System.out.print("Is Mandatory? (true/false): ");
        boolean mandatory = Boolean.parseBoolean(scanner.nextLine());

        SchemeRequiredDocument doc = new SchemeRequiredDocument();
        // Just setting ID isn't directly supported by the entity because it maps to Scheme, let's create a stub scheme object
        com.infosys.subsidy.entity.Scheme scheme = new com.infosys.subsidy.entity.Scheme();
        scheme.setId(schemeId);
        doc.setScheme(scheme);
        doc.setDocumentType(type);
        doc.setDocumentName(name);
        doc.setMandatory(mandatory);
        doc.setActive(true);

        schemeRequiredDocumentRepository.save(doc);
        System.out.println("Scheme required document configured successfully.");
    }

    private void viewSchemeDocuments(Scanner scanner) {
        System.out.print("Enter Scheme ID: ");
        Long schemeId = Long.parseLong(scanner.nextLine());
        List<SchemeRequiredDocument> docs = schemeRequiredDocumentRepository.findBySchemeIdAndActiveTrue(schemeId);
        System.out.println("\nRequired Documents for Scheme " + schemeId + ":");
        for (SchemeRequiredDocument d : docs) {
            System.out.println("- " + d.getDocumentType() + " | " + d.getDocumentName() + " | Mandatory: " + d.isMandatory());
        }
    }

    private void uploadApplicationDocument(Scanner scanner) {
        System.out.print("Enter Application ID: ");
        Long appId = Long.parseLong(scanner.nextLine());
        System.out.print("Enter Document Type: ");
        DocumentType type = DocumentType.valueOf(scanner.nextLine().toUpperCase());

        // We create a mock MultipartFile to bypass actual file reading
        // Using MockMultipartFile is usually for testing, but we can't easily use it in main sources without spring-test.
        // Instead I will write a custom stub implementation for MultipartFile within this demo class.
        System.out.println("Mocking file upload for type " + type + "...");
        org.springframework.web.multipart.MultipartFile mockFile = new ConsoleMockMultipartFile("file.pdf", "application/pdf", new byte[]{1,2,3,4});
        
        try {
            applicationDocumentService.uploadDocument(appId, type, mockFile);
            System.out.println("Document successfully uploaded.");
        } catch (Exception e) {
            System.out.println("Error uploading document: " + e.getMessage());
        }
    }

    private void viewApplicationDocuments(Scanner scanner) {
        System.out.print("Enter Application ID: ");
        Long appId = Long.parseLong(scanner.nextLine());
        
        Application app = applicationRepository.findById(appId).orElse(null);
        if(app == null) { System.out.println("App not found."); return; }

        List<ApplicationDocument> docs = applicationDocumentService.getApplicationDocuments(appId);
        
        System.out.println("\n========================================");
        System.out.println("APPLICATION DOCUMENTS");
        System.out.println("========================================");
        System.out.println("Application ID: " + appId);
        System.out.println("Scheme ID: " + app.getSchemeId());
        System.out.println("----------------------------------------");
        
        for (ApplicationDocument d : docs) {
            System.out.println(d.getDocumentType());
            System.out.println("Status: " + d.getDocumentStatus());
            System.out.println("ID: " + d.getId() + " | File: " + d.getOriginalFileName());
            System.out.println("Remarks: " + (d.getRemarks() != null ? d.getRemarks() : ""));
            System.out.println("----------------------------------------");
        }
    }

    private void validateAndSubmitAppDocs(Scanner scanner) {
        System.out.print("Enter Application ID to submit docs for: ");
        Long appId = Long.parseLong(scanner.nextLine());
        try {
            Application application = applicationService.submitApplicationDocuments(appId);
            System.out.println("Successfully submitted application! Status moved to: " + application.getStatus());
            System.out.println("Current Level: " + application.getCurrentVerificationLevel() + " Route: " + application.getVerificationRoute());
        } catch (Exception e) {
            System.out.println("Validation failed: " + e.getMessage());
        }
    }

    private void verifyDocumentAction(Scanner scanner, String action) {
        System.out.print("Enter Document ID: ");
        Long docId;
        try { docId = Long.parseLong(scanner.nextLine()); } catch (Exception e) { return; }
        
        System.out.print("Enter officer remarks: ");
        String remarks = scanner.nextLine();

        DocumentVerificationRequest req = new DocumentVerificationRequest();
        req.setRemarks(remarks);
        if ("VERIFY".equals(action)) { req.setVerified(true); }
        else if ("REJECT".equals(action)) { req.setVerified(false); }
        else if ("REUPLOAD".equals(action)) { req.setReuploadRequired(true); }

        try {
            applicationDocumentService.verifyDocument(docId, req);
            System.out.println("Document successfully updated to " + action);
        } catch (Exception e) {
            System.out.println("Action failed: " + e.getMessage());
        }
    }

    private void viewApplicationWorkflow(Scanner scanner) {
        System.out.print("Enter Application ID: ");
        Long appId = Long.parseLong(scanner.nextLine());
        Application app = applicationRepository.findById(appId).orElse(null);
        if(app == null) { System.out.println("App not found."); return; }

        System.out.println("\n--- WORKFLOW STATUS ---");
        System.out.println("Application ID: " + app.getId());
        System.out.println("Status: " + app.getStatus());
        System.out.println("Verification Route: " + app.getVerificationRoute());
        System.out.println("Current Level: " + app.getCurrentVerificationLevel());
        System.out.println("App Remarks: " + app.getRemarks());
    }

    private static class ConsoleMockMultipartFile implements org.springframework.web.multipart.MultipartFile {
        private final String name;
        private final String contentType;
        private final byte[] content;
        
        public ConsoleMockMultipartFile(String name, String contentType, byte[] content) {
            this.name = name;
            this.contentType = contentType;
            this.content = content;
        }
        @Override public String getName() { return name; }
        @Override public String getOriginalFilename() { return name; }
        @Override public String getContentType() { return contentType; }
        @Override public boolean isEmpty() { return content.length == 0; }
        @Override public long getSize() { return content.length; }
        @Override public byte[] getBytes() { return content; }
        @Override public InputStream getInputStream() { return new java.io.ByteArrayInputStream(content); }
        @Override public void transferTo(java.io.File dest) throws java.io.IOException, IllegalStateException {
            java.nio.file.Files.write(dest.toPath(), content);
        }
    }
}
