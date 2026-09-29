package com.infosys.subsidy.dto;

import com.infosys.subsidy.enums.MilestoneComplianceType;
import com.infosys.subsidy.enums.MilestoneStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MilestoneResponseDTO {
    private Long id;
    private Integer number;
    private String name;
    private String description;
    private BigDecimal amount;
    private LocalDateTime dueDate;
    private MilestoneStatus status;
    private MilestoneComplianceType complianceType;
    private LocalDateTime complianceVerifiedAt;
    private String remarks;

    public MilestoneResponseDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getNumber() { return number; }
    public void setNumber(Integer number) { this.number = number; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }

    public MilestoneStatus getStatus() { return status; }
    public void setStatus(MilestoneStatus status) { this.status = status; }

    public MilestoneComplianceType getComplianceType() { return complianceType; }
    public void setComplianceType(MilestoneComplianceType complianceType) { this.complianceType = complianceType; }

    public LocalDateTime getComplianceVerifiedAt() { return complianceVerifiedAt; }
    public void setComplianceVerifiedAt(LocalDateTime complianceVerifiedAt) { this.complianceVerifiedAt = complianceVerifiedAt; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
