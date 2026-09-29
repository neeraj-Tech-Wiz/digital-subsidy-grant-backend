package com.infosys.subsidy.dto;

import com.infosys.subsidy.enums.ApplicationStatus;

import java.math.BigDecimal;
import java.util.List;

public class PlanResponseDTO {
    private Long applicationId;
    private BigDecimal approvedAmount;
    private BigDecimal plannedAmount;
    private BigDecimal releasedAmount;
    private BigDecimal remainingAmount;
    private ApplicationStatus status;
    private List<MilestoneResponseDTO> milestones;

    public PlanResponseDTO() {}

    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }

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

    public List<MilestoneResponseDTO> getMilestones() { return milestones; }
    public void setMilestones(List<MilestoneResponseDTO> milestones) { this.milestones = milestones; }
}
