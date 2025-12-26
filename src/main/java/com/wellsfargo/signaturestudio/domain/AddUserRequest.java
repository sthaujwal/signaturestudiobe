package com.wellsfargo.signaturestudio.domain;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Request domain object for adding a user to a transaction.
 * Contains all required fields for user creation.
 */
public class AddUserRequest {
    
    @Size(max = 255, message = "First name must not exceed 255 characters")
    private String firstName;
    
    @Size(max = 255, message = "Last name must not exceed 255 characters")
    private String lastName;
    
    @Size(max = 500, message = "Full name must not exceed 500 characters")
    private String fullName;
    
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name; // Keep for backward compatibility
    
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;
    
    @Size(max = 50, message = "Phone number must not exceed 50 characters")
    private String phoneNumber;
    
    private String uniqueId;
    
    @Size(max = 255, message = "External ID must not exceed 255 characters")
    private String externalId;
    
    /**
     * External ID Type - must be either "AD-ENT" or "ECN"
     */
    private String externalIdType;
    
    /**
     * Authentication type - required for user creation
     */
    private String authType;
    
    private String role; // signer, reviewer, approver (kept for backward compatibility)
    
    private Integer signingOrder;
    
    private String type; // Approver, Signer, Reviewer, etc.
    
    private String userCategory; // team-member, customer
    
    // Getters and Setters
    public String getFirstName() {
        return firstName;
    }
    
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    
    public String getFullName() {
        return fullName;
    }
    
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    public String getUniqueId() {
        return uniqueId;
    }
    
    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }
    
    public String getExternalId() {
        return externalId;
    }
    
    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }
    
    public String getExternalIdType() {
        return externalIdType;
    }
    
    public void setExternalIdType(String externalIdType) {
        this.externalIdType = externalIdType;
    }
    
    public String getAuthType() {
        return authType;
    }
    
    public void setAuthType(String authType) {
        this.authType = authType;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    public Integer getSigningOrder() {
        return signingOrder;
    }
    
    public void setSigningOrder(Integer signingOrder) {
        this.signingOrder = signingOrder;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getUserCategory() {
        return userCategory;
    }
    
    public void setUserCategory(String userCategory) {
        this.userCategory = userCategory;
    }
}

