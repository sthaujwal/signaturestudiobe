package com.wellsfargo.signaturestudio.service;

import com.wellsfargo.signaturestudio.domain.*;
import com.wellsfargo.signaturestudio.exception.DuplicateAccountKeyException;
import com.wellsfargo.signaturestudio.exception.ErrorCode;
import com.wellsfargo.signaturestudio.exception.InvalidAccountKeyException;
import com.wellsfargo.signaturestudio.exception.ServiceException;
import com.wellsfargo.signaturestudio.model.AccountEntity;
import com.wellsfargo.signaturestudio.model.AccountRole;
import com.wellsfargo.signaturestudio.repository.AccountRepository;
import com.wellsfargo.signaturestudio.repository.AccountRoleRepository;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
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
    private final SessionService sessionService;
    
    public AccountService(AccountRepository accountRepository, 
                         AccountRoleRepository accountRoleRepository,
                         SessionService sessionService) {
        this.accountRepository = accountRepository;
        this.accountRoleRepository = accountRoleRepository;
        this.sessionService = sessionService;
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
     * Excludes ORG_ADMIN role as it's organization-level, not account-level.
     */
    private List<String> filterSignatureStudioRoles(List<String> roles) {
        return roles.stream()
            .filter(role -> role != null && role.startsWith(ROLE_PREFIX))
            .filter(role -> !role.equals("DPD_SIGNATURE_STUDIO_ORG_ADMIN"))  // Exclude ORG_ADMIN
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
    
    /**
     * Check if user has a specific role for an account.
     * Uses session data for fast authorization checks.
     * 
     * @param accountId The account ID to check
     * @param requiredRole The required role (ADMIN, SENDER, AUDIT, etc.)
     * @param session The HTTP session
     * @return true if user has the required role for the account
     */
    public boolean hasRoleForAccount(String accountId, String requiredRole, HttpSession session) {
        if (accountId == null || requiredRole == null || session == null) {
            return false;
        }
        
        List<AccountWithRole> accounts = sessionService.getAccountsWithRoles(session);
        return accounts.stream()
            .anyMatch(account -> accountId.equals(account.getAccountId()) 
                && requiredRole.equalsIgnoreCase(account.getRole()));
    }
    
    /**
     * Get user's role for a specific account from session.
     *
     * @param accountId The account ID
     * @param session The HTTP session
     * @return Role name (ADMIN, SENDER, etc.) or null if not found
     */
    public String getRoleForAccount(String accountId, HttpSession session) {
        if (accountId == null || session == null) {
            return null;
        }

        return sessionService.getAccountsWithRoles(session).stream()
            .filter(account -> accountId.equals(account.getAccountId()))
            .map(AccountWithRole::getRole)
            .findFirst()
            .orElse(null);
    }

    // ==================== ORG_ADMIN Account Management Methods ====================

    /**
     * Create a new account (ORG_ADMIN only).
     *
     * Creates an account with:
     * - Generated UUIDs for id and accountId
     * - Uppercase account key
     * - Default account roles (ADMIN, SENDER, AUDIT, READ_ONLY)
     *
     * @param request Account creation request with name, key, and optional settings
     * @param createdBy Username of the user creating the account
     * @return Created account entity
     * @throws InvalidAccountKeyException if account key is invalid
     * @throws DuplicateAccountKeyException if account key already exists
     */
    @Transactional
    public AccountEntity createAccount(CreateAccountRequest request, String createdBy) {
        logger.info("Creating new account: {} by user: {}", request.getAccountKey(), createdBy);

        // Validate account key
        validateAccountKey(request.getAccountKey());

        // Create account entity
        AccountEntity account = new AccountEntity();
        account.setId(UUID.randomUUID().toString());
        account.setAccountId(UUID.randomUUID().toString());
        account.setAccountName(request.getAccountName());
        account.setAccountKey(request.getAccountKey().toUpperCase());
        account.setCreatedAt(Instant.now());
        account.setModifiedAt(Instant.now());

        // Note: AccountSettings would be stored separately if needed
        // For now, we're focusing on basic account creation

        // Create default account roles
        createDefaultAccountRoles(account);

        // Save account (cascade will save roles)
        AccountEntity savedAccount = accountRepository.save(account);

        auditLogger.info("ACCOUNT_CREATED | AccountId: {} | AccountKey: {} | CreatedBy: {}",
            savedAccount.getAccountId(), savedAccount.getAccountKey(), createdBy);

        // Log the role names that need to be created in Auth0/Ping IdP
        logRequiredAuth0Roles(savedAccount);

        return savedAccount;
    }

    /**
     * Update an existing account (ORG_ADMIN only).
     *
     * @param accountId The account ID to update
     * @param request Update request with name and/or settings
     * @param modifiedBy Username of the user updating the account
     * @return Updated account entity
     * @throws ServiceException if account not found
     */
    @Transactional
    public AccountEntity updateAccount(String accountId, UpdateAccountRequest request, String modifiedBy) {
        logger.info("Updating account: {} by user: {}", accountId, modifiedBy);

        // Load existing account
        AccountEntity account = accountRepository.findByAccountId(accountId)
            .orElseThrow(() -> new ServiceException(ErrorCode.RESOURCE_NOT_FOUND,
                "Account not found: " + accountId));

        // Update name if provided
        if (request.getAccountName() != null && !request.getAccountName().trim().isEmpty()) {
            account.setAccountName(request.getAccountName());
        }

        // Note: AccountSettings would be updated separately if needed
        // For now, we're focusing on basic account name updates

        account.setModifiedAt(Instant.now());

        AccountEntity updatedAccount = accountRepository.save(account);

        auditLogger.info("ACCOUNT_UPDATED | AccountId: {} | AccountKey: {} | ModifiedBy: {}",
            updatedAccount.getAccountId(), updatedAccount.getAccountKey(), modifiedBy);

        return updatedAccount;
    }

    /**
     * Get all accounts with summary information (ORG_ADMIN only).
     *
     * @return List of all accounts with summary data
     */
    public List<AccountSummary> getAllAccounts() {
        logger.info("Getting all accounts");

        List<AccountEntity> accounts = accountRepository.findAll();

        return accounts.stream()
            .map(this::convertToAccountSummary)
            .collect(Collectors.toList());
    }

    /**
     * Get detailed account information by account ID (ORG_ADMIN only).
     *
     * @param accountId The account ID
     * @return Account entity
     * @throws ServiceException if account not found
     */
    public AccountEntity getAccountDetails(String accountId) {
        logger.info("Getting account details: {}", accountId);

        return accountRepository.findByAccountId(accountId)
            .orElseThrow(() -> new ServiceException(ErrorCode.RESOURCE_NOT_FOUND,
                "Account not found: " + accountId));
    }

    /**
     * Validate account key format and uniqueness.
     *
     * Rules:
     * - Length: 2-50 characters
     * - Format: Only uppercase letters, numbers, and underscores
     * - Not a reserved word (ADMIN, SENDER, AUDIT, READ_ONLY, ORG_ADMIN, ORG)
     * - Must be unique (case-insensitive)
     *
     * @param accountKey The account key to validate
     * @throws InvalidAccountKeyException if validation fails
     * @throws DuplicateAccountKeyException if key already exists
     */
    private void validateAccountKey(String accountKey) {
        // Length check
        if (accountKey == null || accountKey.length() < 2) {
            throw new InvalidAccountKeyException("Account key must be at least 2 characters", accountKey);
        }
        if (accountKey.length() > 50) {
            throw new InvalidAccountKeyException("Account key must be 50 characters or less", accountKey);
        }

        // Format check (uppercase letters, numbers, underscores only)
        if (!accountKey.matches("^[A-Z0-9_]+$")) {
            throw new InvalidAccountKeyException(
                "Account key must contain only uppercase letters, numbers, and underscores", accountKey);
        }

        // Reserved word check
        List<String> reservedWords = Arrays.asList("ADMIN", "SENDER", "AUDIT", "READ_ONLY", "ORG_ADMIN", "ORG");
        if (reservedWords.contains(accountKey.toUpperCase())) {
            throw new InvalidAccountKeyException("Account key '" + accountKey + "' is reserved", accountKey);
        }

        // Uniqueness check (case-insensitive)
        if (accountRepository.existsByAccountKey(accountKey.toUpperCase())) {
            throw new DuplicateAccountKeyException("Account key '" + accountKey + "' already exists", accountKey);
        }
    }

    /**
     * Create default AccountRole entries for a new account.
     *
     * Creates roles:
     * - DPD_SIGNATURE_STUDIO_{ACCOUNT_KEY}_ADMIN
     * - DPD_SIGNATURE_STUDIO_{ACCOUNT_KEY}_SENDER
     * - DPD_SIGNATURE_STUDIO_{ACCOUNT_KEY}_AUDIT
     * - DPD_SIGNATURE_STUDIO_{ACCOUNT_KEY}_READ_ONLY
     *
     * @param account The account entity
     */
    private void createDefaultAccountRoles(AccountEntity account) {
        String[] roleTypes = {"ADMIN", "SENDER", "AUDIT", "READ_ONLY"};

        for (String roleType : roleTypes) {
            AccountRole role = new AccountRole();
            role.setId(UUID.randomUUID().toString());
            role.setRoleName(ROLE_PREFIX + account.getAccountKey() + "_" + roleType);
            role.setAccount(account);
            account.getRoles().add(role);
        }

        logger.debug("Created {} default roles for account: {}", roleTypes.length, account.getAccountKey());
    }

    /**
     * Log the role names that need to be created in Auth0/Ping IdP.
     *
     * These roles must be manually created in the identity provider after
     * account creation in the application database.
     *
     * @param account The newly created account
     */
    private void logRequiredAuth0Roles(AccountEntity account) {
        logger.info("================================================================================");
        logger.info("ACTION REQUIRED: Create the following roles in Auth0/Ping IdP:");
        logger.info("================================================================================");

        for (AccountRole role : account.getRoles()) {
            logger.info("  - {}", role.getRoleName());
        }

        logger.info("================================================================================");
    }

    /**
     * Convert AccountEntity to AccountSummary DTO.
     *
     * @param account The account entity
     * @return Account summary
     */
    private AccountSummary convertToAccountSummary(AccountEntity account) {
        AccountSummary summary = new AccountSummary();
        summary.setId(account.getId());
        summary.setAccountId(account.getAccountId());
        summary.setAccountName(account.getAccountName());
        summary.setAccountKey(account.getAccountKey());
        summary.setCreatedAt(account.getCreatedAt());
        summary.setModifiedAt(account.getModifiedAt());

        // TODO: Add user count query if needed
        // For now, set to null to indicate it's not calculated
        summary.setUserCount(null);

        return summary;
    }

    /**
     * Check if user has access to a specific account (enhanced for ORG_ADMIN support).
     *
     * @param userId The user ID
     * @param accountId The account ID to check
     * @param session The HTTP session
     * @return true if user has access (either through account-level role or ORG_ADMIN)
     */
    public boolean hasAccountAccess(String userId, String accountId, HttpSession session) {
        if (userId == null || accountId == null || session == null) {
            return false;
        }

        // Check if user is ORG_ADMIN first
        Boolean isOrgAdmin = (Boolean) session.getAttribute(com.wellsfargo.signaturestudio.constants.SessionConstants.IS_ORG_ADMIN);
        if (Boolean.TRUE.equals(isOrgAdmin)) {
            logger.debug("User {} has ORG_ADMIN access to account {}", userId, accountId);
            return true;
        }

        // Regular user - check if account is in their list
        List<AccountWithRole> accounts = sessionService.getAccountsWithRoles(session);
        boolean hasAccess = accounts.stream()
            .anyMatch(account -> accountId.equals(account.getAccountId()));

        if (!hasAccess) {
            auditLogger.warn("ACCOUNT_ACCESS_DENIED | User: {} | AccountId: {} | Reason: Account not accessible",
                userId, accountId);
        }

        return hasAccess;
    }
}

