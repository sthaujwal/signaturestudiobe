package com.wellsfargo.signaturestudio.model;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {
    @Id
    private String id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;
    
    @Column(name = "first_name", length = 255)
    private String firstName;
    
    @Column(name = "last_name", length = 255)
    private String lastName;
    
    @Column(name = "full_name", length = 500)
    private String fullName;
    
    @Column(length = 255)
    private String name; // Keep for backward compatibility
    
    @Column(length = 255)
    private String email;
    
    @Column(name = "phone_number", length = 50)
    private String phoneNumber;
    
    @Column(name = "unique_id", length = 255)
    private String uniqueId;
    
    @Column(name = "external_id", length = 255)
    private String externalId;
    
    @Column(length = 50)
    private String role; // signer, reviewer, approver (kept for backward compatibility)
    
    @Column(name = "signing_order")
    private Integer signingOrder;
    
    @Column(length = 50)
    private String type; // Approver, Signer, Reviewer, etc.
    
    @Column(name = "user_category", length = 50)
    private String userCategory; // team-member, customer
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public Transaction getTransaction() {
        return transaction;
    }
    
    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }
    
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

