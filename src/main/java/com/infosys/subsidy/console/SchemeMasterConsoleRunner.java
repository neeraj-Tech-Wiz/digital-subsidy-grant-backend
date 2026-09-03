//package com.infosys.subsidy.console;
//
//import com.infosys.subsidy.entity.EligibilityCriteria;
//import com.infosys.subsidy.entity.Scheme;
//import com.infosys.subsidy.enums.BeneficiaryCategory;
//import com.infosys.subsidy.enums.CriterionType;
//import com.infosys.subsidy.enums.Operator;
//import com.infosys.subsidy.enums.SchemeStatus;
//import com.infosys.subsidy.runner.SampleDataInitializer;
//import com.infosys.subsidy.service.SchemeService;
//import com.infosys.subsidy.util.ConsoleTable;
//import org.springframework.stereotype.Component;
//
//import java.time.format.DateTimeFormatter;
//import java.util.List;
//import java.util.Scanner;
//
///**
// * Interactive Terminal Interface for Scheme Master Management in the Spring Boot backend.
// */
//@Component
//public class SchemeMasterConsoleRunner {
//
//    private final SchemeService schemeService;
//    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
//
//    private String formatCurrency(double amount) {
//        return String.format("Rs. %,.2f", amount);
//    }
//
//    public SchemeMasterConsoleRunner(SchemeService schemeService) {
//        this.schemeService = schemeService;
//    }
//
//    public void runInteractive(Scanner scanner) {
//        printBanner();
//        boolean running = true;
//
//        while (running) {
//            printMainMenu();
//            System.out.print("Enter your choice (0-9): ");
//            String choice = scanner.nextLine();
//
//            try {
//                switch (choice.trim()) {
//                    case "1" -> handleCreateScheme(scanner);
//                    case "2" -> handleListAllSchemes();
//                    case "3" -> handleViewSchemeDetails(scanner);
//                    case "4" -> handleUpdateScheme(scanner);
//                    case "5" -> handleManageCriteria(scanner);
//                    case "6" -> handleChangeSchemeStatus(scanner);
//                    case "7" -> handleSearchAndFilter(scanner);
//                    case "8" -> handleDeleteScheme(scanner);
//                    case "9" -> handleReloadSampleData(scanner);
//                    case "0" -> {
//                        System.out.println("\n[INFO] Exiting Scheme Master Terminal Application.");
//                        running = false;
//                    }
//                    default -> System.out.println("\n[ERROR] Invalid option selected. Please choose between 0 and 9.");
//                }
//            } catch (Exception e) {
//                System.out.println("\n[ERROR] Operation failed: " + e.getMessage());
//            }
//
//            if (running) {
//                System.out.println("\nPress Enter to continue...");
//                scanner.nextLine();
//            }
//        }
//    }
//
//    private void printBanner() {
//        System.out.println("==========================================================================================");
//        System.out.println("            DIGITAL SUBSIDY & GRANT PLATFORM - SCHEME MASTER MODULE                       ");
//        System.out.println("            Spring Boot JPA Architecture: Scheme Entity, Repository & Service Layer      ");
//        System.out.println("==========================================================================================");
//    }
//
//    private void printMainMenu() {
//        System.out.println("\n============================= SCHEME MASTER MENU =============================");
//        System.out.println("  1.  Create New Scheme");
//        System.out.println("  2.  List All Schemes (Summary Table)");
//        System.out.println("  3.  View Scheme Details & Configured Eligibility Criteria");
//        System.out.println("  4.  Update Scheme Details");
//        System.out.println("  5.  Manage Scheme Eligibility Criteria (Add / Edit / Remove / Toggle)");
//        System.out.println("  6.  Change Scheme Status (Draft / Active / Inactive / Closed)");
//        System.out.println("  7.  Search & Filter Schemes");
//        System.out.println("  8.  Delete Scheme");
//        System.out.println("  9.  Reset & Reload Default Government Schemes");
//        System.out.println("  0.  Exit to Main Application");
//        System.out.println("==============================================================================");
//    }
//
//    private void handleCreateScheme(Scanner scanner) {
//        System.out.println("\n--- [CREATE NEW GOVERNMENT SCHEME] ---");
//        System.out.print("Enter Scheme Code (e.g. SCH-PM-AGRI-02): ");
//        String code = scanner.nextLine().trim();
//        System.out.print("Enter Scheme Name: ");
//        String name = scanner.nextLine().trim();
//        System.out.print("Enter Description: ");
//        String description = scanner.nextLine().trim();
//
//        System.out.print("Enter Grant Amount per Beneficiary (INR): ");
//        double grantAmount = Double.parseDouble(scanner.nextLine().trim());
//        System.out.print("Enter Total Scheme Budget (INR): ");
//        double totalBudget = Double.parseDouble(scanner.nextLine().trim());
//        System.out.print("Enter Applicable Region [Default: All India]: ");
//        String regionInput = scanner.nextLine().trim();
//        String region = regionInput.isEmpty() ? "All India" : regionInput;
//
//        BeneficiaryCategory category = promptBeneficiaryCategory(scanner);
//        SchemeStatus status = promptSchemeStatus(scanner, SchemeStatus.DRAFT);
//
//        Scheme scheme = new Scheme(null, code, name, description, grantAmount, totalBudget, status, region, category);
//        Scheme created = schemeService.createScheme(scheme);
//
//        System.out.println("\n[SUCCESS] Scheme created successfully! (ID: " + created.getId() + ", Code: " + created.getSchemeCode() + ")");
//    }
//
//    private void handleListAllSchemes() {
//        List<Scheme> schemes = schemeService.getAllSchemes();
//        System.out.println("\n--- [ALL REGISTERED SCHEMES (" + schemes.size() + ")] ---");
//        displaySchemesTable(schemes);
//    }
//
//    private void handleViewSchemeDetails(Scanner scanner) {
//        System.out.println("\n--- [VIEW SCHEME DETAILS] ---");
//        System.out.print("Enter Scheme ID: ");
//        String input = scanner.nextLine().trim();
//        Scheme scheme;
//        try {
//            Long id = Long.parseLong(input);
//            scheme = schemeService.getSchemeById(id);
//        } catch (NumberFormatException e) {
//            scheme = schemeService.getSchemeByCode(input);
//        }
//        displaySchemeFullProfile(scheme);
//    }
//
//    private void handleUpdateScheme(Scanner scanner) {
//        System.out.println("\n--- [UPDATE SCHEME] ---");
//        System.out.print("Enter Scheme ID to update: ");
//        Long schemeId = Long.parseLong(scanner.nextLine().trim());
//        Scheme existing = schemeService.getSchemeById(schemeId);
//
//        System.out.println("Editing Scheme: " + existing.getSchemeName() + " (" + existing.getSchemeCode() + ")");
//        System.out.println("Leave blank to keep existing value.");
//
//        System.out.print("New Scheme Name [" + existing.getSchemeName() + "]: ");
//        String newName = scanner.nextLine().trim();
//        System.out.print("New Description [" + existing.getDescription() + "]: ");
//        String newDesc = scanner.nextLine().trim();
//
//        if (!newName.isEmpty()) existing.setSchemeName(newName);
//        if (!newDesc.isEmpty()) existing.setDescription(newDesc);
//
//        Scheme updated = schemeService.createScheme(existing); // updates
//        System.out.println("\n[SUCCESS] Scheme updated successfully!");
//        displaySchemeFullProfile(updated);
//    }
//
//    private void handleManageCriteria(Scanner scanner) {
//        System.out.println("\n--- [MANAGE SCHEME ELIGIBILITY CRITERIA] ---");
//        System.out.print("Enter Scheme ID: ");
//        Long schemeId = Long.parseLong(scanner.nextLine().trim());
//        Scheme scheme = schemeService.getSchemeById(schemeId);
//
//        System.out.println("\nTarget Scheme: " + scheme.getSchemeName() + " (Code: " + scheme.getSchemeCode() + ")");
//        displayCriteriaTable(scheme.getCriteriaList());
//
//        System.out.println("\nCriteria Operations:");
//        System.out.println("  1. Add New Criterion");
//        System.out.println("  2. Remove Criterion");
//        System.out.println("  3. Toggle Active/Inactive Status");
//        System.out.println("  0. Back");
//
//        System.out.print("Select criteria operation: ");
//        String subChoice = scanner.nextLine().trim();
//        switch (subChoice) {
//            case "1" -> handleAddCriterionToScheme(scanner, schemeId);
//            case "2" -> {
//                System.out.print("Enter Criterion ID to remove: ");
//                Long critId = Long.parseLong(scanner.nextLine().trim());
//                schemeService.removeCriterionFromScheme(schemeId, critId);
//                System.out.println("[SUCCESS] Criterion removed.");
//            }
//            case "3" -> {
//                System.out.print("Enter Criterion ID to toggle: ");
//                Long critId = Long.parseLong(scanner.nextLine().trim());
//                EligibilityCriteria c = schemeService.toggleCriterionStatus(schemeId, critId);
//                System.out.println("[SUCCESS] Criterion active state: " + c.isActive());
//            }
//            case "0" -> {}
//            default -> System.out.println("[ERROR] Invalid selection.");
//        }
//    }
//
//    private void handleAddCriterionToScheme(Scanner scanner, Long schemeId) {
//        System.out.print("Criterion Name: ");
//        String name = scanner.nextLine().trim();
//        CriterionType type = promptCriterionType(scanner);
//        Operator op = promptOperator(scanner, type);
//        System.out.print("Expected Value: ");
//        String expectedVal = scanner.nextLine().trim();
//        System.out.print("Weight Points (1-100): ");
//        int weight = Integer.parseInt(scanner.nextLine().trim());
//        System.out.print("Is Mandatory (y/n): ");
//        boolean mandatory = scanner.nextLine().trim().equalsIgnoreCase("y");
//
//        EligibilityCriteria criteria = new EligibilityCriteria(null, name, type, op, expectedVal, weight, weight, mandatory, true, "");
//        EligibilityCriteria added = schemeService.addCriterionToScheme(schemeId, criteria);
//        System.out.println("\n[SUCCESS] Criterion '" + added.getCriterionName() + "' added with ID " + added.getId());
//    }
//
//    private void handleChangeSchemeStatus(Scanner scanner) {
//        System.out.print("Enter Scheme ID: ");
//        Long schemeId = Long.parseLong(scanner.nextLine().trim());
//        SchemeStatus newStatus = promptSchemeStatus(scanner, null);
//        Scheme updated = schemeService.changeSchemeStatus(schemeId, newStatus);
//        System.out.println("\n[SUCCESS] Scheme status updated to: " + updated.getStatus());
//    }
//
//    private void handleSearchAndFilter(Scanner scanner) {
//        System.out.print("Search Keyword: ");
//        String keyword = scanner.nextLine().trim();
//        List<Scheme> results = schemeService.filterSchemes(keyword, null, null, null);
//        displaySchemesTable(results);
//    }
//
//    private void handleDeleteScheme(Scanner scanner) {
//        System.out.print("Enter Scheme ID to delete: ");
//        Long schemeId = Long.parseLong(scanner.nextLine().trim());
//        schemeService.deleteScheme(schemeId);
//        System.out.println("\n[SUCCESS] Scheme deleted.");
//    }
//
//    private void handleReloadSampleData(Scanner scanner) {
//        SampleDataInitializer.loadSampleData(schemeService);
//        System.out.println("\n[SUCCESS] Default schemes loaded successfully!");
//        displaySchemesTable(schemeService.getAllSchemes());
//    }
//
//    private void displaySchemesTable(List<Scheme> schemes) {
//        ConsoleTable table = new ConsoleTable("ID", "Scheme Code", "Scheme Name", "Category", "Grant (INR)", "Budget Remaining", "Criteria", "Status");
//        for (Scheme s : schemes) {
//            table.addRow(
//                    String.valueOf(s.getId()),
//                    s.getSchemeCode(),
//                    s.getSchemeName(),
//                    s.getBeneficiaryCategory().name(),
//                    formatCurrency(s.getGrantAmount()),
//                    formatCurrency(s.getRemainingBudget()),
//                    String.valueOf(s.getCriteriaList() != null ? s.getCriteriaList().size() : 0),
//                    s.getStatus().name()
//            );
//        }
//        table.print();
//    }
//
//    private void displaySchemeFullProfile(Scheme scheme) {
//        System.out.println("\n==========================================================================================");
//        System.out.println(" SCHEME PROFILE: " + scheme.getSchemeName() + " (" + scheme.getSchemeCode() + ")");
//        System.out.println("==========================================================================================");
//        System.out.println("  ID                  : " + scheme.getId());
//        System.out.println("  Status              : " + scheme.getStatus() + " (" + scheme.getStatus().getDescription() + ")");
//        System.out.println("  Target Category     : " + scheme.getBeneficiaryCategory().getDisplayName());
//        System.out.println("  Applicable Region   : " + scheme.getApplicableRegion());
//        System.out.println("  Grant Amount        : " + formatCurrency(scheme.getGrantAmount()));
//        System.out.println("  Total Budget        : " + formatCurrency(scheme.getTotalBudget()));
//        System.out.println("  Disbursed Amount    : " + formatCurrency(scheme.getDisbursedAmount()));
//        System.out.println("  Remaining Budget    : " + formatCurrency(scheme.getRemainingBudget()));
//        System.out.println("  Description         : " + scheme.getDescription());
//        System.out.println("------------------------------------------------------------------------------------------");
//        System.out.println(" CONFIGURED ELIGIBILITY CRITERIA (" + (scheme.getCriteriaList() != null ? scheme.getCriteriaList().size() : 0) + "):");
//        displayCriteriaTable(scheme.getCriteriaList());
//    }
//
//    private void displayCriteriaTable(List<EligibilityCriteria> criteria) {
//        if (criteria == null || criteria.isEmpty()) {
//            System.out.println("  (No criteria configured)");
//            return;
//        }
//        ConsoleTable table = new ConsoleTable("Crit ID", "Criterion Name", "Type", "Operator", "Expected Value", "Weight", "Mandatory", "Active");
//        for (EligibilityCriteria c : criteria) {
//            table.addRow(
//                    String.valueOf(c.getId()),
//                    c.getCriterionName(),
//                    c.getCriterionType().name(),
//                    c.getOperator().getSymbol() + " (" + c.getOperator().name() + ")",
//                    c.getExpectedValue(),
//                    c.getWeight() + " pts",
//                    c.isMandatory() ? "YES [MANDATORY]" : "No",
//                    c.isActive() ? "ACTIVE" : "INACTIVE"
//            );
//        }
//        table.print();
//    }
//
//    private BeneficiaryCategory promptBeneficiaryCategory(Scanner scanner) {
//        BeneficiaryCategory[] categories = BeneficiaryCategory.values();
//        for (int i = 0; i < categories.length; i++) {
//            System.out.println("  " + (i + 1) + ". " + categories[i].name() + " (" + categories[i].getDisplayName() + ")");
//        }
//        System.out.print("Select Category (1-" + categories.length + ") [Default 1]: ");
//        String in = scanner.nextLine().trim();
//        int sel = in.isEmpty() ? 1 : Integer.parseInt(in);
//        return categories[Math.max(0, Math.min(categories.length - 1, sel - 1))];
//    }
//
//    private SchemeStatus promptSchemeStatus(Scanner scanner, SchemeStatus defaultStatus) {
//        SchemeStatus[] statuses = SchemeStatus.values();
//        for (int i = 0; i < statuses.length; i++) {
//            System.out.println("  " + (i + 1) + ". " + statuses[i].name() + " - " + statuses[i].getDescription());
//        }
//        System.out.print("Select Status (1-" + statuses.length + ") [Default 1]: ");
//        String in = scanner.nextLine().trim();
//        int sel = in.isEmpty() ? 1 : Integer.parseInt(in);
//        return statuses[Math.max(0, Math.min(statuses.length - 1, sel - 1))];
//    }
//
//    private CriterionType promptCriterionType(Scanner scanner) {
//        CriterionType[] types = CriterionType.values();
//        for (int i = 0; i < types.length; i++) {
//            System.out.println("  " + (i + 1) + ". " + types[i].name() + " - " + types[i].getDescription());
//        }
//        System.out.print("Select Criterion Type (1-" + types.length + "): ");
//        int sel = Integer.parseInt(scanner.nextLine().trim());
//        return types[Math.max(0, Math.min(types.length - 1, sel - 1))];
//    }
//
//    private Operator promptOperator(Scanner scanner, CriterionType type) {
//        Operator[] ops = Operator.values();
//        for (int i = 0; i < ops.length; i++) {
//            System.out.println("  " + (i + 1) + ". " + ops[i].getSymbol() + " - " + ops[i].getDescription());
//        }
//        System.out.print("Select Operator (1-" + ops.length + "): ");
//        int sel = Integer.parseInt(scanner.nextLine().trim());
//        return ops[Math.max(0, Math.min(ops.length - 1, sel - 1))];
//    }
//}
