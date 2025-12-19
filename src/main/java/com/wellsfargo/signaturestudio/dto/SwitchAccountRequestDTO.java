package com.wellsfargo.signaturestudio.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for account switching request.
 */
public class SwitchAccountRequestDTO {
    
    @NotBlank(message = "Account ID is required")
    private String accountId;
    
    public String getAccountId() {
        return accountId;
    }
    
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }
}

