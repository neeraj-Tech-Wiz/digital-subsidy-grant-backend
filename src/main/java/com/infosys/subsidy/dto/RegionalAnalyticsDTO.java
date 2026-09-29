package com.infosys.subsidy.dto;

import java.math.BigDecimal;

public class RegionalAnalyticsDTO {

    private String region;
    private long applicationCount;
    private BigDecimal approvedAmount;
    private BigDecimal plannedAmount;
    private BigDecimal releasedAmount;
    private BigDecimal remainingAmount;
    private BigDecimal utilizationPercentage;

    private long fullyDisbursedCount;
    private long partiallyDisbursedCount;
    private long pendingCount;
    private long overdueCount;
    private long nonCompliantCount;

    public RegionalAnalyticsDTO() {
        this.approvedAmount = BigDecimal.ZERO;
        this.plannedAmount = BigDecimal.ZERO;
        this.releasedAmount = BigDecimal.ZERO;
        this.remainingAmount = BigDecimal.ZERO;
        this.utilizationPercentage = BigDecimal.ZERO;
    }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public long getApplicationCount() { return applicationCount; }
    public void setApplicationCount(long applicationCount) { this.applicationCount = applicationCount; }
    public void incrementApplicationCount() { this.applicationCount++; }

    public BigDecimal getApprovedAmount() { return approvedAmount; }
    public void setApprovedAmount(BigDecimal approvedAmount) { this.approvedAmount = approvedAmount; }
    public void addApprovedAmount(BigDecimal amount) { this.approvedAmount = this.approvedAmount.add(amount != null ? amount : BigDecimal.ZERO); }

    public BigDecimal getPlannedAmount() { return plannedAmount; }
    public void setPlannedAmount(BigDecimal plannedAmount) { this.plannedAmount = plannedAmount; }
    public void addPlannedAmount(BigDecimal amount) { this.plannedAmount = this.plannedAmount.add(amount != null ? amount : BigDecimal.ZERO); }

    public BigDecimal getReleasedAmount() { return releasedAmount; }
    public void setReleasedAmount(BigDecimal releasedAmount) { this.releasedAmount = releasedAmount; }
    public void addReleasedAmount(BigDecimal amount) { this.releasedAmount = this.releasedAmount.add(amount != null ? amount : BigDecimal.ZERO); }

    public BigDecimal getRemainingAmount() { return remainingAmount; }
    public void setRemainingAmount(BigDecimal remainingAmount) { this.remainingAmount = remainingAmount; }

    public BigDecimal getUtilizationPercentage() { return utilizationPercentage; }
    public void setUtilizationPercentage(BigDecimal utilizationPercentage) { this.utilizationPercentage = utilizationPercentage; }

    public void calculateDerived() {
        this.remainingAmount = this.approvedAmount.subtract(this.releasedAmount);
        if (this.remainingAmount.compareTo(BigDecimal.ZERO) < 0) {
            this.remainingAmount = BigDecimal.ZERO;
        }
        if (this.approvedAmount.compareTo(BigDecimal.ZERO) > 0) {
            this.utilizationPercentage = this.releasedAmount.divide(this.approvedAmount, 2, java.math.RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
        }
    }

    public long getFullyDisbursedCount() { return fullyDisbursedCount; }
    public void setFullyDisbursedCount(long fullyDisbursedCount) { this.fullyDisbursedCount = fullyDisbursedCount; }
    public void incrementFullyDisbursedCount() { this.fullyDisbursedCount++; }

    public long getPartiallyDisbursedCount() { return partiallyDisbursedCount; }
    public void setPartiallyDisbursedCount(long partiallyDisbursedCount) { this.partiallyDisbursedCount = partiallyDisbursedCount; }
    public void incrementPartiallyDisbursedCount() { this.partiallyDisbursedCount++; }

    public long getPendingCount() { return pendingCount; }
    public void setPendingCount(long pendingCount) { this.pendingCount = pendingCount; }
    public void incrementPendingCount() { this.pendingCount++; }

    public long getOverdueCount() { return overdueCount; }
    public void setOverdueCount(long overdueCount) { this.overdueCount = overdueCount; }
    public void incrementOverdueCount() { this.overdueCount++; }

    public long getNonCompliantCount() { return nonCompliantCount; }
    public void setNonCompliantCount(long nonCompliantCount) { this.nonCompliantCount = nonCompliantCount; }
    public void incrementNonCompliantCount() { this.nonCompliantCount++; }
}
