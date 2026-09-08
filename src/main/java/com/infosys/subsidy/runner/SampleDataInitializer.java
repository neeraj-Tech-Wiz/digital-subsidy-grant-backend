package com.infosys.subsidy.runner;

import com.infosys.subsidy.entity.EligibilityCriteria;
import com.infosys.subsidy.entity.Scheme;
import com.infosys.subsidy.enums.BeneficiaryCategory;
import com.infosys.subsidy.enums.CriterionType;
import com.infosys.subsidy.enums.Operator;
import com.infosys.subsidy.enums.SchemeStatus;
import com.infosys.subsidy.service.SchemeService;
import com.infosys.subsidy.entity.User;
import com.infosys.subsidy.entity.SchemeRequiredDocument;
import com.infosys.subsidy.enums.DocumentType;
import com.infosys.subsidy.enums.UserRole;
import com.infosys.subsidy.repository.UserRepository;
import com.infosys.subsidy.repository.SchemeRequiredDocumentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Initializes default government subsidy and grant schemes upon application startup
 * if no schemes exist in the database.
 */
@Component
@Order(1)
public class SampleDataInitializer implements CommandLineRunner {

    private final SchemeService schemeService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SchemeRequiredDocumentRepository requiredDocRepo;

    @Value("${admin.default.name:System Admin}")
    private String adminName;

    @Value("${admin.default.email:admin@infosys.com}")
    private String adminEmail;

    @Value("${admin.default.password:admin123}")
    private String adminPassword;

    public SampleDataInitializer(SchemeService schemeService, UserRepository userRepository, PasswordEncoder passwordEncoder, SchemeRequiredDocumentRepository requiredDocRepo) {
        this.schemeService = schemeService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.requiredDocRepo = requiredDocRepo;
    }

    @Override
    public void run(String... args) {
        seedAdmin();
        boolean needsData = false;
        try {
            if (schemeService.getSchemeByCode("SCH-PM-KISAN-01") == null) {
                needsData = true;
            }
        } catch (Exception e) {
            needsData = true; // Exception indicates scheme not found
        }

        if (needsData) {
            loadSampleData(schemeService, requiredDocRepo);
        }
    }

    private void seedAdmin() {
        if (!userRepository.existsByRole(UserRole.ADMIN)) {
            User admin = new User();
            admin.setName(adminName);
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole(UserRole.ADMIN);
            admin.setActive(true);
            userRepository.save(admin);
        }
    }

    public static void loadSampleData(SchemeService service, SchemeRequiredDocumentRepository requiredDocRepo) {
        // Scheme 1: PM Kisan Samman Nidhi (PM-KISAN)
        Scheme s1 = new Scheme(
                null,
                "SCH-PM-KISAN-01",
                "PM Kisan Samman Nidhi",
                "Direct income support of Rs. 6,000 per year in three equal installments to small and marginal farmer families.",
                6000.0,
                50000000.0,
                SchemeStatus.ACTIVE,
                "All India",
                BeneficiaryCategory.FARMER
        );
        Scheme created1 = service.createScheme(s1);
        service.addCriterionToScheme(created1.getId(), new EligibilityCriteria(
                null, "Target Beneficiary Category", CriterionType.ENUM, Operator.EQUAL, "FARMER",
                25, 25, true, true, "Applicant must be a registered agricultural farmer"
        ));
        service.addCriterionToScheme(created1.getId(), new EligibilityCriteria(
                null, "Maximum Land Holding (Acres)", CriterionType.NUMERIC, Operator.LESS_THAN_EQUAL, "5.0",
                30, 30, true, true, "Landholding must not exceed 5.0 acres (Small & Marginal Farmers)"
        ));
        service.addCriterionToScheme(created1.getId(), new EligibilityCriteria(
                null, "Annual Family Income Ceiling", CriterionType.NUMERIC, Operator.LESS_THAN_EQUAL, "300000",
                25, 25, false, true, "Annual household income below Rs. 3,00,000"
        ));
        service.addCriterionToScheme(created1.getId(), new EligibilityCriteria(
                null, "Aadhaar KYC Verification", CriterionType.BOOLEAN, Operator.EQUAL, "true",
                20, 20, true, true, "Applicant Aadhaar number must be linked with bank account"
        ));

        // Add Mandatory Documents
        requiredDocRepo.save(createRequiredDoc(created1, DocumentType.AADHAAR_CARD, "Aadhaar Card", true));
        requiredDocRepo.save(createRequiredDoc(created1, DocumentType.BANK_PASSBOOK, "Bank Passbook", true));
        requiredDocRepo.save(createRequiredDoc(created1, DocumentType.INCOME_CERTIFICATE, "Income Certificate", true));

        // Scheme 2: Prime Minister Employment Generation Programme (PMEGP)
        Scheme s2 = new Scheme(
                null,
                "SCH-PMEGP-02",
                "Prime Minister Employment Generation Programme",
                "Credit-linked subsidy programme to generate self-employment opportunities in rural and urban areas.",
                250000.0,
                100000000.0,
                SchemeStatus.ACTIVE,
                "All India",
                BeneficiaryCategory.WOMEN_ENTREPRENEUR
        );
        Scheme created2 = service.createScheme(s2);
        service.addCriterionToScheme(created2.getId(), new EligibilityCriteria(
                null, "Minimum Age of Entrepreneur", CriterionType.NUMERIC, Operator.GREATER_THAN_EQUAL, "18",
                20, 20, true, true, "Applicant must be at least 18 years old"
        ));
        service.addCriterionToScheme(created2.getId(), new EligibilityCriteria(
                null, "No Prior Government Benefit", CriterionType.BOOLEAN, Operator.EQUAL, "false",
                30, 30, true, true, "Applicant should not have availed any subsidy under other self-employment schemes"
        ));
        service.addCriterionToScheme(created2.getId(), new EligibilityCriteria(
                null, "Documents and Project Report Verified", CriterionType.BOOLEAN, Operator.EQUAL, "true",
                25, 25, true, true, "Project report and identity documents verified by District Industries Centre"
        ));
        service.addCriterionToScheme(created2.getId(), new EligibilityCriteria(
                null, "Target Demographics", CriterionType.ENUM, Operator.IN, "WOMEN_ENTREPRENEUR,ARTISAN,BPL_FAMILY",
                25, 25, false, true, "Preference given to Women, Artisans and Special Category groups"
        ));

        // Scheme 3: National Rooftop Solar Subsidy Scheme
        Scheme s3 = new Scheme(
                null,
                "SCH-SOLAR-03",
                "PM Surya Ghar Muft Bijli Yojana",
                "Subsidy for residential consumers to install solar rooftop systems for clean renewable energy.",
                78000.0,
                20000000.0,
                SchemeStatus.ACTIVE,
                "All India",
                BeneficiaryCategory.GENERAL
        );
        Scheme created3 = service.createScheme(s3);
        service.addCriterionToScheme(created3.getId(), new EligibilityCriteria(
                null, "Minimum Age of Householder", CriterionType.NUMERIC, Operator.GREATER_THAN_EQUAL, "21",
                20, 20, false, true, "Applicant must be an adult homeowner"
        ));
        service.addCriterionToScheme(created3.getId(), new EligibilityCriteria(
                null, "Documents and Roof Ownership Proof", CriterionType.BOOLEAN, Operator.EQUAL, "true",
                40, 40, true, true, "Electricity bill in applicant's name and proof of roof rights"
        ));
        service.addCriterionToScheme(created3.getId(), new EligibilityCriteria(
                null, "Annual Income Threshold", CriterionType.NUMERIC, Operator.LESS_THAN_EQUAL, "800000",
                40, 40, false, true, "Priority given to households with annual income under Rs. 8,00,000"
        ));

        // Scheme 4: Pradhan Mantri Matru Vandana Yojana (PMMVY)
        Scheme s4 = new Scheme(
                null,
                "SCH-PMMVY-04",
                "Pradhan Mantri Matru Vandana Yojana",
                "Maternity benefit cash incentive of Rs. 5,000 for pregnant women and lactating mothers for first living child.",
                5000.0,
                15000000.0,
                SchemeStatus.ACTIVE,
                "All India",
                BeneficiaryCategory.BPL_FAMILY
        );
        Scheme created4 = service.createScheme(s4);
        service.addCriterionToScheme(created4.getId(), new EligibilityCriteria(
                null, "Minimum Age", CriterionType.NUMERIC, Operator.GREATER_THAN_EQUAL, "19",
                25, 25, true, true, "Mother must be 19 years of age or older"
        ));
        service.addCriterionToScheme(created4.getId(), new EligibilityCriteria(
                null, "Family Income Criteria", CriterionType.NUMERIC, Operator.LESS_THAN_EQUAL, "250000",
                35, 35, true, true, "Belongs to economically weaker section / BPL"
        ));
        service.addCriterionToScheme(created4.getId(), new EligibilityCriteria(
                null, "Institutional Registration Verified", CriterionType.BOOLEAN, Operator.EQUAL, "true",
                40, 40, true, true, "Early pregnancy registration and MCP card verified"
        ));
    }

    private static SchemeRequiredDocument createRequiredDoc(Scheme scheme, DocumentType type, String name, boolean mandatory) {
        SchemeRequiredDocument doc = new SchemeRequiredDocument();
        doc.setScheme(scheme);
        doc.setDocumentType(type);
        doc.setDocumentName(name);
        doc.setMandatory(mandatory);
        doc.setActive(true);
        return doc;
    }
}
