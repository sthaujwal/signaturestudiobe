package com.wellsfargo.signaturestudio.domain;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for exchanging authorization code for access token.
 */
public class TokenExchangeRequest {

    @NotBlank(message = "Authorization code is required")
    private String code;

    public TokenExchangeRequest() {
    }

    public TokenExchangeRequest(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
