package com.infosys.subsidy.dto;

import com.infosys.subsidy.enums.ApplicationStatus;
import com.infosys.subsidy.enums.VerificationLevel;
import com.infosys.subsidy.enums.VerificationRoute;

import java.time.LocalDateTime;
import java.util.List;

public class ApplicationDetailDTO {

    // Application details
    private Long applicationId;
    private ApplicationStatus status;
    private VerificationLevel currentVerificationLevel;
    private VerificationRoute verificationRoute;
    private Integer eligibilityScore;
    private LocalDateTime applicationDate;
    private LocalDateTime verificationDueDate;
    private String remarks;

    // Beneficiary Details securely extracted
    private String beneficiaryName;
    private String fatherName;
    private String gender;
    private Integer age;
    private String mobileNumber;
    private String email;
    private String address;
    private String maskedAadhaar;

    // Banking Details for Stage 1 Verification
    private String bankAccountHolderName;
    private String bankName;
    private String bankAccountNumber;
    private String bankIfscCode;
    private String bankAccountType;

    // Scheme Details
    private String schemeName;
    private String schemeCode;

    // Sub-objects
    private List<ApplicationDocumentReviewDTO> documents;
    private List<EligibilityScoreDetailsDTO> eligibilityDetails;
    private List<VerificationHistoryDTO> verificationHistory;

    public ApplicationDetailDTO() {}

    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }

    public ApplicationStatus getStatus() { return status; }
    public void setStatus(ApplicationStatus status) { this.status = status; }

    public VerificationLevel getCurrentVerificationLevel() { return currentVerificationLevel; }
    public void setCurrentVerificationLevel(VerificationLevel currentVerificationLevel) { this.currentVerificationLevel = currentVerificationLevel; }

    public VerificationRoute getVerificationRoute() { return verificationRoute; }
    public void setVerificationRoute(VerificationRoute verificationRoute) { this.verificationRoute = verificationRoute; }

    public Integer getEligibilityScore() { return eligibilityScore; }
    public void setEligibilityScore(Integer eligibilityScore) { this.eligibilityScore = eligibilityScore; }

    public LocalDateTime getApplicationDate() { return applicationDate; }
    public void setApplicationDate(LocalDateTime applicationDate) { this.applicationDate = applicationDate; }

    public LocalDateTime getVerificationDueDate() { return verificationDueDate; }
    public void setVerificationDueDate(LocalDateTime verificationDueDate) { this.verificationDueDate = verificationDueDate; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public String getBeneficiaryName() { return beneficiaryName; }
    public void setBeneficiaryName(String beneficiaryName) { this.beneficiaryName = beneficiaryName; }

    public String getFatherName() { return fatherName; }
    public void setFatherName(String fatherName) { this.fatherName = fatherName; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public String getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getMaskedAadhaar() { return maskedAadhaar; }
    public void setMaskedAadhaar(String maskedAadhaar) { this.maskedAadhaar = maskedAadhaar; }

    public String getBankAccountHolderName() { return bankAccountHolderName; }
    public void setBankAccountHolderName(String bankAccountHolderName) { this.bankAccountHolderName = bankAccountHolderName; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getBankAccountNumber() { return bankAccountNumber; }
    public void setBankAccountNumber(String bankAccountNumber) { this.bankAccountNumber = bankAccountNumber; }

    public String getBankIfscCode() { return bankIfscCode; }
    public void setBankIfscCode(String bankIfscCode) { this.bankIfscCode = bankIfscCode; }

    public String getBankAccountType() { return bankAccountType; }
    public void setBankAccountType(String bankAccountType) { this.bankAccountType = bankAccountType; }

    public String getSchemeName() { return schemeName; }
    public void setSchemeName(String schemeName) { this.schemeName = schemeName; }

    public String getSchemeCode() { return schemeCode; }
    public void setSchemeCode(String schemeCode) { this.schemeCode = schemeCode; }

    public List<ApplicationDocumentReviewDTO> getDocuments() { return documents; }
    public void setDocuments(List<ApplicationDocumentReviewDTO> documents) { this.documents = documents; }

    public List<EligibilityScoreDetailsDTO> getEligibilityDetails() { return eligibilityDetails; }
    public void setEligibilityDetails(List<EligibilityScoreDetailsDTO> eligibilityDetails) { this.eligibilityDetails = eligibilityDetails; }

    public List<VerificationHistoryDTO> getVerificationHistory() { return verificationHistory; }
    public void setVerificationHistory(List<VerificationHistoryDTO> verificationHistory) { this.verificationHistory = verificationHistory; }
}
