package com.infosys.subsidy.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class BeneficiaryRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Father name is required")
    private String fatherName;

    @NotBlank(message = "Gender is required")
    private String gender;

    @Min(value = 18, message = "Age must be at least 18")
    private Integer age;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String email;

    @Pattern(
            regexp = "\\d{10}",
            message = "Mobile number must contain exactly 10 digits"
    )
    private String mobileNumber;

    @Pattern(
            regexp = "\\d{12}",
            message = "Aadhaar number must contain exactly 12 digits"
    )
    private String aadhaarNumber;

    @NotBlank(message = "Address is required")
    private String address;


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
}