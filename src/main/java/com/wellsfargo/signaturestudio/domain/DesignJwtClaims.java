package com.wellsfargo.signaturestudio.domain;

public class DesignJwtClaims {

    private final String jti;
    private final String transactionId;
    private final String accountId;
    private final String subject;

    public DesignJwtClaims(String jti, String transactionId, String accountId, String subject) {
        this.jti = jti;
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.subject = subject;
    }

    public String getJti() {
        return jti;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getSubject() {
        return subject;
    }
}
