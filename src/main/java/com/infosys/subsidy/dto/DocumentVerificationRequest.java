package com.infosys.subsidy.dto;

public class DocumentVerificationRequest {

    private boolean verified;
    private boolean reuploadRequired;
    private String remarks;

    public DocumentVerificationRequest() {}

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public boolean isReuploadRequired() {
        return reuploadRequired;
    }

    public void setReuploadRequired(boolean reuploadRequired) {
        this.reuploadRequired = reuploadRequired;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
