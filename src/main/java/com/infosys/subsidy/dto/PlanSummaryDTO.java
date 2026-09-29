package com.infosys.subsidy.dto;

import com.infosys.subsidy.enums.ApplicationStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PlanSummaryDTO {
    private Long applicationId;
    private String applicationReference;
    private String beneficiaryName;
    private String schemeName;
    private String schemeCode;
    
    private BigDecimal approvedAmount;
    private BigDecimal plannedAmount;
    private BigDecimal releasedAmount;
    private BigDecimal remainingAmount;
    
    private ApplicationStatus status;
    private int milestoneCount;
    private int completedMilestones;
    private int pendingMilestones;
    private int overdueMilestones;
    
    private String nextMilestone;
    private LocalDateTime nextMilestoneDueDate;
    private int progressPercentage;

    public PlanSummaryDTO() {}

    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }

    public String getApplicationReference() { return applicationReference; }
    public void setApplicationReference(String applicationReference) { this.applicationReference = applicationReference; }

    public String getBeneficiaryName() { return beneficiaryName; }
    public void setBeneficiaryName(String beneficiaryName) { this.beneficiaryName = beneficiaryName; }

    public String getSchemeName() { return schemeName; }
    public void setSchemeName(String schemeName) { this.schemeName = schemeName; }

    public String getSchemeCode() { return schemeCode; }
    public void setSchemeCode(String schemeCode) { this.schemeCode = schemeCode; }

    public BigDecimal getApprovedAmount() { return approvedAmount; }
    public void setApprovedAmount(BigDecimal approvedAmount) { this.approvedAmount = approvedAmount; }

    public BigDecimal getPlannedAmount() { return plannedAmount; }
    public void setPlannedAmount(BigDecimal plannedAmount) { this.plannedAmount = plannedAmount; }

    public BigDecimal getReleasedAmount() { return releasedAmount; }
    public void setReleasedAmount(BigDecimal releasedAmount) { this.releasedAmount = releasedAmount; }

    public BigDecimal getRemainingAmount() { return remainingAmount; }
    public void setRemainingAmount(BigDecimal remainingAmount) { this.remainingAmount = remainingAmount; }

    public ApplicationStatus getStatus() { return status; }
    public void setStatus(ApplicationStatus status) { this.status = status; }

    public int getMilestoneCount() { return milestoneCount; }
    public void setMilestoneCount(int milestoneCount) { this.milestoneCount = milestoneCount; }

    public int getCompletedMilestones() { return completedMilestones; }
    public void setCompletedMilestones(int completedMilestones) { this.completedMilestones = completedMilestones; }

    public int getPendingMilestones() { return pendingMilestones; }
    public void setPendingMilestones(int pendingMilestones) { this.pendingMilestones = pendingMilestones; }

    public int getOverdueMilestones() { return overdueMilestones; }
    public void setOverdueMilestones(int overdueMilestones) { this.overdueMilestones = overdueMilestones; }

    public String getNextMilestone() { return nextMilestone; }
    public void setNextMilestone(String nextMilestone) { this.nextMilestone = nextMilestone; }

    public LocalDateTime getNextMilestoneDueDate() { return nextMilestoneDueDate; }
    public void setNextMilestoneDueDate(LocalDateTime nextMilestoneDueDate) { this.nextMilestoneDueDate = nextMilestoneDueDate; }

    public int getProgressPercentage() { return progressPercentage; }
    public void setProgressPercentage(int progressPercentage) { this.progressPercentage = progressPercentage; }
}
