package com.wellsfargo.signaturestudio.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public class AlertRequestDTO {
    @NotBlank(message = "Template ID is required")
    private String templateId;
    
    @NotBlank(message = "Recipient email is required")
    private String recipientEmail;
    
    private String recipientName;
    private Map<String, String> templateVariables;
    private String transactionId;
    
    // Getters and Setters
    public String getTemplateId() {
        return templateId;
    }
    
    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }
    
    public String getRecipientEmail() {
        return recipientEmail;
    }
    
    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }
    
    public String getRecipientName() {
        return recipientName;
    }
    
    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }
    
    public Map<String, String> getTemplateVariables() {
        return templateVariables;
    }
    
    public void setTemplateVariables(Map<String, String> templateVariables) {
        this.templateVariables = templateVariables;
    }
    
    public String getTransactionId() {
        return transactionId;
    }
    
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }
}


