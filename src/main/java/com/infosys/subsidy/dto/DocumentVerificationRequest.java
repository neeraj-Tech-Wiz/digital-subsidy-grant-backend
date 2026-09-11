package com.infosys.subsidy.dto;

public class DocumentVerificationRequest {

    private String action;
    private String remarks;

    public DocumentVerificationRequest() {}

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
