package com.wellsfargo.signaturestudio.controller;

import com.wellsfargo.signaturestudio.constants.SessionConstants;
import com.wellsfargo.signaturestudio.dto.AccountSettingsDTO;
import com.wellsfargo.signaturestudio.dto.EmailTemplateDTO;
import com.wellsfargo.signaturestudio.dto.NotificationSettingsDTO;
import com.wellsfargo.signaturestudio.service.AdminService;
import com.wellsfargo.signaturestudio.service.AlertService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for admin panel operations.
 * Handles account settings, email templates, and notification settings.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    
    private final AdminService adminService;
    private final AlertService alertService;
    
    public AdminController(AdminService adminService, AlertService alertService) {
        this.adminService = adminService;
        this.alertService = alertService;
    }
    
    /**
     * Get account settings for the current account
     */
    @GetMapping("/settings/account")
    public ResponseEntity<AccountSettingsDTO> getAccountSettings(
            @RequestParam(required = false) String accountId,
            HttpSession session) {
        String userId = (String) session.getAttribute(SessionConstants.USERNAME);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        String currentAccountId = accountId != null ? accountId : 
            (String) session.getAttribute(SessionConstants.ACCOUNT_ID);
        
        AccountSettingsDTO settings = adminService.getAccountSettings(currentAccountId);
        return ResponseEntity.ok(settings);
    }
    
    /**
     * Update account settings
     */
    @PutMapping("/settings/account")
    public ResponseEntity<Void> updateAccountSettings(
            @RequestParam(required = false) String accountId,
            @Valid @RequestBody AccountSettingsDTO settings,
            HttpSession session) {
        String userId = (String) session.getAttribute(SessionConstants.USERNAME);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        String currentAccountId = accountId != null ? accountId : 
            (String) session.getAttribute(SessionConstants.ACCOUNT_ID);
        
        adminService.updateAccountSettings(currentAccountId, settings);
        return ResponseEntity.ok().build();
    }
    
    /**
     * Get notification settings for the current account
     */
    @GetMapping("/settings/notifications")
    public ResponseEntity<NotificationSettingsDTO> getNotificationSettings(
            @RequestParam(required = false) String accountId,
            HttpSession session) {
        String userId = (String) session.getAttribute(SessionConstants.USERNAME);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        String currentAccountId = accountId != null ? accountId : 
            (String) session.getAttribute(SessionConstants.ACCOUNT_ID);
        
        NotificationSettingsDTO settings = adminService.getNotificationSettings(currentAccountId);
        return ResponseEntity.ok(settings);
    }
    
    /**
     * Update notification settings
     */
    @PutMapping("/settings/notifications")
    public ResponseEntity<Void> updateNotificationSettings(
            @RequestParam(required = false) String accountId,
            @Valid @RequestBody NotificationSettingsDTO settings,
            HttpSession session) {
        String userId = (String) session.getAttribute(SessionConstants.USERNAME);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        String currentAccountId = accountId != null ? accountId : 
            (String) session.getAttribute(SessionConstants.ACCOUNT_ID);
        
        adminService.updateNotificationSettings(currentAccountId, settings);
        return ResponseEntity.ok().build();
    }
    
    /**
     * Get all email templates
     */
    @GetMapping("/templates")
    public ResponseEntity<List<EmailTemplateDTO>> getEmailTemplates() {
        List<EmailTemplateDTO> templates = alertService.getTemplates();
        return ResponseEntity.ok(templates);
    }
    
    /**
     * Get a specific email template
     */
    @GetMapping("/templates/{id}")
    public ResponseEntity<EmailTemplateDTO> getEmailTemplate(@PathVariable String id) {
        EmailTemplateDTO template = alertService.getTemplate(id);
        return ResponseEntity.ok(template);
    }
    
    /**
     * Create a new email template
     */
    @PostMapping("/templates")
    public ResponseEntity<EmailTemplateDTO> createEmailTemplate(
            @Valid @RequestBody EmailTemplateDTO template) {
        EmailTemplateDTO created = alertService.createTemplate(template);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    /**
     * Update an email template
     */
    @PutMapping("/templates/{id}")
    public ResponseEntity<Void> updateEmailTemplate(
            @PathVariable String id,
            @Valid @RequestBody EmailTemplateDTO template) {
        alertService.updateTemplate(id, template);
        return ResponseEntity.ok().build();
    }
    
    /**
     * Delete an email template
     */
    @DeleteMapping("/templates/{id}")
    public ResponseEntity<Void> deleteEmailTemplate(@PathVariable String id) {
        alertService.deleteTemplate(id);
        return ResponseEntity.ok().build();
    }
}

