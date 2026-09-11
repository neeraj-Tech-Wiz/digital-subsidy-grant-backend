package com.infosys.subsidy.dto;

/**
 * Clean DTO for returning beneficiary profile data.
 * Does NOT expose the embedded User JPA entity directly.
 * Aadhaar is masked for security (only last 4 digits shown).
 */
public class BeneficiaryProfileResponse {

    private Long id;
    private Long userId;
    private String name;
    private String fatherName;
    private String gender;
    private Integer age;
    private String email;
    private String mobileNumber;
    private String aadhaarNumberMasked; // e.g. XXXX-XXXX-1234
    private String address;
    private boolean profileExists;

    public BeneficiaryProfileResponse() {
        this.profileExists = true;
    }

    // ===================================
    // GETTERS AND SETTERS
    // ===================================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public String getAadhaarNumberMasked() {
        return aadhaarNumberMasked;
    }

    public void setAadhaarNumberMasked(String aadhaarNumberMasked) {
        this.aadhaarNumberMasked = aadhaarNumberMasked;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public boolean isProfileExists() {
        return profileExists;
    }

    public void setProfileExists(boolean profileExists) {
        this.profileExists = profileExists;
    }
}
