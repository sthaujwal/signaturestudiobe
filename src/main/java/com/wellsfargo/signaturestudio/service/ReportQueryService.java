package com.wellsfargo.signaturestudio.service;

import com.wellsfargo.signaturestudio.domain.Account;
import com.wellsfargo.signaturestudio.domain.ReportFilterCriteria;
import com.wellsfargo.signaturestudio.exception.ErrorCode;
import com.wellsfargo.signaturestudio.exception.ServiceException;
import com.wellsfargo.signaturestudio.model.Transaction;
import com.wellsfargo.signaturestudio.repository.TransactionRepository;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for querying data for reports.
 * Handles data fetching with proper access control and filtering.
 */
@Service
public class ReportQueryService {

    private static final Logger logger = LoggerFactory.getLogger(ReportQueryService.class);

    private final TransactionRepository transactionRepository;
    private final AccountService accountService;

    public ReportQueryService(TransactionRepository transactionRepository,
                              AccountService accountService) {
        this.transactionRepository = transactionRepository;
        this.accountService = accountService;
    }

    /**
     * Query transactions for a report with access control and filtering.
     *
     * @param accountId Account ID to filter by (null for all accessible accounts)
     * @param includeAllAccounts If true, include all accessible accounts (org admin only)
     * @param userId User requesting the report
     * @param session User session for access validation
     * @param filterCriteria Optional filter criteria (date ranges, status, etc.)
     * @return List of transactions the user has access to
     */
    public List<Transaction> queryTransactionsForReport(String accountId,
                                                        boolean includeAllAccounts,
                                                        String userId,
                                                        HttpSession session,
                                                        ReportFilterCriteria filterCriteria) {
        logger.debug("Querying transactions for report: accountId={}, includeAllAccounts={}, userId={}, hasFilters={}",
                    accountId, includeAllAccounts, userId, filterCriteria != null && filterCriteria.hasFilters());

        List<Transaction> transactions = new ArrayList<>();

        if (accountId != null && !accountId.isEmpty()) {
            // Query specific account
            // First validate user has access to this account
            if (!accountService.hasAccountAccess(userId, accountId, session)) {
                throw new ServiceException(ErrorCode.ACCESS_DENIED,
                    "User does not have access to account: " + accountId);
            }
            transactions = transactionRepository.findByAccountId(accountId);
            logger.debug("Found {} transactions for account {}", transactions.size(), accountId);

        } else if (includeAllAccounts) {
            // Query all accessible accounts
            List<String> accessibleAccountIds = getAccessibleAccountIds(userId, session);
            if (accessibleAccountIds.isEmpty()) {
                logger.warn("No accessible accounts found for user: {}", userId);
                return transactions;
            }

            // Query transactions for all accessible accounts
            for (String accId : accessibleAccountIds) {
                List<Transaction> accTransactions = transactionRepository.findByAccountId(accId);
                transactions.addAll(accTransactions);
            }
            logger.debug("Found {} transactions across {} accounts",
                        transactions.size(), accessibleAccountIds.size());

        } else {
            // Query user's created transactions (default behavior)
            transactions = transactionRepository.findByCreatedBy(userId);
            logger.debug("Found {} transactions created by user {}", transactions.size(), userId);
        }

        // Apply filters if provided
        if (filterCriteria != null && filterCriteria.hasFilters()) {
            transactions = applyFilters(transactions, filterCriteria);
            logger.debug("After applying filters: {} transactions remain", transactions.size());
        }

        return transactions;
    }

    /**
     * Apply filter criteria to transaction list.
     */
    private List<Transaction> applyFilters(List<Transaction> transactions, ReportFilterCriteria criteria) {
        return transactions.stream()
            .filter(txn -> matchesDateFilters(txn, criteria))
            .filter(txn -> matchesStatusFilter(txn, criteria))
            .filter(txn -> matchesPriorityFilter(txn, criteria))
            .filter(txn -> matchesFormTypeFilter(txn, criteria))
            .filter(txn -> matchesSearchText(txn, criteria))
            .collect(Collectors.toList());
    }

    /**
     * Check if transaction matches date range filters.
     */
    private boolean matchesDateFilters(Transaction txn, ReportFilterCriteria criteria) {
        // Created date filters
        if (criteria.getCreatedAfter() != null && txn.getCreatedAt() != null) {
            if (txn.getCreatedAt().isBefore(criteria.getCreatedAfter())) {
                return false;
            }
        }
        if (criteria.getCreatedBefore() != null && txn.getCreatedAt() != null) {
            if (txn.getCreatedAt().isAfter(criteria.getCreatedBefore())) {
                return false;
            }
        }

        // Updated date filters
        if (criteria.getUpdatedAfter() != null && txn.getUpdatedAt() != null) {
            if (txn.getUpdatedAt().isBefore(criteria.getUpdatedAfter())) {
                return false;
            }
        }
        if (criteria.getUpdatedBefore() != null && txn.getUpdatedAt() != null) {
            if (txn.getUpdatedAt().isAfter(criteria.getUpdatedBefore())) {
                return false;
            }
        }

        return true;
    }

    /**
     * Check if transaction matches status filter.
     */
    private boolean matchesStatusFilter(Transaction txn, ReportFilterCriteria criteria) {
        if (criteria.getStatuses() == null || criteria.getStatuses().isEmpty()) {
            return true;
        }
        return criteria.getStatuses().contains(txn.getStatus());
    }

    /**
     * Check if transaction matches priority filter.
     */
    private boolean matchesPriorityFilter(Transaction txn, ReportFilterCriteria criteria) {
        if (criteria.getPriorities() == null || criteria.getPriorities().isEmpty()) {
            return true;
        }
        return criteria.getPriorities().contains(txn.getPriority());
    }

    /**
     * Check if transaction matches form type filter.
     */
    private boolean matchesFormTypeFilter(Transaction txn, ReportFilterCriteria criteria) {
        if (criteria.getFormType() == null || criteria.getFormType().isEmpty()) {
            return true;
        }
        return criteria.getFormType().equals(txn.getFormType());
    }

    /**
     * Check if transaction matches search text (in title or description).
     */
    private boolean matchesSearchText(Transaction txn, ReportFilterCriteria criteria) {
        if (criteria.getSearchText() == null || criteria.getSearchText().isEmpty()) {
            return true;
        }

        String searchLower = criteria.getSearchText().toLowerCase();

        // Check title
        if (txn.getTitle() != null && txn.getTitle().toLowerCase().contains(searchLower)) {
            return true;
        }

        // Check description
        if (txn.getDescription() != null && txn.getDescription().toLowerCase().contains(searchLower)) {
            return true;
        }

        return false;
    }

    /**
     * Get list of account IDs the user has access to.
     */
    private List<String> getAccessibleAccountIds(String userId, HttpSession session) {
        try {
            List<Account> accounts = accountService.getUserAccounts(userId);
            return accounts.stream()
                .map(Account::getAccountId)
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error getting accessible accounts for user: {}", userId, e);
            throw new ServiceException(ErrorCode.INTERNAL_ERROR,
                "Failed to retrieve accessible accounts", e);
        }
    }

    /**
     * Count transactions for validation.
     */
    public long countTransactionsForReport(String accountId,
                                          boolean includeAllAccounts,
                                          String userId,
                                          HttpSession session,
                                          ReportFilterCriteria filterCriteria) {
        return queryTransactionsForReport(accountId, includeAllAccounts, userId, session, filterCriteria).size();
    }
}
