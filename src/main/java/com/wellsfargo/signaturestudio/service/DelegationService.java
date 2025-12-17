package com.wellsfargo.signaturestudio.service;

import com.wellsfargo.signaturestudio.dto.DelegationDTO;
import com.wellsfargo.signaturestudio.exception.ErrorCode;
import com.wellsfargo.signaturestudio.exception.ServiceException;
import com.wellsfargo.signaturestudio.model.Delegation;
import com.wellsfargo.signaturestudio.repository.DelegationRepository;
import com.wellsfargo.signaturestudio.util.UpdateHelper;
import com.wellsfargo.signaturestudio.util.ValidationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DelegationService {
    
    private static final Logger logger = LoggerFactory.getLogger(DelegationService.class);
    
    private final DelegationRepository delegationRepository;
    
    public DelegationService(DelegationRepository delegationRepository) {
        this.delegationRepository = delegationRepository;
    }
    
    @Transactional
    public DelegationDTO createDelegation(DelegationDTO dto, String createdBy) {
        logger.info("Creating delegation from {} to {}", dto.getDelegatorUserId(), dto.getDelegateUserId());
        
        ValidationHelper.validateDateRange(dto.getStartDate(), dto.getEndDate());
        validateNoOverlappingDelegations(dto);
        
        Delegation delegation = buildDelegationEntity(dto, createdBy);
        Delegation saved = delegationRepository.save(delegation);
        logger.info("Delegation created: {}", saved.getId());
        
        return toDTO(saved);
    }
    
    private void validateNoOverlappingDelegations(DelegationDTO dto) {
        List<Delegation> existingDelegations = delegationRepository.findActiveDelegationsByDelegator(
            dto.getDelegatorUserId(), Instant.now());
        
        if (existingDelegations.isEmpty()) {
            return;
        }
        
        boolean hasOverlap = existingDelegations.stream()
            .filter(existing -> existing.getDelegateUserId().equals(dto.getDelegateUserId()))
            .anyMatch(existing -> hasDateOverlap(dto, existing));
        
        if (hasOverlap) {
            throw new ServiceException(ErrorCode.RESOURCE_CONFLICT, 
                "An active delegation already exists for this user and delegate with overlapping dates");
        }
    }
    
    private boolean hasDateOverlap(DelegationDTO newDelegation, Delegation existing) {
        Instant newStart = newDelegation.getStartDate();
        Instant newEnd = newDelegation.getEndDate();
        Instant existingStart = existing.getStartDate();
        Instant existingEnd = existing.getEndDate() != null 
            ? existing.getEndDate() 
            : Instant.now().plusSeconds(31536000L * 100); // 100 years in seconds
        
        return (newStart.isBefore(existingEnd) && newStart.isAfter(existingStart)) ||
               (newEnd != null && newEnd.isAfter(existingStart) && 
                (existing.getEndDate() == null || newEnd.isBefore(existingEnd))) ||
               (newStart.isBefore(existingStart) && 
                (newEnd == null || newEnd.isAfter(existingStart)));
    }
    
    private Delegation buildDelegationEntity(DelegationDTO dto, String createdBy) {
        Delegation delegation = new Delegation();
        delegation.setId(UUID.randomUUID().toString());
        delegation.setDelegatorUserId(dto.getDelegatorUserId());
        delegation.setDelegateUserId(dto.getDelegateUserId());
        delegation.setDelegatorEmail(dto.getDelegatorEmail());
        delegation.setDelegateEmail(dto.getDelegateEmail());
        delegation.setReason(dto.getReason());
        delegation.setDescription(dto.getDescription());
        delegation.setStartDate(dto.getStartDate());
        delegation.setEndDate(dto.getEndDate());
        delegation.setStatus("active");
        delegation.setAccountId(dto.getAccountId());
        delegation.setCreatedBy(createdBy);
        return delegation;
    }
    
    public List<DelegationDTO> getDelegationsByDelegator(String delegatorUserId) {
        List<Delegation> delegations = delegationRepository.findByDelegatorUserId(delegatorUserId);
        return delegations.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    public List<DelegationDTO> getDelegationsByDelegate(String delegateUserId) {
        List<Delegation> delegations = delegationRepository.findByDelegateUserId(delegateUserId);
        return delegations.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    public List<DelegationDTO> getActiveDelegationsByDelegator(String delegatorUserId, String accountId) {
        List<Delegation> delegations;
        if (accountId != null) {
            delegations = delegationRepository.findActiveDelegationsByDelegatorAndAccount(
                    delegatorUserId, accountId, Instant.now());
        } else {
            delegations = delegationRepository.findActiveDelegationsByDelegator(
                    delegatorUserId, Instant.now());
        }
        return delegations.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    public List<DelegationDTO> getActiveDelegationsByDelegate(String delegateUserId) {
        List<Delegation> delegations = delegationRepository.findActiveDelegationsByDelegate(
                delegateUserId, Instant.now());
        return delegations.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public DelegationDTO updateDelegation(String id, DelegationDTO dto) {
        Delegation delegation = delegationRepository.findById(id)
                .orElseThrow(() -> new ServiceException(ErrorCode.RESOURCE_NOT_FOUND, "Delegation not found: " + id));
        
        // Validate dates if provided
        UpdateHelper.setIfNotNull(dto.getStartDate(), delegation::setStartDate);
        
        UpdateHelper.ifNotNull(dto.getEndDate(), endDate -> {
            ValidationHelper.validateDateRange(delegation.getStartDate(), endDate);
            delegation.setEndDate(endDate);
        });
        
        UpdateHelper.setIfNotNull(dto.getReason(), delegation::setReason);
        UpdateHelper.setIfNotNull(dto.getDescription(), delegation::setDescription);
        UpdateHelper.setIfNotNull(dto.getStatus(), delegation::setStatus);
        UpdateHelper.setIfNotNull(dto.getAccountId(), delegation::setAccountId);
        
        Delegation savedDelegation = delegationRepository.save(delegation);
        logger.info("Delegation updated: {}", id);
        
        return toDTO(savedDelegation);
    }
    
    @Transactional
    public void cancelDelegation(String id) {
        Delegation delegation = delegationRepository.findById(id)
                .orElseThrow(() -> new ServiceException(ErrorCode.RESOURCE_NOT_FOUND, "Delegation not found: " + id));
        
        delegation.setStatus("cancelled");
        delegationRepository.save(delegation);
        logger.info("Delegation cancelled: {}", id);
    }
    
    @Transactional
    public void expireDelegations() {
        Instant now = Instant.now();
        List<Delegation> expiredDelegations = delegationRepository.findByStatus("active").stream()
                .filter(d -> d.getEndDate() != null && d.getEndDate().isBefore(now))
                .collect(Collectors.toList());
        
        for (Delegation delegation : expiredDelegations) {
            delegation.setStatus("expired");
            delegationRepository.save(delegation);
        }
        
        if (!expiredDelegations.isEmpty()) {
            logger.info("Expired {} delegations", expiredDelegations.size());
        }
    }
    
    /**
     * Check if a user has an active delegation for another user
     */
    public boolean hasActiveDelegation(String delegatorUserId, String delegateUserId, String accountId) {
        List<Delegation> delegations;
        if (accountId != null) {
            delegations = delegationRepository.findActiveDelegationsByDelegatorAndAccount(
                    delegatorUserId, accountId, Instant.now());
        } else {
            delegations = delegationRepository.findActiveDelegationsByDelegator(
                    delegatorUserId, Instant.now());
        }
        
        return delegations.stream()
                .anyMatch(d -> d.getDelegateUserId().equals(delegateUserId));
    }
    
    /**
     * Get the effective user ID considering delegations
     * If user has active delegation, return delegate user ID, otherwise return original user ID
     */
    public String getEffectiveUserId(String userId, String accountId) {
        List<Delegation> delegations;
        if (accountId != null) {
            delegations = delegationRepository.findActiveDelegationsByDelegatorAndAccount(
                    userId, accountId, Instant.now());
        } else {
            delegations = delegationRepository.findActiveDelegationsByDelegator(
                    userId, Instant.now());
        }
        
        // Return the first active delegate, or original user if no delegation
        if (!delegations.isEmpty()) {
            return delegations.get(0).getDelegateUserId();
        }
        
        return userId;
    }
    
    private DelegationDTO toDTO(Delegation delegation) {
        DelegationDTO dto = new DelegationDTO();
        dto.setId(delegation.getId());
        dto.setDelegatorUserId(delegation.getDelegatorUserId());
        dto.setDelegateUserId(delegation.getDelegateUserId());
        dto.setDelegatorEmail(delegation.getDelegatorEmail());
        dto.setDelegateEmail(delegation.getDelegateEmail());
        dto.setReason(delegation.getReason());
        dto.setDescription(delegation.getDescription());
        dto.setStartDate(delegation.getStartDate());
        dto.setEndDate(delegation.getEndDate());
        dto.setStatus(delegation.getStatus());
        dto.setAccountId(delegation.getAccountId());
        dto.setCreatedBy(delegation.getCreatedBy());
        dto.setCreatedAt(delegation.getCreatedAt());
        dto.setUpdatedAt(delegation.getUpdatedAt());
        return dto;
    }
}

