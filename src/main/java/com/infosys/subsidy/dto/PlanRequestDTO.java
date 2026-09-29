package com.infosys.subsidy.dto;

import java.util.List;

public class PlanRequestDTO {
    private List<MilestoneRequestDTO> milestones;

    public PlanRequestDTO() {}

    public List<MilestoneRequestDTO> getMilestones() {
        return milestones;
    }

    public void setMilestones(List<MilestoneRequestDTO> milestones) {
        this.milestones = milestones;
    }
}
