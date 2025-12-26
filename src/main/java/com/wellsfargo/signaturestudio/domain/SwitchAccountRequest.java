package com.wellsfargo.signaturestudio.domain;

import jakarta.validation.constraints.NotBlank;

/**
 * Domain object for account switching request.
 */
public class SwitchAccountRequest {
    
    @NotBlank(message = "Account ID is required")
    private String accountId;
    
    public String getAccountId() {
        return accountId;
    }
    
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }
}

