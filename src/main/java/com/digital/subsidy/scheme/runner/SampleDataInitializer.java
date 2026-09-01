package com.digital.subsidy.scheme.runner;

import com.digital.subsidy.scheme.enums.BeneficiaryCategory;
import com.digital.subsidy.scheme.enums.CriterionType;
import com.digital.subsidy.scheme.enums.Operator;
import com.digital.subsidy.scheme.enums.SchemeStatus;
import com.digital.subsidy.scheme.model.EligibilityCriteria;
import com.digital.subsidy.scheme.model.Scheme;
import com.digital.subsidy.scheme.service.SchemeService;

/**
 * Initializes realistic sample government subsidy schemes and configurable criteria.
 */
public class SampleDataInitializer {

    public static void loadSampleData(SchemeService schemeService) {
        // Clear any existing schemes first if needed (only if service provides clear or by creating fresh)
        // Scheme 1: PM Kisan Samman Nidhi
        Scheme kisan = new Scheme(
                null,
                "SCH-PM-KISAN",
                "PM Kisan Samman Nidhi",
                "Direct income support of Rs. 6,000 per year to small and marginal farmer families.",
                6000.0,
                50000000.0,
                SchemeStatus.ACTIVE,
                "All India",
                BeneficiaryCategory.FARMER
        );
        kisan.setDisbursedAmount(12500000.0);
        Scheme savedKisan = schemeService.createScheme(kisan);

        schemeService.addCriterionToScheme(savedKisan.getId(), new EligibilityCriteria(
                null, "Annual Family Income", CriterionType.NUMERIC, Operator.LESS_THAN_EQUAL, "250000",
                25, 25, true, true, "Annual household income must not exceed Rs. 2,50,000"
        ));
        schemeService.addCriterionToScheme(savedKisan.getId(), new EligibilityCriteria(
                null, "Land Holding Area (Acres)", CriterionType.NUMERIC, Operator.LESS_THAN_EQUAL, "5.0",
                20, 20, true, true, "Cultivable land holding must be up to 5.0 acres"
        ));
        schemeService.addCriterionToScheme(savedKisan.getId(), new EligibilityCriteria(
                null, "Beneficiary Category", CriterionType.ENUM, Operator.EQUAL, "FARMER",
                15, 15, true, true, "Applicant must be registered in the Farmer category"
        ));
        schemeService.addCriterionToScheme(savedKisan.getId(), new EligibilityCriteria(
                null, "Aadhaar Identity KYC", CriterionType.BOOLEAN, Operator.EQUAL, "true",
                15, 15, true, true, "Aadhaar number must be linked and verified"
        ));
        schemeService.addCriterionToScheme(savedKisan.getId(), new EligibilityCriteria(
                null, "Valid Land Documents", CriterionType.BOOLEAN, Operator.EQUAL, "true",
                15, 15, false, true, "Land ownership records (7/12 or RoR) verified"
        ));
        schemeService.addCriterionToScheme(savedKisan.getId(), new EligibilityCriteria(
                null, "Active Bank Mobile", CriterionType.BOOLEAN, Operator.EQUAL, "true",
                10, 10, false, true, "Active mobile number linked with DBT bank account"
        ));

        // Scheme 2: PM Surya Ghar - National Solar Rooftop Scheme
        Scheme solar = new Scheme(
                null,
                "SCH-SOLAR-01",
                "PM Surya Ghar Rooftop Solar Grant",
                "Capital subsidy to install rooftop solar panels for free electricity generation.",
                78000.0,
                120000000.0,
                SchemeStatus.ACTIVE,
                "All India",
                BeneficiaryCategory.GENERAL
        );
        solar.setDisbursedAmount(35000000.0);
        Scheme savedSolar = schemeService.createScheme(solar);

        schemeService.addCriterionToScheme(savedSolar.getId(), new EligibilityCriteria(
                null, "Annual Family Income", CriterionType.NUMERIC, Operator.LESS_THAN_EQUAL, "800000",
                25, 25, false, true, "Income ceiling of Rs. 8,00,000 for standard subsidy"
        ));
        schemeService.addCriterionToScheme(savedSolar.getId(), new EligibilityCriteria(
                null, "Resident Region", CriterionType.TEXT, Operator.IN, "All India,Maharashtra,Karnataka,Gujarat,Delhi",
                20, 20, false, true, "Eligible states for Phase 1 deployment"
        ));
        schemeService.addCriterionToScheme(savedSolar.getId(), new EligibilityCriteria(
                null, "Aadhaar Identity KYC", CriterionType.BOOLEAN, Operator.EQUAL, "true",
                25, 25, true, true, "Valid Aadhaar identification is required"
        ));
        schemeService.addCriterionToScheme(savedSolar.getId(), new EligibilityCriteria(
                null, "Valid Electricity Connection Documents", CriterionType.BOOLEAN, Operator.EQUAL, "true",
                30, 30, true, true, "Valid consumer number and latest electricity bill in applicant name"
        ));

        // Scheme 3: National Higher Education Merit Scholarship
        Scheme edu = new Scheme(
                null,
                "SCH-EDU-GRANT",
                "National Higher Education Scholarship",
                "Financial scholarship for meritorious students pursuing college and university education.",
                50000.0,
                40000000.0,
                SchemeStatus.ACTIVE,
                "All India",
                BeneficiaryCategory.STUDENT
        );
        edu.setDisbursedAmount(8500000.0);
        Scheme savedEdu = schemeService.createScheme(edu);

        schemeService.addCriterionToScheme(savedEdu.getId(), new EligibilityCriteria(
                null, "Beneficiary Category", CriterionType.ENUM, Operator.EQUAL, "STUDENT",
                30, 30, true, true, "Applicant must be a currently enrolled student"
        ));
        schemeService.addCriterionToScheme(savedEdu.getId(), new EligibilityCriteria(
                null, "Minimum Age", CriterionType.NUMERIC, Operator.GREATER_THAN_EQUAL, "17",
                15, 15, true, true, "Minimum age requirement of 17 years"
        ));
        schemeService.addCriterionToScheme(savedEdu.getId(), new EligibilityCriteria(
                null, "Maximum Age", CriterionType.NUMERIC, Operator.LESS_THAN_EQUAL, "28",
                15, 15, true, true, "Maximum age limit of 28 years"
        ));
        schemeService.addCriterionToScheme(savedEdu.getId(), new EligibilityCriteria(
                null, "Annual Family Income", CriterionType.NUMERIC, Operator.LESS_THAN_EQUAL, "350000",
                25, 25, false, true, "Family income preference limit"
        ));
        schemeService.addCriterionToScheme(savedEdu.getId(), new EligibilityCriteria(
                null, "Academic Documents Verified", CriterionType.BOOLEAN, Operator.EQUAL, "true",
                15, 15, true, true, "Enrollment proof and transcripts verified"
        ));

        // Scheme 4: Mahila Udyami - Women Entrepreneurship Grant
        Scheme women = new Scheme(
                null,
                "SCH-WOMEN-ENT",
                "Mahila Udyami Small Business Grant",
                "Special grant to empower women entrepreneurs to establish micro and home-based enterprises.",
                100000.0,
                80000000.0,
                SchemeStatus.ACTIVE,
                "All India",
                BeneficiaryCategory.WOMEN_ENTREPRENEUR
        );
        women.setDisbursedAmount(14000000.0);
        Scheme savedWomen = schemeService.createScheme(women);

        schemeService.addCriterionToScheme(savedWomen.getId(), new EligibilityCriteria(
                null, "Beneficiary Category", CriterionType.ENUM, Operator.EQUAL, "WOMEN_ENTREPRENEUR",
                35, 35, true, true, "Must belong to Women Entrepreneur category or Women SHG"
        ));
        schemeService.addCriterionToScheme(savedWomen.getId(), new EligibilityCriteria(
                null, "Minimum Age", CriterionType.NUMERIC, Operator.GREATER_THAN_EQUAL, "21",
                15, 15, true, true, "Must be at least 21 years old"
        ));
        schemeService.addCriterionToScheme(savedWomen.getId(), new EligibilityCriteria(
                null, "Annual Family Income", CriterionType.NUMERIC, Operator.LESS_THAN_EQUAL, "500000",
                25, 25, false, true, "Annual family income ceiling"
        ));
        schemeService.addCriterionToScheme(savedWomen.getId(), new EligibilityCriteria(
                null, "Business Documents Verified", CriterionType.BOOLEAN, Operator.EQUAL, "true",
                25, 25, true, true, "Udyam registration or business plan submission verified"
        ));
    }
}
