package com.wellsfargo.signaturestudio.domain;

import java.util.Map;

public class Branding {
    private String accountId;
    private String accountCode;
    private String logoUrl;
    private String primaryColor;
    private String secondaryColor;
    private String companyName;
    private Map<String, String> customStyles;
    
    // Getters and Setters
    public String getAccountId() {
        return accountId;
    }
    
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }
    
    public String getAccountCode() {
        return accountCode;
    }
    
    public void setAccountCode(String accountCode) {
        this.accountCode = accountCode;
    }
    
    public String getLogoUrl() {
        return logoUrl;
    }
    
    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }
    
    public String getPrimaryColor() {
        return primaryColor;
    }
    
    public void setPrimaryColor(String primaryColor) {
        this.primaryColor = primaryColor;
    }
    
    public String getSecondaryColor() {
        return secondaryColor;
    }
    
    public void setSecondaryColor(String secondaryColor) {
        this.secondaryColor = secondaryColor;
    }
    
    public String getCompanyName() {
        return companyName;
    }
    
    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }
    
    public Map<String, String> getCustomStyles() {
        return customStyles;
    }
    
    public void setCustomStyles(Map<String, String> customStyles) {
        this.customStyles = customStyles;
    }
}

