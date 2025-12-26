package com.wellsfargo.signaturestudio.controller;

import com.wellsfargo.signaturestudio.domain.Branding;
import com.wellsfargo.signaturestudio.service.BrandingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/branding")
public class BrandingController {
    
    private final BrandingService brandingService;
    
    public BrandingController(BrandingService brandingService) {
        this.brandingService = brandingService;
    }
    
    @GetMapping("/account/{accountId}")
    public ResponseEntity<Branding> getBrandingByAccountId(@PathVariable String accountId) {
        Branding branding = brandingService.getBrandingByAccountId(accountId);
        return ResponseEntity.ok(branding);
    }
    
    @GetMapping("/account-code/{code}")
    public ResponseEntity<Branding> getBrandingByAccountCode(@PathVariable String code) {
        Branding branding = brandingService.getBrandingByAccountCode(code);
        return ResponseEntity.ok(branding);
    }
    
    @PutMapping("/account/{accountId}")
    public ResponseEntity<Void> saveBranding(
            @PathVariable String accountId,
            @Valid @RequestBody Branding branding) {
        brandingService.saveBranding(accountId, branding);
        return ResponseEntity.ok().build();
    }
}


