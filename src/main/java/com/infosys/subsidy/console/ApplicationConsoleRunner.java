//package com.infosys.subsidy.console;
//
//import com.infosys.subsidy.dto.ApplicationRequest;
//import com.infosys.subsidy.entity.Application;
//import com.infosys.subsidy.entity.Scheme;
//import com.infosys.subsidy.repository.SchemeRepository;
//import com.infosys.subsidy.service.ApplicationService;
//
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.stereotype.Component;
//
//import java.util.List;
//import java.util.Scanner;
//
//@Component
//public class ApplicationConsoleRunner implements CommandLineRunner {
//
//    private final ApplicationService applicationService;
//    private final SchemeRepository schemeRepository;
//
//    public ApplicationConsoleRunner(
//            ApplicationService applicationService,
//            SchemeRepository schemeRepository) {
//
//        this.applicationService = applicationService;
//        this.schemeRepository = schemeRepository;
//    }
//
//    @Override
//    public void run(String... args) {
//
//        Scanner scanner = new Scanner(System.in);
//
//        System.out.println("\n");
//        System.out.println("==============================================");
//        System.out.println(" DIGITAL SUBSIDY & GRANT ADMINISTRATION SYSTEM");
//        System.out.println("==============================================");
//
//        // ==========================================
//        // SHOW AVAILABLE SCHEMES
//        // ==========================================
//
//        List<Scheme> schemes = schemeRepository.findAll();
//
//        System.out.println("\nAvailable Schemes:");
//
//        for (Scheme scheme : schemes) {
//
//            System.out.println(
//                    scheme.getId() + ". "
//                            + scheme.getSchemeName()
//            );
//        }
//
//        // ==========================================
//        // GET BENEFICIARY ID
//        // ==========================================
//
//        System.out.print("\nEnter Beneficiary ID: ");
//
//        Long beneficiaryId =
//                Long.parseLong(scanner.nextLine());
//
//
//        // ==========================================
//        // GET SCHEME ID
//        // ==========================================
//
//        System.out.print("Enter Scheme ID: ");
//
//        Long schemeId =
//                Long.parseLong(scanner.nextLine());
//
//
//        // ==========================================
//        // GET ELIGIBILITY DETAILS
//        // ==========================================
//
//        ApplicationRequest request =
//                new ApplicationRequest();
//
//
//        System.out.println("\n--- Enter Eligibility Details ---");
//
//
//        System.out.print("Enter Annual Income: ");
//
//        request.setIncome(
//                Double.parseDouble(scanner.nextLine())
//        );
//
//
//        System.out.print("Enter Land Area: ");
//
//        request.setLandArea(
//                Double.parseDouble(scanner.nextLine())
//        );
//
//
//        System.out.print("Enter Region: ");
//
//        request.setRegion(
//                scanner.nextLine()
//        );
//
//
//        System.out.print(
//                "Documents Completed (true/false): "
//        );
//
//        request.setDocumentsCompleted(
//                Boolean.parseBoolean(scanner.nextLine())
//        );
//
//
//        System.out.print(
//                "KYC Verified (true/false): "
//        );
//
//        request.setKycVerified(
//                Boolean.parseBoolean(scanner.nextLine())
//        );
//
//
//        System.out.print(
//                "Previous Benefit Received (true/false): "
//        );
//
//        request.setPreviousBenefit(
//                Boolean.parseBoolean(scanner.nextLine())
//        );
//
//
//        // ==========================================
//        // APPLY FOR SCHEME
//        // ==========================================
//
//        System.out.println("\n==============================================");
//        System.out.println(" CHECKING ELIGIBILITY...");
//        System.out.println("==============================================");
//
//
//        try {
//
//            Application application =
//                    applicationService.applyForScheme(
//                            beneficiaryId,
//                            schemeId,
//                            request
//                    );
//
//
//            // ==========================================
//            // SHOW RESULT
//            // ==========================================
//
//            System.out.println("\n==============================================");
//            System.out.println(" APPLICATION RESULT");
//            System.out.println("==============================================");
//
//            System.out.println(
//                    "Application ID: "
//                            + application.getId()
//            );
//
//            System.out.println(
//                    "Beneficiary ID: "
//                            + application.getBeneficiaryId()
//            );
//
//            System.out.println(
//                    "Scheme ID: "
//                            + application.getSchemeId()
//            );
//
//            System.out.println(
//                    "Eligibility Score: "
//                            + application.getEligibilityScore()
//            );
//
//            System.out.println(
//                    "Final Status: "
//                            + application.getStatus()
//            );
//
//            System.out.println("==============================================");
//
//
//        } catch (Exception e) {
//
//            System.out.println("\n❌ ERROR: " + e.getMessage());
//
//        }
//
//        // Keep console open for demo
//        System.out.println("\nPress ENTER to exit...");
//        scanner.nextLine();
//    }
//}