package com.wellsfargo.signaturestudio.controller;

import com.wellsfargo.signaturestudio.dto.BrandingDTO;
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
    public ResponseEntity<BrandingDTO> getBrandingByAccountId(@PathVariable String accountId) {
        BrandingDTO branding = brandingService.getBrandingByAccountId(accountId);
        return ResponseEntity.ok(branding);
    }
    
    @GetMapping("/account-code/{code}")
    public ResponseEntity<BrandingDTO> getBrandingByAccountCode(@PathVariable String code) {
        BrandingDTO branding = brandingService.getBrandingByAccountCode(code);
        return ResponseEntity.ok(branding);
    }
    
    @PutMapping("/account/{accountId}")
    public ResponseEntity<Void> saveBranding(
            @PathVariable String accountId,
            @Valid @RequestBody BrandingDTO branding) {
        brandingService.saveBranding(accountId, branding);
        return ResponseEntity.ok().build();
    }
}


