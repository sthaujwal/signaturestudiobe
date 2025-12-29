package com.wellsfargo.signaturestudio.service;

import com.wellsfargo.signaturestudio.domain.Account;
import com.wellsfargo.signaturestudio.domain.AccountWithRole;
import com.wellsfargo.signaturestudio.domain.AuthUser;
import com.wellsfargo.signaturestudio.exception.ErrorCode;
import com.wellsfargo.signaturestudio.exception.ServiceException;
import com.wellsfargo.signaturestudio.model.AccountEntity;
import com.wellsfargo.signaturestudio.model.AccountRole;
import com.wellsfargo.signaturestudio.repository.AccountRepository;
import com.wellsfargo.signaturestudio.repository.AccountRoleRepository;
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
    private final AccountRoleRepository accountRoleRepository;
    
    public AccountService(AccountRepository accountRepository, AccountRoleRepository accountRoleRepository) {
        this.accountRepository = accountRepository;
        this.accountRoleRepository = accountRoleRepository;
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
        
        if (hasNoRoles(authUser)) {
            logger.warn("No roles found for user: {}", authUser.getUserId());
            return new ArrayList<>();
        }
        
        List<String> signatureStudioRoles = filterSignatureStudioRoles(authUser.getRoles());
        logger.debug("Found {} roles starting with {} for user: {}", 
            signatureStudioRoles.size(), ROLE_PREFIX, authUser.getUserId());
        
        List<AccountWithRole> accountsWithRoles = signatureStudioRoles.stream()
            .map(this::processRole)
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toList());
        
        logger.info("Found {} accounts with roles for user: {}", 
            accountsWithRoles.size(), authUser.getUserId());
        
        return accountsWithRoles;
    }
    
    /**
     * Check if user has no roles.
     */
    private boolean hasNoRoles(AuthUser authUser) {
        return authUser.getRoles() == null || authUser.getRoles().isEmpty();
    }
    
    /**
     * Filter roles that start with DPD_SIGNATURE_STUDIO_ prefix.
     */
    private List<String> filterSignatureStudioRoles(List<String> roles) {
        return roles.stream()
            .filter(role -> role != null && role.startsWith(ROLE_PREFIX))
            .collect(Collectors.toList());
    }
    
    /**
     * Process a single role and convert it to AccountWithRole.
     * Returns null if role cannot be processed.
     */
    private AccountWithRole processRole(String fullRoleName) {
        try {
            return accountRoleRepository.findByRoleName(fullRoleName)
                .map(this::createAccountWithRole)
                .orElseGet(() -> {
                    logger.warn("AccountRole not found for role name: {}", fullRoleName);
                    return null;
                });
        } catch (Exception e) {
            logger.error("Error processing role: {}", fullRoleName, e);
            return null;
        }
    }
    
    /**
     * Create AccountWithRole from AccountRole entity.
     */
    private AccountWithRole createAccountWithRole(AccountRole accountRole) {
        AccountEntity accountEntity = accountRole.getAccount();
        if (accountEntity == null) {
            logger.warn("Account not found for role: {}", accountRole.getRoleName());
            return null;
        }
        
        String roleType = extractRoleType(accountRole.getRoleName());
        AccountWithRole accountWithRole = buildAccountWithRole(accountEntity, accountRole.getRoleName(), roleType);
        
        logger.debug("Found role: {} -> Account: {} ({}), Role: {}", 
            accountRole.getRoleName(), accountEntity.getAccountKey(), accountEntity.getAccountId(), roleType);
        
        return accountWithRole;
    }
    
    /**
     * Build AccountWithRole domain object from AccountEntity and role information.
     */
    private AccountWithRole buildAccountWithRole(AccountEntity accountEntity, String fullRoleName, String roleType) {
        AccountWithRole accountWithRole = new AccountWithRole();
        accountWithRole.setAccountId(accountEntity.getAccountId());
        accountWithRole.setAccountName(accountEntity.getAccountName());
        accountWithRole.setAccountKey(accountEntity.getAccountKey());
        accountWithRole.setRole(roleType);
        accountWithRole.setFullRoleName(fullRoleName);
        return accountWithRole;
    }
    
    /**
     * Extract role type from full role name by checking if it ends with known role types.
     * Role format: DPD_SIGNATURE_STUDIO_{ACCOUNT_KEY}_{ROLE_TYPE}
     * Examples:
     * - DPD_SIGNATURE_STUDIO_TEST_ADMIN -> ADMIN
     * - DPD_SIGNATURE_STUDIO_TEST2_SENDER -> SENDER
     * - DPD_SIGNATURE_STUDIO_TEST_READ_ONLY -> READ_ONLY
     * 
     * @param fullRoleName Full role name (e.g., DPD_SIGNATURE_STUDIO_TEST_ADMIN)
     * @return Role type (ADMIN, SENDER, READ_ONLY, etc.)
     */
    private String extractRoleType(String fullRoleName) {
        if (!fullRoleName.startsWith(ROLE_PREFIX)) {
            throw new IllegalArgumentException("Role does not start with expected prefix: " + ROLE_PREFIX);
        }
        
        String upperRoleName = fullRoleName.toUpperCase();
        
        // Check for compound role types first (longer matches first)
        if (upperRoleName.endsWith("_READ_ONLY")) {
            return "READ_ONLY";
        }
        
        // Check for single-word role types (case-insensitive)
        if (upperRoleName.endsWith("_ADMIN")) {
            return "ADMIN";
        }
        if (upperRoleName.endsWith("_SENDER")) {
            return "SENDER";
        }
        if (upperRoleName.endsWith("_AUDIT")) {
            return "AUDIT";
        }
        
        // If no known role type found, extract the last part after the last underscore
        int lastUnderscore = fullRoleName.lastIndexOf('_');
        if (lastUnderscore > 0 && lastUnderscore < fullRoleName.length() - 1) {
            return fullRoleName.substring(lastUnderscore + 1).toUpperCase();
        }
        
        throw new IllegalArgumentException("Could not extract role type from: " + fullRoleName);
    }
}

