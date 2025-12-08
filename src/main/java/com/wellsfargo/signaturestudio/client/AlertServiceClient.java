package com.wellsfargo.signaturestudio.client;

import com.wellsfargo.signaturestudio.dto.AlertRequestDTO;
import com.wellsfargo.signaturestudio.dto.EmailTemplateDTO;
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
    
    public void sendAlert(AlertRequestDTO alertRequest) {
        try {
            String url = baseUrl + "/api/v1/alerts/send";
            logger.info("Calling Alert service to send alert: {}", url);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            HttpEntity<AlertRequestDTO> request = new HttpEntity<>(alertRequest, headers);
            
            restTemplate.exchange(url, HttpMethod.POST, request, Void.class);
            logger.info("Alert sent successfully to: {}", alertRequest.getRecipientEmail());
        } catch (RestClientException e) {
            logger.error("Error calling Alert service to send alert", e);
            throw new RuntimeException("Failed to send alert", e);
        }
    }
    
    public List<EmailTemplateDTO> getTemplates() {
        try {
            String url = baseUrl + "/api/v1/templates";
            logger.info("Calling Alert service to get templates: {}", url);
            
            ResponseEntity<List<EmailTemplateDTO>> response = restTemplate.exchange(
                    url, HttpMethod.GET, null, 
                    new ParameterizedTypeReference<List<EmailTemplateDTO>>() {});
            
            return response.getBody();
        } catch (RestClientException e) {
            logger.error("Error calling Alert service to get templates", e);
            throw new RuntimeException("Failed to get email templates", e);
        }
    }
    
    public EmailTemplateDTO getTemplate(String templateId) {
        try {
            String url = baseUrl + "/api/v1/templates/" + templateId;
            logger.info("Calling Alert service to get template: {}", url);
            
            ResponseEntity<EmailTemplateDTO> response = restTemplate.getForEntity(url, EmailTemplateDTO.class);
            return response.getBody();
        } catch (RestClientException e) {
            logger.error("Error calling Alert service to get template: {}", templateId, e);
            throw new RuntimeException("Failed to get email template", e);
        }
    }
    
    public void updateTemplate(String templateId, EmailTemplateDTO template) {
        try {
            String url = baseUrl + "/api/v1/templates/" + templateId;
            logger.info("Calling Alert service to update template: {}", url);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            HttpEntity<EmailTemplateDTO> request = new HttpEntity<>(template, headers);
            
            restTemplate.exchange(url, HttpMethod.PUT, request, Void.class);
            logger.info("Template updated successfully: {}", templateId);
        } catch (RestClientException e) {
            logger.error("Error calling Alert service to update template: {}", templateId, e);
            throw new RuntimeException("Failed to update email template", e);
        }
    }
}

