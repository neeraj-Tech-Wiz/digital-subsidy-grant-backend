package com.infosys.subsidy.dto;

public class VerificationRequest {

    private Boolean approved;

    private String remarks;


    public VerificationRequest() {
    }


    public VerificationRequest(
            Boolean approved,
            String remarks) {

        this.approved = approved;
        this.remarks = remarks;
    }


    public Boolean getApproved() {
        return approved;
    }

    public void setApproved(Boolean approved) {
        this.approved = approved;
    }


    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}