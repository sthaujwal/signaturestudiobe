package com.wellsfargo.signaturestudio.domain;

import com.wellsfargo.signaturestudio.validation.NoXss;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new account.
 */
public class CreateAccountRequest {

    @NoXss
    @NotBlank(message = "Account name is required")
    @Size(min = 1, max = 255, message = "Account name must be between 1 and 255 characters")
    private String accountName;

    @NotBlank(message = "Account key is required")
    @Size(min = 2, max = 50, message = "Account key must be between 2 and 50 characters")
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "Account key must contain only uppercase letters, numbers, and underscores")
    private String accountKey;

    // Optional - for future use
    private AccountSettings accountSettings;

    public CreateAccountRequest() {
    }

    public CreateAccountRequest(String accountName, String accountKey) {
        this.accountName = accountName;
        this.accountKey = accountKey;
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

    public AccountSettings getAccountSettings() {
        return accountSettings;
    }

    public void setAccountSettings(AccountSettings accountSettings) {
        this.accountSettings = accountSettings;
    }
}
