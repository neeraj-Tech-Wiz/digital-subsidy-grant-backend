package com.digital.subsidy.scheme.model;

import com.digital.subsidy.scheme.enums.BeneficiaryCategory;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * BeneficiaryProfile Model.
 * Represents an applicant's data for evaluating eligibility against Scheme criteria.
 */
public class BeneficiaryProfile implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private int age;
    private String aadhaarNumber;
    private String mobileNumber;
    private String email;
    private double annualIncome;
    private double landHoldingAcres;
    private BeneficiaryCategory category;
    private String region; // State or District
    private boolean aadhaarVerified;
    private boolean mobileVerified;
    private boolean documentsVerified;
    private boolean previousBenefitsReceived;

    // Additional generic/extensible attributes
    private Map<String, Object> attributes = new HashMap<>();

    public BeneficiaryProfile() {
        this.aadhaarVerified = true;
        this.mobileVerified = true;
        this.documentsVerified = true;
        this.previousBenefitsReceived = false;
        this.category = BeneficiaryCategory.GENERAL;
    }

    public BeneficiaryProfile(Long id, String name, int age, String aadhaarNumber,
                              String mobileNumber, String email, double annualIncome,
                              double landHoldingAcres, BeneficiaryCategory category, String region) {
        this();
        this.id = id;
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

    public void setAttribute(String key, Object value) {
        this.attributes.put(key.toLowerCase(), value);
    }

    public Object getAttribute(String key) {
        return this.attributes.get(key.toLowerCase());
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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
