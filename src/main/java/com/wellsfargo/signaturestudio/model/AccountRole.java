package com.wellsfargo.signaturestudio.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "account_role")
public class AccountRole {
    @Id
    private String id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private AccountEntity account;
    
    @Column(name = "role_name", length = 255, nullable = false)
    private String roleName;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "modified_at", nullable = false)
    private Instant modifiedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        modifiedAt = Instant.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        modifiedAt = Instant.now();
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public AccountEntity getAccount() {
        return account;
    }
    
    public void setAccount(AccountEntity account) {
        this.account = account;
    }
    
    public String getRoleName() {
        return roleName;
    }
    
    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public Instant getModifiedAt() {
        return modifiedAt;
    }
    
    public void setModifiedAt(Instant modifiedAt) {
        this.modifiedAt = modifiedAt;
    }
}

