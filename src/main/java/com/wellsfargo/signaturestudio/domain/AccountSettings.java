package com.wellsfargo.signaturestudio.domain;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Domain object for account settings
 */
public class AccountSettings {
    @NotBlank(message = "Company name is required")
    private String companyName;
    
    @Min(value = 1, message = "Default due days must be at least 1")
    private int defaultDueDays;
    
    private boolean requireAuthentication;
    private boolean allowDelegation;
    private boolean autoArchive;
    
    @Min(value = 1, message = "Retention days must be at least 1")
    private int retentionDays;
    
    // Getters and Setters
    public String getCompanyName() {
        return companyName;
    }
    
    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }
    
    public int getDefaultDueDays() {
        return defaultDueDays;
    }
    
    public void setDefaultDueDays(int defaultDueDays) {
        this.defaultDueDays = defaultDueDays;
    }
    
    public boolean isRequireAuthentication() {
        return requireAuthentication;
    }
    
    public void setRequireAuthentication(boolean requireAuthentication) {
        this.requireAuthentication = requireAuthentication;
    }
    
    public boolean isAllowDelegation() {
        return allowDelegation;
    }
    
    public void setAllowDelegation(boolean allowDelegation) {
        this.allowDelegation = allowDelegation;
    }
    
    public boolean isAutoArchive() {
        return autoArchive;
    }
    
    public void setAutoArchive(boolean autoArchive) {
        this.autoArchive = autoArchive;
    }
    
    public int getRetentionDays() {
        return retentionDays;
    }
    
    public void setRetentionDays(int retentionDays) {
        this.retentionDays = retentionDays;
    }
}

