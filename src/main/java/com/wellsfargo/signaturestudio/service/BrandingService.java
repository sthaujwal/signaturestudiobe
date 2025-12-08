package com.wellsfargo.signaturestudio.service;

import com.wellsfargo.signaturestudio.client.BrandingServiceClient;
import com.wellsfargo.signaturestudio.dto.BrandingDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BrandingService {
    
    private static final Logger logger = LoggerFactory.getLogger(BrandingService.class);
    
    private final BrandingServiceClient brandingServiceClient;
    
    public BrandingService(BrandingServiceClient brandingServiceClient) {
        this.brandingServiceClient = brandingServiceClient;
    }
    
    public BrandingDTO getBrandingByAccountId(String accountId) {
        logger.info("Getting branding for account ID: {}", accountId);
        return brandingServiceClient.getBrandingByAccountId(accountId);
    }
    
    public BrandingDTO getBrandingByAccountCode(String accountCode) {
        logger.info("Getting branding for account code: {}", accountCode);
        return brandingServiceClient.getBrandingByAccountCode(accountCode);
    }
    
    public void saveBranding(String accountId, BrandingDTO branding) {
        logger.info("Saving branding for account ID: {}", accountId);
        brandingServiceClient.saveBranding(accountId, branding);
    }
}


