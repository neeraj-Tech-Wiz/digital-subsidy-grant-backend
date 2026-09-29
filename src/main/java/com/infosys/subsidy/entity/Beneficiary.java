package com.infosys.subsidy.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "beneficiaries")
public class Beneficiary {

    // =============================
    // USER RELATIONSHIP
    // =============================

    @OneToOne
    @JoinColumn(
            name = "user_id",
            nullable = true,
            unique = true
    )
    private User user;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =============================
    // BASIC PERSONAL INFORMATION
    // =============================

    @Column(nullable = false)
    private String name;

    @Column(name = "father_name")
    private String fatherName;

    private String gender;

    @Column(nullable = false)
    private Integer age;

    @Column(nullable = false)
    private String email;

    @Column(
            name = "mobile_number",
            nullable = false,
            unique = true,
            length = 10
    )
    private String mobileNumber;

    @Column(
            name = "aadhaar_number",
            nullable = false,
            unique = true,
            length = 12
    )
    private String aadhaarNumber;

    @Column(length = 500)
    private String address;


    // =============================
    // EXISTING OLD FIELDS
    // =============================

    @Column(name = "government_scheme")
    private String governmentScheme;

    @Column(name = "beneficiary_category")
    private String beneficiaryCategory;

    @Column(name = "documents_completed")
    private Boolean documentsCompleted;

    @Column(name = "kyc_verified")
    private Boolean kycVerified;

    @Column(name = "land_area")
    private Double landArea;

    @Column(name = "previous_benefit")
    private Boolean previousBenefit;

    private Double income;

    private String region;

    // =============================
    // BANKING DETAILS (MODULE 3)
    // =============================

    @Column(name = "bank_account_holder_name")
    private String bankAccountHolderName;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "bank_account_number", length = 30)
    private String bankAccountNumber;

    @Column(name = "bank_ifsc_code", length = 20)
    private String bankIfscCode;

    @Column(name = "bank_account_type", length = 30)
    private String bankAccountType;

    // =============================
    // CONSTRUCTOR
    // =============================

    public Beneficiary() {
    }


    // =============================
    // GETTERS AND SETTERS
    // =============================

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


    public String getFatherName() {
        return fatherName;
    }

    public void setFatherName(String fatherName) {
        this.fatherName = fatherName;
    }


    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }


    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }


    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }


    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }


    public String getAadhaarNumber() {
        return aadhaarNumber;
    }

    public void setAadhaarNumber(String aadhaarNumber) {
        this.aadhaarNumber = aadhaarNumber;
    }


    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }


    public String getGovernmentScheme() {
        return governmentScheme;
    }

    public void setGovernmentScheme(String governmentScheme) {
        this.governmentScheme = governmentScheme;
    }


    public String getBeneficiaryCategory() {
        return beneficiaryCategory;
    }

    public void setBeneficiaryCategory(String beneficiaryCategory) {
        this.beneficiaryCategory = beneficiaryCategory;
    }


    public Boolean getDocumentsCompleted() {
        return documentsCompleted;
    }

    public void setDocumentsCompleted(Boolean documentsCompleted) {
        this.documentsCompleted = documentsCompleted;
    }


    public Boolean getKycVerified() {
        return kycVerified;
    }

    public void setKycVerified(Boolean kycVerified) {
        this.kycVerified = kycVerified;
    }


    public Double getLandArea() {
        return landArea;
    }

    public void setLandArea(Double landArea) {
        this.landArea = landArea;
    }


    public Boolean getPreviousBenefit() {
        return previousBenefit;
    }

    public void setPreviousBenefit(Boolean previousBenefit) {
        this.previousBenefit = previousBenefit;
    }


    public Double getIncome() {
        return income;
    }

    public void setIncome(Double income) {
        this.income = income;
    }


    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
    
    // Banking Getters & Setters
    
    public String getBankAccountHolderName() {
        return bankAccountHolderName;
    }

    public void setBankAccountHolderName(String bankAccountHolderName) {
        this.bankAccountHolderName = bankAccountHolderName;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getBankAccountNumber() {
        return bankAccountNumber;
    }

    public void setBankAccountNumber(String bankAccountNumber) {
        this.bankAccountNumber = bankAccountNumber;
    }

    public String getBankIfscCode() {
        return bankIfscCode;
    }

    public void setBankIfscCode(String bankIfscCode) {
        this.bankIfscCode = bankIfscCode;
    }

    public String getBankAccountType() {
        return bankAccountType;
    }

    public void setBankAccountType(String bankAccountType) {
        this.bankAccountType = bankAccountType;
    }
}