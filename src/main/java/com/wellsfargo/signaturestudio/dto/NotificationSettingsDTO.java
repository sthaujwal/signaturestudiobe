package com.wellsfargo.signaturestudio.dto;

/**
 * DTO for notification settings
 */
public class NotificationSettingsDTO {
    private boolean emailNotifications;
    private boolean smsNotifications;
    private String reminderFrequency; // hourly, daily, weekly
    private boolean escalationEnabled;
    private int escalationDays;
    
    // Getters and Setters
    public boolean isEmailNotifications() {
        return emailNotifications;
    }
    
    public void setEmailNotifications(boolean emailNotifications) {
        this.emailNotifications = emailNotifications;
    }
    
    public boolean isSmsNotifications() {
        return smsNotifications;
    }
    
    public void setSmsNotifications(boolean smsNotifications) {
        this.smsNotifications = smsNotifications;
    }
    
    public String getReminderFrequency() {
        return reminderFrequency;
    }
    
    public void setReminderFrequency(String reminderFrequency) {
        this.reminderFrequency = reminderFrequency;
    }
    
    public boolean isEscalationEnabled() {
        return escalationEnabled;
    }
    
    public void setEscalationEnabled(boolean escalationEnabled) {
        this.escalationEnabled = escalationEnabled;
    }
    
    public int getEscalationDays() {
        return escalationDays;
    }
    
    public void setEscalationDays(int escalationDays) {
        this.escalationDays = escalationDays;
    }
}

