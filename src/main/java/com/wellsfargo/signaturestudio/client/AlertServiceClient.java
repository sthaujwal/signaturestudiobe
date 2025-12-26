package com.wellsfargo.signaturestudio.client;

import com.wellsfargo.signaturestudio.domain.AlertRequest;
import com.wellsfargo.signaturestudio.domain.EmailTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Component
public class AlertServiceClient {
    
    private static final Logger logger = LoggerFactory.getLogger(AlertServiceClient.class);
    
    private final RestTemplate restTemplate;
    private final String baseUrl;
    
    public AlertServiceClient(
            RestTemplate restTemplate,
            @Value("${alert.service.url:http://localhost:8082}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }
    
    public void sendAlert(AlertRequest alertRequest) {
        try {
            String url = baseUrl + "/api/v1/alerts/send";
            logger.info("Calling Alert service to send alert: {}", url);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            HttpEntity<AlertRequest> request = new HttpEntity<>(alertRequest, headers);
            
            restTemplate.exchange(url, HttpMethod.POST, request, Void.class);
            logger.info("Alert sent successfully to: {}", alertRequest.getRecipientEmail());
        } catch (RestClientException e) {
            logger.error("Error calling Alert service to send alert", e);
            throw new RuntimeException("Failed to send alert", e);
        }
    }
    
    public List<EmailTemplate> getTemplates() {
        try {
            String url = baseUrl + "/api/v1/templates";
            logger.info("Calling Alert service to get templates: {}", url);
            
            ResponseEntity<List<EmailTemplate>> response = restTemplate.exchange(
                    url, HttpMethod.GET, null, 
                    new ParameterizedTypeReference<List<EmailTemplate>>() {});
            
            return response.getBody();
        } catch (RestClientException e) {
            logger.error("Error calling Alert service to get templates", e);
            throw new RuntimeException("Failed to get email templates", e);
        }
    }
    
    public EmailTemplate getTemplate(String templateId) {
        try {
            String url = baseUrl + "/api/v1/templates/" + templateId;
            logger.info("Calling Alert service to get template: {}", url);
            
            ResponseEntity<EmailTemplate> response = restTemplate.getForEntity(url, EmailTemplate.class);
            return response.getBody();
        } catch (RestClientException e) {
            logger.error("Error calling Alert service to get template: {}", templateId, e);
            throw new RuntimeException("Failed to get email template", e);
        }
    }
    
    public void updateTemplate(String templateId, EmailTemplate template) {
        try {
            String url = baseUrl + "/api/v1/templates/" + templateId;
            logger.info("Calling Alert service to update template: {}", url);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            HttpEntity<EmailTemplate> request = new HttpEntity<>(template, headers);
            
            restTemplate.exchange(url, HttpMethod.PUT, request, Void.class);
            logger.info("Template updated successfully: {}", templateId);
        } catch (RestClientException e) {
            logger.error("Error calling Alert service to update template: {}", templateId, e);
            throw new RuntimeException("Failed to update email template", e);
        }
    }
    
    public EmailTemplate createTemplate(EmailTemplate template) {
        try {
            String url = baseUrl + "/api/v1/templates";
            logger.info("Calling Alert service to create template: {}", url);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            HttpEntity<EmailTemplate> request = new HttpEntity<>(template, headers);
            
            ResponseEntity<EmailTemplate> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, EmailTemplate.class);
            logger.info("Template created successfully");
            return response.getBody();
        } catch (RestClientException e) {
            logger.error("Error calling Alert service to create template", e);
            throw new RuntimeException("Failed to create email template", e);
        }
    }
    
    public void deleteTemplate(String templateId) {
        try {
            String url = baseUrl + "/api/v1/templates/" + templateId;
            logger.info("Calling Alert service to delete template: {}", url);
            
            restTemplate.exchange(url, HttpMethod.DELETE, null, Void.class);
            logger.info("Template deleted successfully: {}", templateId);
        } catch (RestClientException e) {
            logger.error("Error calling Alert service to delete template: {}", templateId, e);
            throw new RuntimeException("Failed to delete email template", e);
        }
    }
}

