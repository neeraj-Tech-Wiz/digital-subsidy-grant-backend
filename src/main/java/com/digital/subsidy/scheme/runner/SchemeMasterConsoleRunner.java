package com.digital.subsidy.scheme.runner;

import com.digital.subsidy.scheme.enums.BeneficiaryCategory;
import com.digital.subsidy.scheme.enums.CriterionType;
import com.digital.subsidy.scheme.enums.Operator;
import com.digital.subsidy.scheme.enums.SchemeStatus;
import com.digital.subsidy.scheme.model.BeneficiaryProfile;
import com.digital.subsidy.scheme.model.CriterionEvaluationResult;
import com.digital.subsidy.scheme.model.EligibilityCriteria;
import com.digital.subsidy.scheme.model.EligibilityEvaluationResult;
import com.digital.subsidy.scheme.model.Scheme;
import com.digital.subsidy.scheme.service.EligibilityEvaluationService;
import com.digital.subsidy.scheme.service.SchemeService;
import com.digital.subsidy.scheme.util.ConsoleTable;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

/**
 * Interactive Terminal Interface for Scheme Master Management.
 * Provides rich terminal inputs, formatted tabular outputs, and live scoring simulation.
 */
public class SchemeMasterConsoleRunner {

    private final SchemeService schemeService;
    private final EligibilityEvaluationService evaluationService;
    private final Scanner scanner;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private String formatCurrency(double amount) {
        return String.format("Rs. %,.2f", amount);
    }

    public SchemeMasterConsoleRunner(SchemeService schemeService, EligibilityEvaluationService evaluationService) {
        this.schemeService = schemeService;
        this.evaluationService = evaluationService;
        this.scanner = new Scanner(System.in);
    }

    public void run() {
        printBanner();
        boolean running = true;

        while (running) {
            printMainMenu();
            String choice = prompt("Enter your choice (0-10): ");

            try {
                switch (choice.trim()) {
                    case "1" -> handleCreateScheme();
                    case "2" -> handleListAllSchemes();
                    case "3" -> handleViewSchemeDetails();
                    case "4" -> handleUpdateScheme();
                    case "5" -> handleManageCriteria();
                    case "6" -> handleChangeSchemeStatus();
                    case "7" -> handleSearchAndFilter();
                    case "8" -> handleDeleteScheme();
                    case "9" -> handleSimulateEligibility();
                    case "10" -> handleReloadSampleData();
                    case "0" -> {
                        System.out.println("\n[INFO] Exiting Scheme Master Terminal Application. Goodbye!");
                        running = false;
                    }
                    default -> System.out.println("\n[ERROR] Invalid option selected. Please choose between 0 and 10.");
                }
            } catch (Exception e) {
                System.out.println("\n[ERROR] Operation failed: " + e.getMessage());
            }

            if (running) {
                System.out.println("\nPress Enter to continue...");
                scanner.nextLine();
            }
        }
    }

    private void printBanner() {
        System.out.println("==========================================================================================");
        System.out.println("            DIGITAL SUBSIDY & GRANT PLATFORM - SCHEME MASTER MODULE                       ");
        System.out.println("            Internship Task: Scheme Entity, Repository, Service & Criteria               ");
        System.out.println("==========================================================================================");
    }

    private void printMainMenu() {
        System.out.println("\n============================= SCHEME MASTER MENU =============================");
        System.out.println("  1.  Create New Scheme");
        System.out.println("  2.  List All Schemes (Summary Table)");
        System.out.println("  3.  View Scheme Details & Configured Eligibility Criteria");
        System.out.println("  4.  Update Scheme Details");
        System.out.println("  5.  Manage Scheme Eligibility Criteria (Add / Edit / Remove / Toggle)");
        System.out.println("  6.  Change Scheme Status (Draft / Active / Inactive / Closed)");
        System.out.println("  7.  Search & Filter Schemes");
        System.out.println("  8.  Delete Scheme");
        System.out.println("  9.  Simulate Beneficiary Eligibility Scoring & Routing");
        System.out.println("  10. Reset & Reload Default Government Schemes");
        System.out.println("  0.  Exit");
        System.out.println("==============================================================================");
    }

    // ==================== 1. CREATE SCHEME ====================
    private void handleCreateScheme() {
        System.out.println("\n--- [CREATE NEW GOVERNMENT SCHEME] ---");
        String code = promptRequired("Enter Scheme Code (e.g. SCH-PM-AGRI-02): ");
        String name = promptRequired("Enter Scheme Name: ");
        String description = prompt("Enter Description: ");
        double grantAmount = promptDouble("Enter Grant Amount per Beneficiary (INR): ", 1.0, 100000000.0);
        double totalBudget = promptDouble("Enter Total Scheme Budget (INR): ", grantAmount, 10000000000.0);
        String region = promptDefault("Enter Applicable Region [Default: All India]: ", "All India");

        BeneficiaryCategory category = promptBeneficiaryCategory();
        SchemeStatus status = promptSchemeStatus(SchemeStatus.DRAFT);

        Scheme scheme = new Scheme(null, code, name, description, grantAmount, totalBudget, status, region, category);
        Scheme created = schemeService.createScheme(scheme);

        System.out.println("\n[SUCCESS] Scheme created successfully! (ID: " + created.getId() + ", Code: " + created.getSchemeCode() + ")");

        String addCrit = promptDefault("Would you like to add eligibility criteria now? (y/n) [Default: y]: ", "y");
        if (addCrit.equalsIgnoreCase("y") || addCrit.equalsIgnoreCase("yes")) {
            boolean adding = true;
            while (adding) {
                handleAddCriterionToScheme(created.getId());
                String more = promptDefault("Add another criterion? (y/n) [Default: n]: ", "n");
                adding = more.equalsIgnoreCase("y") || more.equalsIgnoreCase("yes");
            }
        }
    }

    // ==================== 2. LIST ALL SCHEMES ====================
    private void handleListAllSchemes() {
        List<Scheme> schemes = schemeService.getAllSchemes();
        System.out.println("\n--- [ALL REGISTERED SCHEMES (" + schemes.size() + ")] ---");
        displaySchemesTable(schemes);
    }

    // ==================== 3. VIEW SCHEME DETAILS ====================
    private void handleViewSchemeDetails() {
        System.out.println("\n--- [VIEW SCHEME DETAILS] ---");
        Long schemeId = promptLong("Enter Scheme ID (or leave blank to search by code): ", true);
        Scheme scheme;
        if (schemeId != null) {
            scheme = schemeService.getSchemeById(schemeId);
        } else {
            String code = promptRequired("Enter Scheme Code: ");
            scheme = schemeService.getSchemeByCode(code);
        }

        displaySchemeFullProfile(scheme);
    }

    // ==================== 4. UPDATE SCHEME ====================
    private void handleUpdateScheme() {
        System.out.println("\n--- [UPDATE SCHEME] ---");
        Long schemeId = promptLong("Enter Scheme ID to update: ", false);
        Scheme existing = schemeService.getSchemeById(schemeId);

        System.out.println("Editing Scheme: " + existing.getSchemeName() + " (" + existing.getSchemeCode() + ")");
        System.out.println("Leave blank to keep existing value.");

        String newName = prompt("New Scheme Name [" + existing.getSchemeName() + "]: ");
        String newDesc = prompt("New Description [" + existing.getDescription() + "]: ");
        String grantStr = prompt("New Grant Amount [" + existing.getGrantAmount() + "]: ");
        String budgetStr = prompt("New Total Budget [" + existing.getTotalBudget() + "]: ");
        String region = prompt("New Region [" + existing.getApplicableRegion() + "]: ");

        Scheme updates = new Scheme();
        if (!newName.trim().isEmpty()) updates.setSchemeName(newName);
        if (!newDesc.trim().isEmpty()) updates.setDescription(newDesc);
        if (!grantStr.trim().isEmpty()) updates.setGrantAmount(Double.parseDouble(grantStr.trim()));
        if (!budgetStr.trim().isEmpty()) updates.setTotalBudget(Double.parseDouble(budgetStr.trim()));
        if (!region.trim().isEmpty()) updates.setApplicableRegion(region);

        String changeCat = promptDefault("Change Beneficiary Category? (y/n) [Default: n]: ", "n");
        if (changeCat.equalsIgnoreCase("y")) {
            updates.setBeneficiaryCategory(promptBeneficiaryCategory());
        }

        Scheme updated = schemeService.updateScheme(schemeId, updates);
        System.out.println("\n[SUCCESS] Scheme updated successfully!");
        displaySchemeFullProfile(updated);
    }

    // ==================== 5. MANAGE CRITERIA ====================
    private void handleManageCriteria() {
        System.out.println("\n--- [MANAGE SCHEME ELIGIBILITY CRITERIA] ---");
        Long schemeId = promptLong("Enter Scheme ID: ", false);
        Scheme scheme = schemeService.getSchemeById(schemeId);

        System.out.println("\nTarget Scheme: " + scheme.getSchemeName() + " (Code: " + scheme.getSchemeCode() + ")");
        displayCriteriaTable(scheme.getCriteriaList());

        System.out.println("\nCriteria Operations:");
        System.out.println("  1. Add New Criterion");
        System.out.println("  2. Edit Existing Criterion");
        System.out.println("  3. Remove Criterion");
        System.out.println("  4. Toggle Active/Inactive Status");
        System.out.println("  0. Back to Main Menu");

        String subChoice = prompt("Select criteria operation: ");
        switch (subChoice.trim()) {
            case "1" -> handleAddCriterionToScheme(schemeId);
            case "2" -> handleEditCriterion(schemeId);
            case "3" -> handleRemoveCriterion(schemeId);
            case "4" -> handleToggleCriterion(schemeId);
            case "0" -> {}
            default -> System.out.println("[ERROR] Invalid selection.");
        }
    }

    private void handleAddCriterionToScheme(Long schemeId) {
        System.out.println("\n--- Add Criterion to Scheme ID " + schemeId + " ---");
        String name = promptRequired("Criterion Name (e.g., Annual Income Limit, Minimum Age, Land Holding): ");
        CriterionType type = promptCriterionType();
        Operator op = promptOperator(type);
        String expectedVal = promptRequired("Expected Threshold / Target Value (e.g., 250000, true, FARMER): ");
        int weight = promptInt("Weight Points contribution (e.g. 25 points out of 100): ", 1, 100);
        boolean mandatory = promptBoolean("Is this criterion Mandatory (Must Pass)? (y/n): ");
        boolean active = true;
        String desc = prompt("Criterion Description / Notes: ");

        EligibilityCriteria criteria = new EligibilityCriteria(null, name, type, op, expectedVal, weight, weight, mandatory, active, desc);
        EligibilityCriteria added = schemeService.addCriterionToScheme(schemeId, criteria);
        System.out.println("\n[SUCCESS] Criterion '" + added.getCriterionName() + "' added with ID " + added.getId() + "!");
    }

    private void handleEditCriterion(Long schemeId) {
        Long critId = promptLong("Enter Criterion ID to edit: ", false);
        EligibilityCriteria updated = new EligibilityCriteria();

        String name = prompt("New Criterion Name (leave blank to keep current): ");
        if (!name.trim().isEmpty()) updated.setCriterionName(name);

        String changeType = promptDefault("Change Type & Operator? (y/n) [Default: n]: ", "n");
        if (changeType.equalsIgnoreCase("y")) {
            updated.setCriterionType(promptCriterionType());
            updated.setOperator(promptOperator(updated.getCriterionType()));
        }

        String expected = prompt("New Expected Value (leave blank to keep current): ");
        if (!expected.trim().isEmpty()) updated.setExpectedValue(expected);

        String weightStr = prompt("New Weight Points (leave blank to keep current): ");
        if (!weightStr.trim().isEmpty()) updated.setWeight(Integer.parseInt(weightStr.trim()));

        String mandStr = prompt("Is Mandatory? (y/n, or leave blank to keep current): ");
        if (!mandStr.trim().isEmpty()) updated.setMandatory(mandStr.equalsIgnoreCase("y") || mandStr.equalsIgnoreCase("yes"));

        schemeService.updateCriterion(schemeId, critId, updated);
        System.out.println("\n[SUCCESS] Criterion updated successfully!");
    }

    private void handleRemoveCriterion(Long schemeId) {
        Long critId = promptLong("Enter Criterion ID to remove: ", false);
        boolean removed = schemeService.removeCriterionFromScheme(schemeId, critId);
        if (removed) {
            System.out.println("\n[SUCCESS] Criterion removed successfully from Scheme.");
        } else {
            System.out.println("\n[ERROR] Criterion with ID " + critId + " could not be found.");
        }
    }

    private void handleToggleCriterion(Long schemeId) {
        Long critId = promptLong("Enter Criterion ID to toggle: ", false);
        EligibilityCriteria c = schemeService.toggleCriterionStatus(schemeId, critId);
        System.out.println("\n[SUCCESS] Criterion ID " + c.getId() + " status changed to: " + (c.isActive() ? "ACTIVE" : "INACTIVE"));
    }

    // ==================== 6. CHANGE STATUS ====================
    private void handleChangeSchemeStatus() {
        System.out.println("\n--- [CHANGE SCHEME STATUS] ---");
        Long schemeId = promptLong("Enter Scheme ID: ", false);
        Scheme scheme = schemeService.getSchemeById(schemeId);
        System.out.println("Current status: " + scheme.getStatus());

        SchemeStatus newStatus = promptSchemeStatus(scheme.getStatus());
        Scheme updated = schemeService.changeSchemeStatus(schemeId, newStatus);
        System.out.println("\n[SUCCESS] Scheme status updated to: " + updated.getStatus());
    }

    // ==================== 7. SEARCH & FILTER ====================
    private void handleSearchAndFilter() {
        System.out.println("\n--- [SEARCH & FILTER SCHEMES] ---");
        String keyword = prompt("Search Keyword (name/code/description, or leave empty): ");
        String region = prompt("Filter by Region (or leave empty): ");

        String filterCat = promptDefault("Filter by Target Category? (y/n) [Default: n]: ", "n");
        BeneficiaryCategory category = (filterCat.equalsIgnoreCase("y")) ? promptBeneficiaryCategory() : null;

        String filterStat = promptDefault("Filter by Scheme Status? (y/n) [Default: n]: ", "n");
        SchemeStatus status = (filterStat.equalsIgnoreCase("y")) ? promptSchemeStatus(null) : null;

        List<Scheme> results = schemeService.filterSchemes(keyword, category, region, status);
        System.out.println("\n--- [FILTER RESULTS (" + results.size() + " matches)] ---");
        displaySchemesTable(results);
    }

    // ==================== 8. DELETE SCHEME ====================
    private void handleDeleteScheme() {
        System.out.println("\n--- [DELETE SCHEME] ---");
        Long schemeId = promptLong("Enter Scheme ID to delete: ", false);
        Scheme scheme = schemeService.getSchemeById(schemeId);

        String confirm = prompt("Are you sure you want to permanently delete '" + scheme.getSchemeName() + "'? (Type 'DELETE' to confirm): ");
        if ("DELETE".equalsIgnoreCase(confirm.trim())) {
            schemeService.deleteScheme(schemeId);
            System.out.println("\n[SUCCESS] Scheme ID " + schemeId + " and all associated criteria deleted.");
        } else {
            System.out.println("\n[INFO] Delete operation cancelled.");
        }
    }

    // ==================== 9. SIMULATE ELIGIBILITY ====================
    private void handleSimulateEligibility() {
        System.out.println("\n==============================================================================");
        System.out.println("         BENEFICIARY APPLICATION ELIGIBILITY SCORING SIMULATION               ");
        System.out.println("==============================================================================");

        List<Scheme> activeSchemes = schemeService.getActiveSchemes();
        if (activeSchemes.isEmpty()) {
            System.out.println("\n[WARNING] No active schemes found. Please create or activate a scheme first.");
            return;
        }

        System.out.println("Active Schemes Available for Application:");
        for (Scheme s : activeSchemes) {
            System.out.println("  ID " + s.getId() + ": " + s.getSchemeName() + " (" + s.getSchemeCode() + ") - Grant: " + formatCurrency(s.getGrantAmount()));
        }

        Long schemeId = promptLong("\nSelect Target Scheme ID for application: ", false);
        Scheme selectedScheme = schemeService.getSchemeById(schemeId);

        System.out.println("\n--- Enter Beneficiary Applicant Details ---");
        String name = promptDefault("Beneficiary Full Name [Default: Ramesh Patil]: ", "Ramesh Patil");
        int age = promptInt("Age [Default: 38]: ", 18, 100);
        String aadhaar = promptDefault("12-Digit Aadhaar [Default: 123456789012]: ", "123456789012");
        String mobile = promptDefault("10-Digit Mobile [Default: 9876543210]: ", "9876543210");
        String email = promptDefault("Email [Default: ramesh@example.com]: ", "ramesh@example.com");
        double income = promptDouble("Annual Family Income in INR [Default: 180000]: ", 0.0, 100000000.0);
        double land = promptDouble("Cultivable Land Holding in Acres [Default: 3.5]: ", 0.0, 10000.0);
        String region = promptDefault("State / Region [Default: Maharashtra]: ", "Maharashtra");

        System.out.println("\nSelect Beneficiary Category:");
        BeneficiaryCategory cat = promptBeneficiaryCategory();

        boolean aadhaarKyc = promptBoolean("Is Aadhaar KYC verified? (y/n) [Default: y]: ");
        boolean docsValid = promptBoolean("Are submitted documents verified? (y/n) [Default: y]: ");
        boolean prevBenefits = promptBoolean("Has received previous benefits? (y/n) [Default: n]: ");

        BeneficiaryProfile profile = new BeneficiaryProfile(1L, name, age, aadhaar, mobile, email, income, land, cat, region);
        profile.setAadhaarVerified(aadhaarKyc);
        profile.setDocumentsVerified(docsValid);
        profile.setPreviousBenefitsReceived(prevBenefits);

        // Run evaluation
        EligibilityEvaluationResult report = evaluationService.evaluate(selectedScheme, profile);

        displayEvaluationReport(report, profile);
    }

    // ==================== 10. RELOAD SAMPLE DATA ====================
    private void handleReloadSampleData() {
        System.out.println("\n--- [RELOAD SAMPLE GOVERNMENT SCHEMES] ---");
        String confirm = prompt("Do you want to reload default government schemes? (y/n): ");
        if (confirm.equalsIgnoreCase("y") || confirm.equalsIgnoreCase("yes")) {
            SampleDataInitializer.loadSampleData(schemeService);
            System.out.println("\n[SUCCESS] Pre-configured schemes loaded successfully!");
            displaySchemesTable(schemeService.getAllSchemes());
        }
    }

    // ==================== DISPLAY FORMATTERS ====================

    private void displaySchemesTable(List<Scheme> schemes) {
        ConsoleTable table = new ConsoleTable("ID", "Scheme Code", "Scheme Name", "Category", "Grant (INR)", "Budget Remaining", "Criteria", "Status");
        for (Scheme s : schemes) {
            table.addRow(
                    String.valueOf(s.getId()),
                    s.getSchemeCode(),
                    truncate(s.getSchemeName(), 30),
                    s.getBeneficiaryCategory().name(),
                    formatCurrency(s.getGrantAmount()),
                    formatCurrency(s.getRemainingBudget()),
                    String.valueOf(s.getCriteriaList().size()),
                    s.getStatus().name()
            );
        }
        table.print();
    }

    private void displaySchemeFullProfile(Scheme scheme) {
        System.out.println("\n==========================================================================================");
        System.out.println(" SCHEME PROFILE: " + scheme.getSchemeName() + " (" + scheme.getSchemeCode() + ")");
        System.out.println("==========================================================================================");
        System.out.println("  ID                  : " + scheme.getId());
        System.out.println("  Status              : " + scheme.getStatus() + " (" + scheme.getStatus().getDescription() + ")");
        System.out.println("  Target Category     : " + scheme.getBeneficiaryCategory().getDisplayName());
        System.out.println("  Applicable Region   : " + scheme.getApplicableRegion());
        System.out.println("  Grant Amount        : " + formatCurrency(scheme.getGrantAmount()) + " per beneficiary");
        System.out.println("  Total Budget        : " + formatCurrency(scheme.getTotalBudget()));
        System.out.println("  Disbursed Amount    : " + formatCurrency(scheme.getDisbursedAmount()));
        System.out.println("  Remaining Budget    : " + formatCurrency(scheme.getRemainingBudget()));
        System.out.println("  Total Weight Points : " + scheme.getTotalCriteriaWeight() + " / 100");
        System.out.println("  Mandatory Rules     : " + scheme.getMandatoryCriteriaCount() + " conditions");
        System.out.println("  Created On          : " + scheme.getCreatedAt().format(dateFormatter));
        System.out.println("  Description         : " + scheme.getDescription());
        System.out.println("------------------------------------------------------------------------------------------");
        System.out.println(" CONFIGURED ELIGIBILITY CRITERIA (" + scheme.getCriteriaList().size() + "):");
        displayCriteriaTable(scheme.getCriteriaList());
    }

    private void displayCriteriaTable(List<EligibilityCriteria> criteria) {
        ConsoleTable table = new ConsoleTable("Crit ID", "Criterion Name", "Type", "Operator", "Expected Value", "Weight", "Mandatory", "Active");
        for (EligibilityCriteria c : criteria) {
            table.addRow(
                    String.valueOf(c.getId()),
                    c.getCriterionName(),
                    c.getCriterionType().name(),
                    c.getOperator().getSymbol() + " (" + c.getOperator().name() + ")",
                    c.getExpectedValue(),
                    c.getWeight() + " pts",
                    c.isMandatory() ? "YES [MANDATORY]" : "No",
                    c.isActive() ? "ACTIVE" : "INACTIVE"
            );
        }
        table.print();
    }

    private void displayEvaluationReport(EligibilityEvaluationResult report, BeneficiaryProfile profile) {
        System.out.println("\n==========================================================================================");
        System.out.println("                     ELIGIBILITY EVALUATION & SCORING REPORT                              ");
        System.out.println("==========================================================================================");
        System.out.println("  Applicant Name       : " + report.getBeneficiaryName() + " (Aadhaar: " + report.getAadhaarNumber() + ")");
        System.out.println("  Target Scheme        : " + report.getSchemeName() + " [" + report.getSchemeCode() + "]");
        System.out.println("  Grant Amount         : " + formatCurrency(report.getRequestedGrantAmount()));
        System.out.println("  Evaluation Date      : " + report.getEvaluatedAt().format(dateFormatter));
        System.out.println("------------------------------------------------------------------------------------------");
        System.out.println(" CRITERION-WISE TRANSPARENT SCORE BREAKDOWN:");

        ConsoleTable table = new ConsoleTable("Criterion", "Rule", "Applicant Value", "Result", "Score", "Evaluation Reason");
        for (CriterionEvaluationResult res : report.getCriteriaResults()) {
            String ruleStr = res.getCriterion().getOperator().getSymbol() + " " + res.getCriterion().getExpectedValue();
            String mandBadge = res.getCriterion().isMandatory() ? " *" : "";
            table.addRow(
                    res.getCriterion().getCriterionName() + mandBadge,
                    ruleStr,
                    res.getApplicantValue(),
                    res.isSatisfied() ? "[PASS]" : (res.getCriterion().isMandatory() ? "[FAIL-MANDATORY]" : "[FAIL]"),
                    res.getPointsAwarded() + " / " + res.getMaxPoints(),
                    res.getStatusMessage()
            );
        }
        table.print();
        System.out.println("  (* Indicates mandatory criterion)");

        System.out.println("------------------------------------------------------------------------------------------");
        System.out.println(" FINAL SCORING & WORKFLOW ROUTING DECISION:");
        System.out.println("  Total Score Calculated : " + report.getTotalScore() + " / " + report.getMaxPossibleScore() + " Points");
        System.out.println("  Mandatory Rules Passed : " + (report.isAllMandatorySatisfied() ? "YES (All Mandatory Conditions Satisfied)" : "NO (Failed Mandatory Requirement)"));
        System.out.println("  Routing Category       : " + report.getScoreCategory().getTitle());
        System.out.println("  Workflow Description   : " + report.getScoreCategory().getDescription());
        System.out.println("==========================================================================================");
    }

    // ==================== PROMPT HELPERS ====================

    private String prompt(String message) {
        System.out.print(message);
        return scanner.nextLine();
    }

    private String promptRequired(String message) {
        while (true) {
            System.out.print(message);
            String input = scanner.nextLine().trim();
            if (!input.isEmpty()) return input;
            System.out.println("[ERROR] This field cannot be empty. Please enter a value.");
        }
    }

    private String promptDefault(String message, String defaultVal) {
        System.out.print(message);
        String input = scanner.nextLine().trim();
        return input.isEmpty() ? defaultVal : input;
    }

    private double promptDouble(String message, double min, double max) {
        while (true) {
            System.out.print(message);
            String input = scanner.nextLine().trim();
            try {
                double val = Double.parseDouble(input);
                if (val >= min && val <= max) return val;
                System.out.println("[ERROR] Value must be between " + min + " and " + max);
            } catch (NumberFormatException e) {
                System.out.println("[ERROR] Please enter a valid decimal number.");
            }
        }
    }

    private int promptInt(String message, int min, int max) {
        while (true) {
            System.out.print(message);
            String input = scanner.nextLine().trim();
            try {
                int val = Integer.parseInt(input);
                if (val >= min && val <= max) return val;
                System.out.println("[ERROR] Value must be between " + min + " and " + max);
            } catch (NumberFormatException e) {
                System.out.println("[ERROR] Please enter a valid integer.");
            }
        }
    }

    private Long promptLong(String message, boolean allowBlank) {
        while (true) {
            System.out.print(message);
            String input = scanner.nextLine().trim();
            if (allowBlank && input.isEmpty()) return null;
            try {
                return Long.parseLong(input);
            } catch (NumberFormatException e) {
                System.out.println("[ERROR] Please enter a valid numeric ID.");
            }
        }
    }

    private boolean promptBoolean(String message) {
        System.out.print(message);
        String input = scanner.nextLine().trim().toLowerCase();
        return input.equals("y") || input.equals("yes") || input.equals("true") || input.equals("1");
    }

    private BeneficiaryCategory promptBeneficiaryCategory() {
        System.out.println("Available Beneficiary Categories:");
        BeneficiaryCategory[] categories = BeneficiaryCategory.values();
        for (int i = 0; i < categories.length; i++) {
            System.out.println("  " + (i + 1) + ". " + categories[i].name() + " (" + categories[i].getDisplayName() + ")");
        }
        int sel = promptInt("Select Category (1-" + categories.length + ") [Default 1]: ", 1, categories.length);
        return categories[sel - 1];
    }

    private SchemeStatus promptSchemeStatus(SchemeStatus defaultStatus) {
        System.out.println("Available Scheme Statuses:");
        SchemeStatus[] statuses = SchemeStatus.values();
        for (int i = 0; i < statuses.length; i++) {
            System.out.println("  " + (i + 1) + ". " + statuses[i].name() + " - " + statuses[i].getDescription());
        }
        int sel = promptInt("Select Status (1-" + statuses.length + "): ", 1, statuses.length);
        return statuses[sel - 1];
    }

    private CriterionType promptCriterionType() {
        System.out.println("Available Criterion Types:");
        CriterionType[] types = CriterionType.values();
        for (int i = 0; i < types.length; i++) {
            System.out.println("  " + (i + 1) + ". " + types[i].name() + " - " + types[i].getDescription());
        }
        int sel = promptInt("Select Criterion Type (1-" + types.length + "): ", 1, types.length);
        return types[sel - 1];
    }

    private Operator promptOperator(CriterionType type) {
        System.out.println("Available Operators for " + type + ":");
        Operator[] ops = Operator.values();
        for (int i = 0; i < ops.length; i++) {
            System.out.println("  " + (i + 1) + ". " + ops[i].getSymbol() + " - " + ops[i].getDescription());
        }
        int sel = promptInt("Select Operator (1-" + ops.length + "): ", 1, ops.length);
        return ops[sel - 1];
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen - 3) + "...";
    }
}
