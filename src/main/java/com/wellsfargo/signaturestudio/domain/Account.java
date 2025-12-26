package com.wellsfargo.signaturestudio.domain;

/**
 * Domain object for account information.
 * Represents an account that a user can access and switch to.
 */
public class Account {
    private String accountId;
    private String accountCode;
    private String accountName;
    private String accountType; // e.g., "primary", "secondary", "delegated"
    private boolean isDefault; // Whether this is the user's default account
    
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
    
    public String getAccountName() {
        return accountName;
    }
    
    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }
    
    public String getAccountType() {
        return accountType;
    }
    
    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }
    
    public boolean isDefault() {
        return isDefault;
    }
    
    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }
}

