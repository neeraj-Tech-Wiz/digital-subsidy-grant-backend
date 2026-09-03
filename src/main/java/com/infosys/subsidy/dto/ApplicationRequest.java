package com.infosys.subsidy.dto;

import java.util.Map;

public class ApplicationRequest {

    /*
     * Dynamic eligibility data.
     *
     * Example:
     *
     * {
     *   "Income": "150000",
     *   "Land Area": "4",
     *   "Marks": "85",
     *   "Attendance": "90"
     * }
     */
    private Map<String, String> eligibilityData;


    // =========================
    // GETTER
    // =========================

    public Map<String, String> getEligibilityData() {
        return eligibilityData;
    }


    // =========================
    // SETTER
    // =========================

    public void setEligibilityData(
            Map<String, String> eligibilityData) {

        this.eligibilityData = eligibilityData;
    }
}