package com.wellsfargo.signaturestudio.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "accounts")
public class AccountEntity {
    @Id
    private String id;
    
    @Column(name = "account_name", length = 255, nullable = false)
    private String accountName;
    
    @Column(name = "account_id", length = 255, unique = true, nullable = false)
    private String accountId;
    
    @Column(name = "account_key", length = 255, unique = true, nullable = false)
    private String accountKey;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "modified_at", nullable = false)
    private Instant modifiedAt;
    
    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<AccountRole> roles = new ArrayList<>();
    
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
    
    public String getAccountName() {
        return accountName;
    }
    
    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }
    
    public String getAccountId() {
        return accountId;
    }
    
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }
    
    public String getAccountKey() {
        return accountKey;
    }
    
    public void setAccountKey(String accountKey) {
        this.accountKey = accountKey;
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
    
    public List<AccountRole> getRoles() {
        return roles;
    }
    
    public void setRoles(List<AccountRole> roles) {
        this.roles = roles;
    }
}

