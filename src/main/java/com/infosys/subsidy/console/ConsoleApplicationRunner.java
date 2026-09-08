package com.infosys.subsidy.console;

import com.infosys.subsidy.dto.ApplicationRequest;
import com.infosys.subsidy.entity.Application;
import com.infosys.subsidy.service.ApplicationService;
import com.infosys.subsidy.service.VerificationService;
import com.infosys.subsidy.repository.ApplicationRepository;
import com.infosys.subsidy.dto.VerificationRequest;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

@Component
public class ConsoleApplicationRunner implements CommandLineRunner {

    private final ApplicationService applicationService;
    private final VerificationService verificationService;
    private final ApplicationRepository applicationRepository;
    private final DocumentConsoleDemo documentConsoleDemo;
    private final OfficerManagementConsoleRunner officerManagementConsoleRunner;

    public ConsoleApplicationRunner(
            ApplicationService applicationService,
            VerificationService verificationService, 
            ApplicationRepository applicationRepository,
            DocumentConsoleDemo documentConsoleDemo,
            OfficerManagementConsoleRunner officerManagementConsoleRunner
    ) {
        this.applicationService = applicationService;
        this.verificationService = verificationService;
        this.applicationRepository = applicationRepository;
        this.documentConsoleDemo = documentConsoleDemo;
        this.officerManagementConsoleRunner = officerManagementConsoleRunner;
    }


    @Override
    public void run(String... args) {

        Scanner scanner = new Scanner(System.in);

        while (true) {

            printMenu();

            System.out.print("\nEnter your choice: ");

            String input = scanner.nextLine();

            int choice;

            try {

                choice = Integer.parseInt(input);

            } catch (NumberFormatException e) {

                System.out.println(
                        "\nInvalid choice. Please enter a number."
                );

                continue;
            }


            switch (choice) {

                case 1 -> createApplication(scanner);

                case 2 -> viewAllApplications();

                case 3 -> viewApplicationById(scanner);

                case 4 -> verifyApplication(scanner);

                case 5 ->
                        System.out.println(
                                "\nReject Application - Coming next."
                        );

                case 6 -> documentConsoleDemo.showDocumentMenu(scanner);

                case 7 -> officerManagementConsoleRunner.showOfflineMenu(scanner);

                case 0 -> {

                    System.out.println(
                            "\nExiting Digital Subsidy & Grant Platform..."
                    );

                    scanner.close();

                    return;
                }

                default ->
                        System.out.println(
                                "\nInvalid choice. Please try again."
                        );
            }
        }
    }


    // =====================================================
    // PRINT MENU
    // =====================================================

    private void printMenu() {

        System.out.println();
        System.out.println(
                "========================================"
        );

        System.out.println(
                " DIGITAL SUBSIDY & GRANT PLATFORM"
        );

        System.out.println(
                "========================================"
        );

        System.out.println(
                "1. Create New Application"
        );

        System.out.println(
                "2. View All Applications"
        );

        System.out.println(
                "3. View Application By ID"
        );

        System.out.println(
                "4. Verify Application"
        );

        System.out.println(
                "5. Reject Application"
        );

        System.out.println(
                "6. Document Management Menu"
        );

        System.out.println(
                "7. Officer Management & Login Menu"
        );

        System.out.println(
                "0. Exit"
        );

        System.out.println(
                "========================================"
        );
    }


    // =====================================================
    // CREATE APPLICATION
    // =====================================================

    private void createApplication(
            Scanner scanner
    ) {

        try {

            System.out.println();
            System.out.println(
                    "========================================"
            );

            System.out.println(
                    "CREATE NEW APPLICATION"
            );

            System.out.println(
                    "========================================"
            );


            // ==========================================
            // BENEFICIARY ID
            // ==========================================

            System.out.print(
                    "Enter Beneficiary ID: "
            );

            Long beneficiaryId =
                    Long.parseLong(
                            scanner.nextLine()
                    );


            // ==========================================
            // SCHEME ID
            // ==========================================

            System.out.print(
                    "Enter Scheme ID: "
            );

            Long schemeId =
                    Long.parseLong(
                            scanner.nextLine()
                    );


            // ==========================================
            // DYNAMIC ELIGIBILITY DATA
            // ==========================================

            System.out.println();
            System.out.println(
                    "Enter Eligibility Details"
            );

            System.out.println(
                    "(Type field name and value)"
            );


            Map<String, String> eligibilityData =
                    new LinkedHashMap<>();


            while (true) {

                System.out.print(
                        "\nEnter field name (or type DONE): "
                );

                String fieldName =
                        scanner.nextLine();


                if ("DONE".equalsIgnoreCase(
                        fieldName
                )) {
                    break;
                }


                if (fieldName.isBlank()) {

                    System.out.println(
                            "Field name cannot be empty."
                    );

                    continue;
                }


                System.out.print(
                        "Enter value for "
                                + fieldName
                                + ": "
                );

                String value =
                        scanner.nextLine();


                eligibilityData.put(
                        fieldName,
                        value
                );
            }


            // ==========================================
            // CREATE REQUEST DTO
            // ==========================================

            ApplicationRequest request =
                    new ApplicationRequest();

            request.setEligibilityData(
                    eligibilityData
            );


            // ==========================================
            // CALL EXISTING SERVICE
            // ==========================================

            Application application =
                    applicationService.applyForScheme(
                            beneficiaryId,
                            schemeId,
                            request
                    );


            // ==========================================
            // DISPLAY RESULT
            // ==========================================

            System.out.println();
            System.out.println(
                    "========================================"
            );

            System.out.println(
                    "APPLICATION CREATED SUCCESSFULLY"
            );

            System.out.println(
                    "========================================"
            );

            System.out.println(
                    "Application ID: "
                            + application.getId()
            );

            System.out.println(
                    "Eligibility Score: "
                            + application.getEligibilityScore()
            );

            System.out.println(
                    "Status: "
                            + application.getStatus()
            );

            System.out.println(
                    "Verification Route: "
                            + application.getVerificationRoute()
            );

            System.out.println(
                    "Current Verification Level: "
                            + application.getCurrentVerificationLevel()
            );

            System.out.println(
                    "Remarks: "
                            + application.getRemarks()
            );

            System.out.println(
                    "========================================"
            );


        } catch (Exception e) {

            System.out.println();
            System.out.println(
                    "ERROR: " + e.getMessage()
            );
        }
    }
    // =====================================================
// VIEW ALL APPLICATIONS
// =====================================================

    private void viewAllApplications() {

        System.out.println();
        System.out.println(
                "========================================"
        );

        System.out.println(
                "ALL APPLICATIONS"
        );

        System.out.println(
                "========================================"
        );

        var applications =
                applicationRepository.findAll();


        // ==========================================
        // CHECK IF EMPTY
        // ==========================================

        if (applications.isEmpty()) {

            System.out.println(
                    "No applications found."
            );

            return;
        }


        // ==========================================
        // DISPLAY APPLICATIONS
        // ==========================================

        for (Application application : applications) {

            System.out.println();

            System.out.println(
                    "Application ID: "
                            + application.getId()
            );

            System.out.println(
                    "Beneficiary ID: "
                            + application.getBeneficiaryId()
            );

            System.out.println(
                    "Scheme ID: "
                            + application.getSchemeId()
            );

            System.out.println(
                    "Eligibility Score: "
                            + application.getEligibilityScore()
            );

            System.out.println(
                    "Status: "
                            + application.getStatus()
            );

            System.out.println(
                    "Verification Route: "
                            + application.getVerificationRoute()
            );

            System.out.println(
                    "Current Level: "
                            + application.getCurrentVerificationLevel()
            );

            System.out.println(
                    "Application Date: "
                            + application.getApplicationDate()
            );

            System.out.println(
                    "----------------------------------------"
            );
        }


        System.out.println(
                "Total Applications: "
                        + applications.size()
        );

        System.out.println(
                "========================================"
        );
    }
    // =====================================================
// VIEW APPLICATION BY ID
// =====================================================

    private void viewApplicationById(
            Scanner scanner
    ) {

        try {

            System.out.println();
            System.out.println(
                    "========================================"
            );

            System.out.println(
                    "VIEW APPLICATION BY ID"
            );

            System.out.println(
                    "========================================"
            );

            System.out.print(
                    "Enter Application ID: "
            );

            Long applicationId =
                    Long.parseLong(
                            scanner.nextLine()
                    );


            Application application =
                    applicationRepository
                            .findById(applicationId)
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Application not found with ID: "
                                                    + applicationId
                                    )
                            );


            // ==========================================
            // DISPLAY APPLICATION DETAILS
            // ==========================================

            System.out.println();

            System.out.println(
                    "Application ID: "
                            + application.getId()
            );

            System.out.println(
                    "Beneficiary ID: "
                            + application.getBeneficiaryId()
            );

            System.out.println(
                    "Scheme ID: "
                            + application.getSchemeId()
            );

            System.out.println(
                    "Application Date: "
                            + application.getApplicationDate()
            );

            System.out.println(
                    "Eligibility Score: "
                            + application.getEligibilityScore()
            );

            System.out.println(
                    "Status: "
                            + application.getStatus()
            );

            System.out.println(
                    "Verification Route: "
                            + application.getVerificationRoute()
            );

            System.out.println(
                    "Current Verification Level: "
                            + application.getCurrentVerificationLevel()
            );

            System.out.println(
                    "Verification Assigned At: "
                            + application.getVerificationAssignedAt()
            );

            System.out.println(
                    "Verification Completed At: "
                            + application.getVerificationCompletedAt()
            );

            System.out.println(
                    "Verification Due Date: "
                            + application.getVerificationDueDate()
            );

            System.out.println(
                    "Remarks: "
                            + application.getRemarks()
            );

            System.out.println(
                    "========================================"
            );


        } catch (NumberFormatException e) {

            System.out.println(
                    "Invalid Application ID. Please enter a number."
            );

        } catch (Exception e) {

            System.out.println(
                    "ERROR: " + e.getMessage()
            );
        }
    }
    // =====================================================
// VERIFY APPLICATION
// =====================================================

    private void verifyApplication(
            Scanner scanner
    ) {

        try {

            System.out.println();
            System.out.println(
                    "========================================"
            );

            System.out.println(
                    "VERIFY APPLICATION"
            );

            System.out.println(
                    "========================================"
            );


            // ==========================================
            // APPLICATION ID
            // ==========================================

            System.out.print(
                    "Enter Application ID: "
            );

            Long applicationId =
                    Long.parseLong(
                            scanner.nextLine()
                    );


            // ==========================================
            // APPROVE / REJECT
            // ==========================================

            System.out.print(
                    "Approve application? (true/false): "
            );

            boolean approved =
                    Boolean.parseBoolean(
                            scanner.nextLine()
                    );


            // ==========================================
            // REMARKS
            // ==========================================

            System.out.print(
                    "Enter remarks: "
            );

            String remarks =
                    scanner.nextLine();


            // ==========================================
            // CREATE VERIFICATION REQUEST
            // ==========================================

            VerificationRequest request =
                    new VerificationRequest();

            request.setApproved(approved);

            request.setRemarks(remarks);


            // ==========================================
            // CALL VERIFICATION SERVICE
            // ==========================================

            Application application =
                    verificationService.verifyApplication(
                            applicationId,
                            request
                    );


            // ==========================================
            // DISPLAY RESULT
            // ==========================================

            System.out.println();
            System.out.println(
                    "========================================"
            );

            System.out.println(
                    "VERIFICATION RESULT"
            );

            System.out.println(
                    "========================================"
            );

            System.out.println(
                    "Application ID: "
                            + application.getId()
            );

            System.out.println(
                    "Status: "
                            + application.getStatus()
            );

            System.out.println(
                    "Verification Route: "
                            + application.getVerificationRoute()
            );

            System.out.println(
                    "Current Verification Level: "
                            + application.getCurrentVerificationLevel()
            );

            System.out.println(
                    "Verification Assigned At: "
                            + application.getVerificationAssignedAt()
            );

            System.out.println(
                    "Verification Completed At: "
                            + application.getVerificationCompletedAt()
            );

            System.out.println(
                    "Verification Due Date: "
                            + application.getVerificationDueDate()
            );

            System.out.println(
                    "Remarks: "
                            + application.getRemarks()
            );

            System.out.println(
                    "========================================"
            );


        } catch (NumberFormatException e) {

            System.out.println(
                    "Invalid Application ID."
            );

        } catch (Exception e) {

            System.out.println(
                    "ERROR: " + e.getMessage()
            );
        }
    }
}