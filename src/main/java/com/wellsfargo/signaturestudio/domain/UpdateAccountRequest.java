package com.wellsfargo.signaturestudio.domain;

import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating an existing account.
 */
public class UpdateAccountRequest {

    @Size(min = 1, max = 255, message = "Account name must be between 1 and 255 characters")
    private String accountName;

    // Optional - for updating account settings
    private AccountSettings accountSettings;

    public UpdateAccountRequest() {
    }

    public UpdateAccountRequest(String accountName) {
        this.accountName = accountName;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public AccountSettings getAccountSettings() {
        return accountSettings;
    }

    public void setAccountSettings(AccountSettings accountSettings) {
        this.accountSettings = accountSettings;
    }
}
