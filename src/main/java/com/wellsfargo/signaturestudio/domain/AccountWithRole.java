package com.wellsfargo.signaturestudio.domain;

import java.io.Serializable;

/**
 * Domain object representing an account with user's role in that account.
 * Used to return account information along with the user's role (ADMIN, SENDER, AUDIT, etc.)
 */
public class AccountWithRole implements Serializable {
    private static final long serialVersionUID = 1L;
    private String accountId;
    private String accountName;
    private String accountKey;
    private String role; // ADMIN, SENDER, AUDIT, READ_ONLY, etc.
    private String fullRoleName; // Full role name from Auth0 (e.g., DPD_SIGNATURE_STUDIO_TEST_ADMIN)
    
    public AccountWithRole() {
    }
    
    public AccountWithRole(String accountId, String accountName, String accountKey, String role, String fullRoleName) {
        this.accountId = accountId;
        this.accountName = accountName;
        this.accountKey = accountKey;
        this.role = role;
        this.fullRoleName = fullRoleName;
    }
    
    // Getters and Setters
    public String getAccountId() {
        return accountId;
    }
    
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }
    
    public String getAccountName() {
        return accountName;
    }
    
    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }
    
    public String getAccountKey() {
        return accountKey;
    }
    
    public void setAccountKey(String accountKey) {
        this.accountKey = accountKey;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    public String getFullRoleName() {
        return fullRoleName;
    }
    
    public void setFullRoleName(String fullRoleName) {
        this.fullRoleName = fullRoleName;
    }
}

