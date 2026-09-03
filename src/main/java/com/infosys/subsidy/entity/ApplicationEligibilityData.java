package com.infosys.subsidy.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "application_eligibility_data")
public class ApplicationEligibilityData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Which application this answer belongs to
    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    // Example: Income, Marks, Attendance, Land Area
    @Column(name = "criterion_name", nullable = false)
    private String criterionName;

    // Example: 150000, 85, true, FARMER
    @Column(name = "actual_value", nullable = false)
    private String actualValue;


    // =========================
    // CONSTRUCTOR
    // =========================

    public ApplicationEligibilityData() {
    }

    public ApplicationEligibilityData(
            Long applicationId,
            String criterionName,
            String actualValue) {

        this.applicationId = applicationId;
        this.criterionName = criterionName;
        this.actualValue = actualValue;
    }


    // =========================
    // GETTERS AND SETTERS
    // =========================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public String getCriterionName() {
        return criterionName;
    }

    public void setCriterionName(String criterionName) {
        this.criterionName = criterionName;
    }

    public String getActualValue() {
        return actualValue;
    }

    public void setActualValue(String actualValue) {
        this.actualValue = actualValue;
    }
}