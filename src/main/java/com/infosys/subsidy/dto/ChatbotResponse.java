package com.infosys.subsidy.dto;

public class ChatbotResponse {
    private String reply;
    private String actionMetadata; // Optional metadata for the frontend to act upon

    public ChatbotResponse() {}

    public ChatbotResponse(String reply) {
        this.reply = reply;
    }

    public ChatbotResponse(String reply, String actionMetadata) {
        this.reply = reply;
        this.actionMetadata = actionMetadata;
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public String getActionMetadata() {
        return actionMetadata;
    }

    public void setActionMetadata(String actionMetadata) {
        this.actionMetadata = actionMetadata;
    }
}
