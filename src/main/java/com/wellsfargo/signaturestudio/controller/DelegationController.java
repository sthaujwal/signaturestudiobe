package com.wellsfargo.signaturestudio.controller;

import com.wellsfargo.signaturestudio.constants.SessionConstants;

import com.wellsfargo.signaturestudio.dto.DelegationDTO;
import com.wellsfargo.signaturestudio.service.DelegationService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/delegations")
public class DelegationController {
    
    private static final Logger logger = LoggerFactory.getLogger(DelegationController.class);
    
    private final DelegationService delegationService;
    
    public DelegationController(DelegationService delegationService) {
        this.delegationService = delegationService;
    }
    
    @PostMapping
    public ResponseEntity<DelegationDTO> createDelegation(
            @Valid @RequestBody DelegationDTO delegationDTO,
            HttpSession session) {
        String createdBy = (String) session.getAttribute(SessionConstants.USERNAME);
        if (createdBy == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        logger.info("Creating delegation from {} to {}", 
                delegationDTO.getDelegatorUserId(), delegationDTO.getDelegateUserId());
        DelegationDTO created = delegationService.createDelegation(delegationDTO, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @GetMapping("/delegator/{delegatorUserId}")
    public ResponseEntity<List<DelegationDTO>> getDelegationsByDelegator(
            @PathVariable String delegatorUserId,
            @RequestParam(required = false) String accountId) {
        logger.info("Fetching delegations for delegator: {} with account: {}", delegatorUserId, accountId);
        
        List<DelegationDTO> delegations;
        if (accountId != null) {
            delegations = delegationService.getActiveDelegationsByDelegator(delegatorUserId, accountId);
        } else {
            delegations = delegationService.getDelegationsByDelegator(delegatorUserId);
        }
        
        return ResponseEntity.ok(delegations);
    }
    
    @GetMapping("/delegate/{delegateUserId}")
    public ResponseEntity<List<DelegationDTO>> getDelegationsByDelegate(
            @PathVariable String delegateUserId) {
        logger.info("Fetching delegations for delegate: {}", delegateUserId);
        List<DelegationDTO> delegations = delegationService.getDelegationsByDelegate(delegateUserId);
        return ResponseEntity.ok(delegations);
    }
    
    @GetMapping("/active/delegator/{delegatorUserId}")
    public ResponseEntity<List<DelegationDTO>> getActiveDelegationsByDelegator(
            @PathVariable String delegatorUserId,
            @RequestParam(required = false) String accountId) {
        logger.info("Fetching active delegations for delegator: {} with account: {}", delegatorUserId, accountId);
        List<DelegationDTO> delegations = delegationService.getActiveDelegationsByDelegator(delegatorUserId, accountId);
        return ResponseEntity.ok(delegations);
    }
    
    @GetMapping("/active/delegate/{delegateUserId}")
    public ResponseEntity<List<DelegationDTO>> getActiveDelegationsByDelegate(
            @PathVariable String delegateUserId) {
        logger.info("Fetching active delegations for delegate: {}", delegateUserId);
        List<DelegationDTO> delegations = delegationService.getActiveDelegationsByDelegate(delegateUserId);
        return ResponseEntity.ok(delegations);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<DelegationDTO> updateDelegation(
            @PathVariable String id,
            @Valid @RequestBody DelegationDTO delegationDTO) {
        logger.info("Updating delegation: {}", id);
        DelegationDTO updated = delegationService.updateDelegation(id, delegationDTO);
        return ResponseEntity.ok(updated);
    }
    
    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelDelegation(@PathVariable String id) {
        logger.info("Cancelling delegation: {}", id);
        delegationService.cancelDelegation(id);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/expire")
    public ResponseEntity<Void> expireDelegations() {
        logger.info("Expiring delegations");
        delegationService.expireDelegations();
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/check")
    public ResponseEntity<Boolean> checkDelegation(
            @RequestParam String delegatorUserId,
            @RequestParam String delegateUserId,
            @RequestParam(required = false) String accountId) {
        logger.info("Checking delegation from {} to {} for account: {}", 
                delegatorUserId, delegateUserId, accountId);
        boolean hasDelegation = delegationService.hasActiveDelegation(
                delegatorUserId, delegateUserId, accountId);
        return ResponseEntity.ok(hasDelegation);
    }
    
    @GetMapping("/effective-user/{userId}")
    public ResponseEntity<String> getEffectiveUserId(
            @PathVariable String userId,
            @RequestParam(required = false) String accountId) {
        logger.info("Getting effective user ID for {} with account: {}", userId, accountId);
        String effectiveUserId = delegationService.getEffectiveUserId(userId, accountId);
        return ResponseEntity.ok(effectiveUserId);
    }
}

