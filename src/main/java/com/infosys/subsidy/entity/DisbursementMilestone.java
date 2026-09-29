package com.infosys.subsidy.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.infosys.subsidy.enums.MilestoneComplianceType;
import com.infosys.subsidy.enums.MilestoneStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "disbursement_milestones")
public class DisbursementMilestone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    @JsonBackReference
    private DisbursementPlan plan;

    @Column(name = "milestone_number", nullable = false)
    private Integer milestoneNumber;

    @Column(name = "milestone_name", nullable = false)
    private String milestoneName;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "compliance_type", nullable = false)
    private MilestoneComplianceType complianceType;

    @Column(name = "scheduled_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal scheduledAmount;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "due_date_offset_days")
    private Integer dueDateOffsetDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MilestoneStatus status;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "compliance_verified_at")
    private LocalDateTime complianceVerifiedAt;
    
    @Column(name = "compliance_verified_by")
    private Long complianceVerifiedBy;

    @Column(name = "remarks")
    private String remarks;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = MilestoneStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public DisbursementPlan getPlan() { return plan; }
    public void setPlan(DisbursementPlan plan) { this.plan = plan; }

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

    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }

    public Integer getDueDateOffsetDays() { return dueDateOffsetDays; }
    public void setDueDateOffsetDays(Integer dueDateOffsetDays) { this.dueDateOffsetDays = dueDateOffsetDays; }

    public MilestoneStatus getStatus() { return status; }
    public void setStatus(MilestoneStatus status) { this.status = status; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public LocalDateTime getComplianceVerifiedAt() { return complianceVerifiedAt; }
    public void setComplianceVerifiedAt(LocalDateTime complianceVerifiedAt) { this.complianceVerifiedAt = complianceVerifiedAt; }
    
    public Long getComplianceVerifiedBy() { return complianceVerifiedBy; }
    public void setComplianceVerifiedBy(Long complianceVerifiedBy) { this.complianceVerifiedBy = complianceVerifiedBy; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
