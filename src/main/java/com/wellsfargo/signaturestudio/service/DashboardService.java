package com.wellsfargo.signaturestudio.service;

import com.wellsfargo.signaturestudio.dto.DashboardStatsDTO;
import com.wellsfargo.signaturestudio.model.Transaction;
import com.wellsfargo.signaturestudio.repository.TransactionRepository;
import com.wellsfargo.signaturestudio.repository.DelegationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import com.wellsfargo.signaturestudio.dto.DelegationDTO;

/**
 * Service for dashboard statistics and aggregated data.
 */
@Service
public class DashboardService {
    
    private static final Logger logger = LoggerFactory.getLogger(DashboardService.class);
    
    private final TransactionRepository transactionRepository;
    private final DelegationRepository delegationRepository;
    private final DelegationService delegationService;
    
    public DashboardService(
            TransactionRepository transactionRepository,
            DelegationRepository delegationRepository,
            DelegationService delegationService) {
        this.transactionRepository = transactionRepository;
        this.delegationRepository = delegationRepository;
        this.delegationService = delegationService;
    }
    
    /**
     * Get dashboard statistics for a user.
     * Includes transactions created by the user and delegated transactions.
     * 
     * @param userId The user ID
     * @param accountId Optional account ID to filter by
     * @return Dashboard statistics
     */
    public DashboardStatsDTO getDashboardStats(String userId, String accountId) {
        logger.info("Getting dashboard stats for user: {}, account: {}", userId, accountId);
        
        // Get user IDs for delegation support (same pattern as TransactionService)
        List<String> userIds = buildUserIdsWithDelegations(userId, accountId);
        
        // Get all transactions for the user (including delegated)
        List<Transaction> allTransactions;
        if (accountId != null && !accountId.isEmpty()) {
            allTransactions = transactionRepository.findByCreatedByInAndAccountId(userIds, accountId, 
                org.springframework.data.domain.Pageable.unpaged()).getContent();
        } else {
            allTransactions = transactionRepository.findByCreatedByIn(userIds, 
                org.springframework.data.domain.Pageable.unpaged()).getContent();
        }
        
        DashboardStatsDTO stats = new DashboardStatsDTO();
        
        // Basic counts
        stats.setTotalTransactions(allTransactions.size());
        stats.setPendingTransactions(countByStatus(allTransactions, "pending"));
        stats.setInProgressTransactions(countByStatus(allTransactions, "in-progress"));
        stats.setCompletedTransactions(countByStatus(allTransactions, "completed"));
        stats.setRejectedTransactions(countByStatus(allTransactions, "rejected"));
        
        // Transactions by status
        Map<String, Long> byStatus = allTransactions.stream()
            .collect(Collectors.groupingBy(
                t -> t.getStatus() != null ? t.getStatus() : "unknown",
                Collectors.counting()
            ));
        stats.setTransactionsByStatus(byStatus);
        
        // Transactions by priority
        Map<String, Long> byPriority = allTransactions.stream()
            .filter(t -> t.getPriority() != null)
            .collect(Collectors.groupingBy(
                Transaction::getPriority,
                Collectors.counting()
            ));
        stats.setTransactionsByPriority(byPriority);
        
        // Transactions this week
        Instant weekAgo = Instant.now().minus(7, ChronoUnit.DAYS);
        long thisWeek = allTransactions.stream()
            .filter(t -> t.getCreatedAt() != null && t.getCreatedAt().isAfter(weekAgo))
            .count();
        stats.setTransactionsThisWeek(thisWeek);
        
        // Completion rate
        long completed = stats.getCompletedTransactions();
        long total = stats.getTotalTransactions();
        double completionRate = total > 0 ? (completed * 100.0 / total) : 0.0;
        stats.setCompletionRate(completionRate);
        
        // Average processing time (for completed transactions)
        double avgProcessingTime = calculateAverageProcessingTime(
            allTransactions.stream()
                .filter(t -> "completed".equals(t.getStatus()))
                .filter(t -> t.getCreatedAt() != null && t.getUpdatedAt() != null)
                .collect(Collectors.toList())
        );
        stats.setAverageProcessingTimeDays(avgProcessingTime);
        
        logger.info("Dashboard stats calculated: total={}, pending={}, completed={}, rejected={}", 
            stats.getTotalTransactions(), stats.getPendingTransactions(), 
            stats.getCompletedTransactions(), stats.getRejectedTransactions());
        
        return stats;
    }
    
    private long countByStatus(List<Transaction> transactions, String status) {
        return transactions.stream()
            .filter(t -> status.equals(t.getStatus()))
            .count();
    }
    
    private double calculateAverageProcessingTime(List<Transaction> completedTransactions) {
        if (completedTransactions.isEmpty()) {
            return 0.0;
        }
        
        double totalDays = completedTransactions.stream()
            .mapToDouble(t -> {
                long seconds = ChronoUnit.SECONDS.between(t.getCreatedAt(), t.getUpdatedAt());
                return seconds / (24.0 * 3600.0); // Convert to days
            })
            .sum();
        
        return totalDays / completedTransactions.size();
    }
    
    /**
     * Build list of user IDs including delegations (same pattern as TransactionService)
     */
    private List<String> buildUserIdsWithDelegations(String userId, String accountId) {
        List<String> userIdsToQuery = new ArrayList<>();
        userIdsToQuery.add(userId);
        
        List<com.wellsfargo.signaturestudio.dto.DelegationDTO> activeDelegations = 
            delegationService.getActiveDelegationsByDelegate(userId);
        
        for (DelegationDTO delegation : activeDelegations) {
            if (isDelegationApplicable(delegation, accountId)) {
                userIdsToQuery.add(delegation.getDelegatorUserId());
            }
        }
        
        return userIdsToQuery;
    }
    
    private boolean isDelegationApplicable(DelegationDTO delegation, String accountId) {
        if (accountId == null || accountId.isEmpty()) {
            return true; // No account filter, include all delegations
        }
        
        // If delegation has accountId, it must match
        // If delegation has no accountId, it applies to all accounts
        return delegation.getAccountId() == null || 
               delegation.getAccountId().isEmpty() || 
               delegation.getAccountId().equals(accountId);
    }
}

