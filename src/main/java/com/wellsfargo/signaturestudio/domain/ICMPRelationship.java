package com.wellsfargo.signaturestudio.domain;

public class ICMPRelationship {
    private String id;
    private String icmpId;
    private String relationshipType; // ACCT, CUST
    private String accountNumber;
    private String customerUserId;
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getIcmpId() {
        return icmpId;
    }
    
    public void setIcmpId(String icmpId) {
        this.icmpId = icmpId;
    }
    
    public String getRelationshipType() {
        return relationshipType;
    }
    
    public void setRelationshipType(String relationshipType) {
        this.relationshipType = relationshipType;
    }
    
    public String getAccountNumber() {
        return accountNumber;
    }
    
    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }
    
    public String getCustomerUserId() {
        return customerUserId;
    }
    
    public void setCustomerUserId(String customerUserId) {
        this.customerUserId = customerUserId;
    }
}

