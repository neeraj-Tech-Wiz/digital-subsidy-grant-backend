package com.infosys.subsidy.dto;

import com.infosys.subsidy.enums.BeneficiaryCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.HashMap;
import java.util.Map;

public class EligibilityEvaluationRequest {

    @NotBlank(message = "Applicant name is required")
    private String name;

    private int age;

    @NotBlank(message = "Aadhaar number is required")
    private String aadhaarNumber;

    private String mobileNumber;

    private String email;

    private double annualIncome;

    private double landHoldingAcres;

    @NotNull(message = "Beneficiary category is required")
    private BeneficiaryCategory category = BeneficiaryCategory.GENERAL;

    private String region = "All India";

    private boolean aadhaarVerified = true;

    private boolean mobileVerified = true;

    private boolean documentsVerified = true;

    private boolean previousBenefitsReceived = false;

    private Map<String, Object> attributes = new HashMap<>();

    public EligibilityEvaluationRequest() {
    }

    public EligibilityEvaluationRequest(String name, int age, String aadhaarNumber, String mobileNumber,
                                        String email, double annualIncome, double landHoldingAcres,
                                        BeneficiaryCategory category, String region) {
        this.name = name;
        this.age = age;
        this.aadhaarNumber = aadhaarNumber;
        this.mobileNumber = mobileNumber;
        this.email = email;
        this.annualIncome = annualIncome;
        this.landHoldingAcres = landHoldingAcres;
        this.category = category;
        this.region = region;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getAadhaarNumber() {
        return aadhaarNumber;
    }

    public void setAadhaarNumber(String aadhaarNumber) {
        this.aadhaarNumber = aadhaarNumber;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public double getAnnualIncome() {
        return annualIncome;
    }

    public void setAnnualIncome(double annualIncome) {
        this.annualIncome = annualIncome;
    }

    public double getLandHoldingAcres() {
        return landHoldingAcres;
    }

    public void setLandHoldingAcres(double landHoldingAcres) {
        this.landHoldingAcres = landHoldingAcres;
    }

    public BeneficiaryCategory getCategory() {
        return category;
    }

    public void setCategory(BeneficiaryCategory category) {
        this.category = category;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public boolean isAadhaarVerified() {
        return aadhaarVerified;
    }

    public void setAadhaarVerified(boolean aadhaarVerified) {
        this.aadhaarVerified = aadhaarVerified;
    }

    public boolean isMobileVerified() {
        return mobileVerified;
    }

    public void setMobileVerified(boolean mobileVerified) {
        this.mobileVerified = mobileVerified;
    }

    public boolean isDocumentsVerified() {
        return documentsVerified;
    }

    public void setDocumentsVerified(boolean documentsVerified) {
        this.documentsVerified = documentsVerified;
    }

    public boolean isPreviousBenefitsReceived() {
        return previousBenefitsReceived;
    }

    public void setPreviousBenefitsReceived(boolean previousBenefitsReceived) {
        this.previousBenefitsReceived = previousBenefitsReceived;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }
}
