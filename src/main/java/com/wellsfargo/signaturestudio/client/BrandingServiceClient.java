package com.wellsfargo.signaturestudio.client;

import com.wellsfargo.signaturestudio.dto.BrandingDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

@Component
public class BrandingServiceClient {
    
    private static final Logger logger = LoggerFactory.getLogger(BrandingServiceClient.class);
    
    private final RestTemplate restTemplate;
    private final String baseUrl;
    
    public BrandingServiceClient(
            RestTemplate restTemplate,
            @Value("${branding.service.url:http://localhost:8083}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }
    
    public BrandingDTO getBrandingByAccountId(String accountId) {
        try {
            String url = baseUrl + "/api/v1/branding/account/" + accountId;
            logger.info("Calling Branding service to get branding by account ID: {}", url);
            
            ResponseEntity<BrandingDTO> response = restTemplate.getForEntity(url, BrandingDTO.class);
            return response.getBody();
        } catch (RestClientException e) {
            logger.error("Error calling Branding service to get branding by account ID: {}", accountId, e);
            throw new RuntimeException("Failed to get branding", e);
        }
    }
    
    public BrandingDTO getBrandingByAccountCode(String accountCode) {
        try {
            String url = baseUrl + "/api/v1/branding/account-code/" + accountCode;
            logger.info("Calling Branding service to get branding by account code: {}", url);
            
            ResponseEntity<BrandingDTO> response = restTemplate.getForEntity(url, BrandingDTO.class);
            return response.getBody();
        } catch (RestClientException e) {
            logger.error("Error calling Branding service to get branding by account code: {}", accountCode, e);
            throw new RuntimeException("Failed to get branding", e);
        }
    }
    
    public void saveBranding(String accountId, BrandingDTO branding) {
        try {
            String url = baseUrl + "/api/v1/branding/account/" + accountId;
            logger.info("Calling Branding service to save branding: {}", url);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            HttpEntity<BrandingDTO> request = new HttpEntity<>(branding, headers);
            
            restTemplate.exchange(url, HttpMethod.PUT, request, Void.class);
            logger.info("Branding saved successfully for account: {}", accountId);
        } catch (RestClientException e) {
            logger.error("Error calling Branding service to save branding for account: {}", accountId, e);
            throw new RuntimeException("Failed to save branding", e);
        }
    }
}


