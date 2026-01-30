package com.wellsfargo.signaturestudio.service;

import com.wellsfargo.signaturestudio.domain.Account;
import com.wellsfargo.signaturestudio.domain.ReportFilterCriteria;
import com.wellsfargo.signaturestudio.exception.ErrorCode;
import com.wellsfargo.signaturestudio.exception.ServiceException;
import com.wellsfargo.signaturestudio.model.Transaction;
import com.wellsfargo.signaturestudio.repository.TransactionRepository;
import com.wellsfargo.signaturestudio.repository.TransactionSpecifications;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
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
     * Filters are applied at the database level for optimal performance.
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

        List<String> accountIds = null;

        if (accountId != null && !accountId.isEmpty()) {
            // Query specific account - validate access first
            if (!accountService.hasAccountAccess(userId, accountId, session)) {
                throw new ServiceException(ErrorCode.ACCESS_DENIED,
                    "User does not have access to account: " + accountId);
            }
            accountIds = List.of(accountId);
            logger.debug("Querying single account: {}", accountId);

        } else if (includeAllAccounts) {
            // Query all accessible accounts
            accountIds = getAccessibleAccountIds(userId, session);
            if (accountIds.isEmpty()) {
                logger.warn("No accessible accounts found for user: {}", userId);
                return new ArrayList<>();
            }
            logger.debug("Querying {} accessible accounts", accountIds.size());
        }

        // Build specification with filters applied at database level
        Specification<Transaction> spec = TransactionSpecifications.forReport(accountIds, userId, filterCriteria);

        // Execute query with all filters applied in database
        List<Transaction> transactions = transactionRepository.findAll(spec);

        logger.info("Found {} transactions matching report criteria", transactions.size());
        return transactions;
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
