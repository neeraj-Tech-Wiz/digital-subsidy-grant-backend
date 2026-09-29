package com.infosys.subsidy.dto;

import com.infosys.subsidy.enums.MilestoneComplianceType;
import java.math.BigDecimal;

public class MilestoneRequestDTO {
    private Integer milestoneNumber;
    private String milestoneName;
    private String description;
    private MilestoneComplianceType complianceType;
    private BigDecimal scheduledAmount;
    private Integer dueDateOffsetDays;

    public MilestoneRequestDTO() {}

    public Integer getMilestoneNumber() { return milestoneNumber; }
    public void setMilestoneNumber(Integer milestoneNumber) { this.milestoneNumber = milestoneNumber; }

    public String getMilestoneName() { return milestoneName; }
    public void setMilestoneName(String milestoneName) { this.milestoneName = milestoneName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public MilestoneComplianceType getComplianceType() { return complianceType; }
    public void setComplianceType(MilestoneComplianceType complianceType) { this.complianceType = complianceType; }

    public BigDecimal getScheduledAmount() { return scheduledAmount; }
    public void setScheduledAmount(BigDecimal scheduledAmount) { this.scheduledAmount = scheduledAmount; }

    public Integer getDueDateOffsetDays() { return dueDateOffsetDays; }
    public void setDueDateOffsetDays(Integer dueDateOffsetDays) { this.dueDateOffsetDays = dueDateOffsetDays; }
}
