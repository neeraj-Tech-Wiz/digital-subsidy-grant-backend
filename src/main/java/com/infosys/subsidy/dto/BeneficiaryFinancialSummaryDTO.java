package com.infosys.subsidy.dto;

import java.math.BigDecimal;

public class BeneficiaryFinancialSummaryDTO {
    private BigDecimal approvedAmount;
    private BigDecimal releasedAmount;
    private BigDecimal remainingAmount;
    private BigDecimal utilizationPercentage;
    private String disbursementStatus;

    public BeneficiaryFinancialSummaryDTO() {}

    public BeneficiaryFinancialSummaryDTO(BigDecimal approvedAmount, BigDecimal releasedAmount, BigDecimal remainingAmount, BigDecimal utilizationPercentage, String disbursementStatus) {
        this.approvedAmount = approvedAmount;
        this.releasedAmount = releasedAmount;
        this.remainingAmount = remainingAmount;
        this.utilizationPercentage = utilizationPercentage;
        this.disbursementStatus = disbursementStatus;
    }

    public BigDecimal getApprovedAmount() { return approvedAmount; }
    public void setApprovedAmount(BigDecimal approvedAmount) { this.approvedAmount = approvedAmount; }

    public BigDecimal getReleasedAmount() { return releasedAmount; }
    public void setReleasedAmount(BigDecimal releasedAmount) { this.releasedAmount = releasedAmount; }

    public BigDecimal getRemainingAmount() { return remainingAmount; }
    public void setRemainingAmount(BigDecimal remainingAmount) { this.remainingAmount = remainingAmount; }

    public BigDecimal getUtilizationPercentage() { return utilizationPercentage; }
    public void setUtilizationPercentage(BigDecimal utilizationPercentage) { this.utilizationPercentage = utilizationPercentage; }

    public String getDisbursementStatus() { return disbursementStatus; }
    public void setDisbursementStatus(String disbursementStatus) { this.disbursementStatus = disbursementStatus; }
}
