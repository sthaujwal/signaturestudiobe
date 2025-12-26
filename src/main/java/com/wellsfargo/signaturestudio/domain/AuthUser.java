package com.wellsfargo.signaturestudio.domain;

import java.util.List;

/**
 * Domain object representing an authenticated user from Auth0.
 * Contains the user's roles that start with DPD_SIGNATURE_STUDIO
 */
public class AuthUser {
    private String userId;
    private String email;
    private String username;
    private List<String> roles; // Roles that start with DPD_SIGNATURE_STUDIO
    
    public AuthUser() {
    }
    
    public AuthUser(String userId, String email, String username, List<String> roles) {
        this.userId = userId;
        this.email = email;
        this.username = username;
        this.roles = roles;
    }
    
    // Getters and Setters
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public List<String> getRoles() {
        return roles;
    }
    
    public void setRoles(List<String> roles) {
        this.roles = roles;
    }
}

