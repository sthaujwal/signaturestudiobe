package com.wellsfargo.signaturestudio.service;

import com.wellsfargo.signaturestudio.client.AlertServiceClient;
import com.wellsfargo.signaturestudio.domain.AlertRequest;
import com.wellsfargo.signaturestudio.domain.EmailTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AlertService {
    
    private static final Logger logger = LoggerFactory.getLogger(AlertService.class);
    
    private final AlertServiceClient alertServiceClient;
    
    public AlertService(AlertServiceClient alertServiceClient) {
        this.alertServiceClient = alertServiceClient;
    }
    
    public void sendAlert(AlertRequest alertRequest) {
        logger.info("Sending alert to: {}", alertRequest.getRecipientEmail());
        alertServiceClient.sendAlert(alertRequest);
    }
    
    public List<EmailTemplate> getTemplates() {
        logger.info("Getting email templates");
        return alertServiceClient.getTemplates();
    }
    
    public EmailTemplate getTemplate(String templateId) {
        logger.info("Getting email template: {}", templateId);
        return alertServiceClient.getTemplate(templateId);
    }
    
    public void updateTemplate(String templateId, EmailTemplate template) {
        logger.info("Updating email template: {}", templateId);
        alertServiceClient.updateTemplate(templateId, template);
    }
    
    public EmailTemplate createTemplate(EmailTemplate template) {
        logger.info("Creating email template: {}", template.getName());
        return alertServiceClient.createTemplate(template);
    }
    
    public void deleteTemplate(String templateId) {
        logger.info("Deleting email template: {}", templateId);
        alertServiceClient.deleteTemplate(templateId);
    }
}


