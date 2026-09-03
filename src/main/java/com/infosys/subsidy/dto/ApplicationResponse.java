package com.infosys.subsidy.dto;

public class ApplicationResponse {

    private Long applicationId;

    private Long schemeId;

    private Integer eligibilityScore;

    private String status;

    private String message;


    public ApplicationResponse() {
    }


    public ApplicationResponse(
            Long applicationId,
            Long schemeId,
            Integer eligibilityScore,
            String status,
            String message) {

        this.applicationId = applicationId;
        this.schemeId = schemeId;
        this.eligibilityScore = eligibilityScore;
        this.status = status;
        this.message = message;
    }


    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }


    public Long getSchemeId() {
        return schemeId;
    }

    public void setSchemeId(Long schemeId) {
        this.schemeId = schemeId;
    }


    public Integer getEligibilityScore() {
        return eligibilityScore;
    }

    public void setEligibilityScore(Integer eligibilityScore) {
        this.eligibilityScore = eligibilityScore;
    }


    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }


    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}