package com.wellsfargo.signaturestudio.domain;

/**
 * Domain object representing a team member from Active Directory.
 */
public class TeamMember {
    
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private String displayName;
    private String department;
    private String title;
    private String employeeId;
    private String distinguishedName;
    
    public TeamMember() {
    }
    
    public TeamMember(String username, String email, String firstName, String lastName, 
                    String fullName, String displayName) {
        this.username = username;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.fullName = fullName;
        this.displayName = displayName;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
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
    
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDepartment() {
        return department;
    }
    
    public void setDepartment(String department) {
        this.department = department;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getEmployeeId() {
        return employeeId;
    }
    
    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }
    
    public String getDistinguishedName() {
        return distinguishedName;
    }
    
    public void setDistinguishedName(String distinguishedName) {
        this.distinguishedName = distinguishedName;
    }
}

