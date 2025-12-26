package com.wellsfargo.signaturestudio.service;

import com.wellsfargo.signaturestudio.domain.Account;
import com.wellsfargo.signaturestudio.domain.AccountWithRole;
import com.wellsfargo.signaturestudio.domain.AuthUser;
import com.wellsfargo.signaturestudio.exception.ErrorCode;
import com.wellsfargo.signaturestudio.exception.ServiceException;
import com.wellsfargo.signaturestudio.model.AccountEntity;
import com.wellsfargo.signaturestudio.repository.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
    private static final String ROLE_PREFIX = "DPD_SIGNATURE_STUDIO_";
    
    private final AccountRepository accountRepository;
    
    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }
    
    /**
     * Gets all accounts that the user has access to.
     * 
     * @param username The username
     * @return List of accounts the user can access
     */
    public List<Account> getUserAccounts(String username) {
        logger.debug("Getting accounts for user: {}", username);
        
        // TODO: In production, this would:
        // 1. Query account management service or database
        // 2. Check user-account relationships
        // 3. Include delegated accounts
        // 4. Return accounts with metadata (name, code, type, etc.)
        
        // Mock implementation - returns accounts based on username
        List<Account> accounts = new ArrayList<>();
        
        // Primary account (default)
        Account primaryAccount = new Account();
        primaryAccount.setAccountId("ACCT_" + username.hashCode());
        primaryAccount.setAccountCode("PRIMARY");
        primaryAccount.setAccountName("Primary Account");
        primaryAccount.setAccountType("primary");
        primaryAccount.setDefault(true);
        accounts.add(primaryAccount);
        
        // Example: Add secondary account if user has one
        // This would come from actual account service/database
        if (username.contains("admin") || username.contains("manager")) {
            Account secondaryAccount = new Account();
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
        List<Account> userAccounts = getUserAccounts(username);
        
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
     * @return Account if user has access, null otherwise
     */
    public Account getAccount(String username, String accountId) {
        if (!hasAccountAccess(username, accountId)) {
            throw new ServiceException(ErrorCode.ACCESS_DENIED, 
                "User does not have access to account: " + accountId);
        }
        
        List<Account> userAccounts = getUserAccounts(username);
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
    public Account getDefaultAccount(String username) {
        List<Account> accounts = getUserAccounts(username);
        
        if (accounts.isEmpty()) {
            throw new ServiceException(ErrorCode.RESOURCE_NOT_FOUND, 
                "No accounts found for user: " + username);
        }
        
        // Return default account, or first account if no default
        return accounts.stream()
            .filter(Account::isDefault)
            .findFirst()
            .orElse(accounts.get(0));
    }
    
    /**
     * Parse roles from AuthUser and return accounts with their roles.
     * Role format: DPD_SIGNATURE_STUDIO_{ACCOUNT_KEY}_{ROLE}
     * Examples:
     * - DPD_SIGNATURE_STUDIO_TEST_ADMIN -> Account: TEST, Role: ADMIN
     * - DPD_SIGNATURE_STUDIO_TEST2_SENDER -> Account: TEST2, Role: SENDER
     * - DPD_SIGNATURE_STUDIO_TEST_READ_ONLY -> Account: TEST, Role: READ_ONLY
     * 
     * @param authUser The authenticated user with roles
     * @return List of accounts with user's role in each account
     */
    public List<AccountWithRole> getAccountsWithRoles(AuthUser authUser) {
        logger.info("Getting accounts with roles for user: {}", authUser.getUserId());
        
        if (authUser.getRoles() == null || authUser.getRoles().isEmpty()) {
            logger.warn("No roles found for user: {}", authUser.getUserId());
            return new ArrayList<>();
        }
        
        List<AccountWithRole> accountsWithRoles = new ArrayList<>();
        
        // Filter roles that start with DPD_SIGNATURE_STUDIO_
        List<String> signatureStudioRoles = authUser.getRoles().stream()
            .filter(role -> role.startsWith(ROLE_PREFIX))
            .collect(Collectors.toList());
        
        logger.debug("Found {} roles starting with {} for user: {}", 
            signatureStudioRoles.size(), ROLE_PREFIX, authUser.getUserId());
        
        // Parse each role to extract account and role type
        for (String fullRoleName : signatureStudioRoles) {
            try {
                RoleParseResult parseResult = parseRole(fullRoleName);
                
                // Find account in database by account key
                AccountEntity accountEntity = accountRepository.findByAccountKey(parseResult.getAccountKey())
                    .orElse(null);
                
                if (accountEntity == null) {
                    logger.warn("Account not found for account key: {} (from role: {})", 
                        parseResult.getAccountKey(), fullRoleName);
                    continue;
                }
                
                // Create AccountWithRole object
                AccountWithRole accountWithRole = new AccountWithRole();
                accountWithRole.setAccountId(accountEntity.getAccountId());
                accountWithRole.setAccountName(accountEntity.getAccountName());
                accountWithRole.setAccountKey(accountEntity.getAccountKey());
                accountWithRole.setRole(parseResult.getRoleType());
                accountWithRole.setFullRoleName(fullRoleName);
                
                accountsWithRoles.add(accountWithRole);
                
                logger.debug("Parsed role: {} -> Account: {}, Role: {}", 
                    fullRoleName, parseResult.getAccountKey(), parseResult.getRoleType());
                
            } catch (Exception e) {
                logger.error("Error parsing role: {}", fullRoleName, e);
            }
        }
        
        logger.info("Found {} accounts with roles for user: {}", 
            accountsWithRoles.size(), authUser.getUserId());
        
        return accountsWithRoles;
    }
    
    /**
     * Parse a role name to extract account key and role type.
     * Role format: DPD_SIGNATURE_STUDIO_{ACCOUNT_KEY}_{ROLE_TYPE}
     * 
     * @param fullRoleName Full role name (e.g., DPD_SIGNATURE_STUDIO_TEST_ADMIN)
     * @return RoleParseResult containing account key and role type
     */
    private RoleParseResult parseRole(String fullRoleName) {
        if (!fullRoleName.startsWith(ROLE_PREFIX)) {
            throw new IllegalArgumentException("Role does not start with expected prefix: " + ROLE_PREFIX);
        }
        
        // Remove prefix: DPD_SIGNATURE_STUDIO_
        String remaining = fullRoleName.substring(ROLE_PREFIX.length());
        
        if (remaining.isEmpty()) {
            throw new IllegalArgumentException("Role has no account and role information: " + fullRoleName);
        }
        
        // Split by underscore
        String[] parts = remaining.split("_");
        
        if (parts.length < 2) {
            throw new IllegalArgumentException("Role format invalid: " + fullRoleName + 
                " (expected: DPD_SIGNATURE_STUDIO_{ACCOUNT_KEY}_{ROLE_TYPE})");
        }
        
        // Find where the role type starts
        // Role type can be: ADMIN, SENDER, AUDIT, READ_ONLY, etc.
        // We need to find the last underscore(s) that separate account from role
        
        // Common role types: ADMIN, SENDER, AUDIT, READ_ONLY, READ_WRITE
        // Strategy: The role type is typically the last 1-2 parts
        // If the last part is "ONLY" or "WRITE", then role type is last 2 parts (e.g., READ_ONLY)
        // Otherwise, role type is last part (e.g., ADMIN, SENDER, AUDIT)
        
        String roleType;
        String accountKey;
        
        if (parts.length >= 2 && (parts[parts.length - 1].equals("ONLY") || 
                                   parts[parts.length - 1].equals("WRITE"))) {
            // Role type is compound (e.g., READ_ONLY, READ_WRITE)
            roleType = parts[parts.length - 2] + "_" + parts[parts.length - 1];
            // Account key is everything before the last 2 parts
            accountKey = String.join("_", java.util.Arrays.copyOf(parts, parts.length - 2));
        } else {
            // Role type is single word (e.g., ADMIN, SENDER, AUDIT)
            roleType = parts[parts.length - 1];
            // Account key is everything before the last part
            accountKey = String.join("_", java.util.Arrays.copyOf(parts, parts.length - 1));
        }
        
        if (accountKey.isEmpty()) {
            throw new IllegalArgumentException("Could not extract account key from role: " + fullRoleName);
        }
        
        return new RoleParseResult(accountKey, roleType);
    }
    
    /**
     * Helper class to hold parsed role information
     */
    private static class RoleParseResult {
        private final String accountKey;
        private final String roleType;
        
        RoleParseResult(String accountKey, String roleType) {
            this.accountKey = accountKey;
            this.roleType = roleType;
        }
        
        String getAccountKey() {
            return accountKey;
        }
        
        String getRoleType() {
            return roleType;
        }
    }
}

