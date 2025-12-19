package com.wellsfargo.signaturestudio.service;

import com.wellsfargo.signaturestudio.dto.AccountDTO;
import com.wellsfargo.signaturestudio.exception.ErrorCode;
import com.wellsfargo.signaturestudio.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing user account access and validation.
 * 
 * Responsibilities:
 * - Retrieving user's available accounts
 * - Validating user has access to a specific account
 * - Managing account context
 * 
 * Note: This is a placeholder implementation. In production, this would:
 * - Integrate with an account management service or database
 * - Check user-account relationships/permissions
 * - Handle delegated accounts
 * - Cache account information for performance
 */
@Service
public class AccountService {
    
    private static final Logger logger = LoggerFactory.getLogger(AccountService.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("SECURITY_AUDIT");
    
    /**
     * Gets all accounts that the user has access to.
     * 
     * @param username The username
     * @return List of accounts the user can access
     */
    public List<AccountDTO> getUserAccounts(String username) {
        logger.debug("Getting accounts for user: {}", username);
        
        // TODO: In production, this would:
        // 1. Query account management service or database
        // 2. Check user-account relationships
        // 3. Include delegated accounts
        // 4. Return accounts with metadata (name, code, type, etc.)
        
        // Mock implementation - returns accounts based on username
        List<AccountDTO> accounts = new ArrayList<>();
        
        // Primary account (default)
        AccountDTO primaryAccount = new AccountDTO();
        primaryAccount.setAccountId("ACCT_" + username.hashCode());
        primaryAccount.setAccountCode("PRIMARY");
        primaryAccount.setAccountName("Primary Account");
        primaryAccount.setAccountType("primary");
        primaryAccount.setDefault(true);
        accounts.add(primaryAccount);
        
        // Example: Add secondary account if user has one
        // This would come from actual account service/database
        if (username.contains("admin") || username.contains("manager")) {
            AccountDTO secondaryAccount = new AccountDTO();
            secondaryAccount.setAccountId("ACCT_SECONDARY_" + username.hashCode());
            secondaryAccount.setAccountCode("SECONDARY");
            secondaryAccount.setAccountName("Secondary Account");
            secondaryAccount.setAccountType("secondary");
            secondaryAccount.setDefault(false);
            accounts.add(secondaryAccount);
        }
        
        logger.info("Found {} accounts for user: {}", accounts.size(), username);
        return accounts;
    }
    
    /**
     * Validates that the user has access to the specified account.
     * 
     * @param username The username
     * @param accountId The account ID to validate
     * @return true if user has access, false otherwise
     */
    public boolean hasAccountAccess(String username, String accountId) {
        if (username == null || accountId == null) {
            return false;
        }
        
        logger.debug("Validating account access: user={}, accountId={}", username, accountId);
        
        // Get user's available accounts
        List<AccountDTO> userAccounts = getUserAccounts(username);
        
        // Check if account is in the list
        boolean hasAccess = userAccounts.stream()
            .anyMatch(account -> account.getAccountId().equals(accountId));
        
        if (!hasAccess) {
            auditLogger.warn("ACCOUNT_ACCESS_DENIED | User: {} | AccountId: {} | Reason: Account not in user's accessible accounts",
                username, accountId);
        }
        
        return hasAccess;
    }
    
    /**
     * Gets account details by account ID.
     * Validates access before returning account information.
     * 
     * @param username The username
     * @param accountId The account ID
     * @return AccountDTO if user has access, null otherwise
     */
    public AccountDTO getAccount(String username, String accountId) {
        if (!hasAccountAccess(username, accountId)) {
            throw new ServiceException(ErrorCode.ACCESS_DENIED, 
                "User does not have access to account: " + accountId);
        }
        
        List<AccountDTO> userAccounts = getUserAccounts(username);
        return userAccounts.stream()
            .filter(account -> account.getAccountId().equals(accountId))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Gets the default account for a user.
     * 
     * @param username The username
     * @return Default account, or first available account if no default is set
     */
    public AccountDTO getDefaultAccount(String username) {
        List<AccountDTO> accounts = getUserAccounts(username);
        
        if (accounts.isEmpty()) {
            throw new ServiceException(ErrorCode.RESOURCE_NOT_FOUND, 
                "No accounts found for user: " + username);
        }
        
        // Return default account, or first account if no default
        return accounts.stream()
            .filter(AccountDTO::isDefault)
            .findFirst()
            .orElse(accounts.get(0));
    }
}

