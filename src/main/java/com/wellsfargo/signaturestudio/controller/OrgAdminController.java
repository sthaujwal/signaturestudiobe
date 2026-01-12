package com.wellsfargo.signaturestudio.controller;

import com.wellsfargo.signaturestudio.annotation.RequireOrgAdmin;
import com.wellsfargo.signaturestudio.constants.SessionConstants;
import com.wellsfargo.signaturestudio.domain.*;
import com.wellsfargo.signaturestudio.model.AccountEntity;
import com.wellsfargo.signaturestudio.service.AccountService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Organization Admin Controller
 *
 * Provides REST API endpoints for organization-level administrators
 * to manage accounts across the entire platform.
 *
 * All endpoints require ORG_ADMIN privileges.
 *
 * Capabilities:
 * - Create new accounts
 * - View all accounts
 * - Update any account's settings
 * - View account details
 */
@RestController
@RequestMapping("/api/org-admin")
public class OrgAdminController {

    private static final Logger logger = LoggerFactory.getLogger(OrgAdminController.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("SECURITY_AUDIT");

    private final AccountService accountService;

    public OrgAdminController(AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * Create a new account.
     *
     * Only ORG_ADMIN can create accounts. This endpoint:
     * - Validates the account key (format, uniqueness, reserved words)
     * - Creates the account with generated IDs
     * - Creates default account roles (ADMIN, SENDER, AUDIT, READ_ONLY)
     * - Logs the role names that must be created in Auth0/Ping IdP
     *
     * POST /api/org-admin/accounts
     *
     * Request Body:
     * {
     *   "accountName": "Test Account",
     *   "accountKey": "TEST",
     *   "accountSettings": { ... } // optional
     * }
     *
     * @param request Account creation request
     * @param session HTTP session to get current user
     * @return Created account entity
     */
    @PostMapping("/accounts")
    @RequireOrgAdmin(operation = "Create new account")
    public ResponseEntity<AccountEntity> createAccount(
            @Valid @RequestBody CreateAccountRequest request,
            HttpSession session) {

        String username = (String) session.getAttribute(SessionConstants.USERNAME);

        logger.info("ORG_ADMIN creating new account: {} by user: {}", request.getAccountKey(), username);

        AccountEntity createdAccount = accountService.createAccount(request, username);

        auditLogger.info("ORG_ADMIN_CREATE_ACCOUNT | User: {} | AccountId: {} | AccountKey: {}",
            username, createdAccount.getAccountId(), createdAccount.getAccountKey());

        return ResponseEntity.status(HttpStatus.CREATED).body(createdAccount);
    }

    /**
     * Get all accounts.
     *
     * Returns a list of all accounts in the system with summary information.
     * Supports pagination and optional search filtering.
     *
     * GET /api/org-admin/accounts?page=0&size=50&search=test
     *
     * @param page Page number (0-indexed)
     * @param size Page size (default: 50)
     * @param search Optional search term to filter accounts by name or key
     * @param session HTTP session to get current user
     * @return Paginated list of account summaries
     */
    @GetMapping("/accounts")
    @RequireOrgAdmin(operation = "View all accounts")
    public ResponseEntity<AccountListResponse> getAllAccounts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String search,
            HttpSession session) {

        String username = (String) session.getAttribute(SessionConstants.USERNAME);

        logger.info("ORG_ADMIN listing all accounts: user={}, page={}, size={}, search={}",
            username, page, size, search);

        // Get all accounts (pagination can be added later)
        List<AccountSummary> accounts = accountService.getAllAccounts();

        // Apply search filter if provided
        if (search != null && !search.trim().isEmpty()) {
            String searchLower = search.toLowerCase();
            accounts = accounts.stream()
                .filter(account ->
                    account.getAccountName().toLowerCase().contains(searchLower) ||
                    account.getAccountKey().toLowerCase().contains(searchLower))
                .toList();
        }

        // Create response (pagination can be enhanced later)
        AccountListResponse response = new AccountListResponse(
            accounts,
            accounts.size(),
            page,
            size
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Get account details by account ID.
     *
     * Returns full account information including settings and metadata.
     *
     * GET /api/org-admin/accounts/{accountId}
     *
     * @param accountId The account ID to retrieve
     * @param session HTTP session to get current user
     * @return Account entity with full details
     */
    @GetMapping("/accounts/{accountId}")
    @RequireOrgAdmin(operation = "View account details")
    public ResponseEntity<AccountEntity> getAccountDetails(
            @PathVariable String accountId,
            HttpSession session) {
        String username = (String) session.getAttribute(SessionConstants.USERNAME);

        logger.info("ORG_ADMIN getting account details: accountId={}, user={}",
            accountId, username);

        AccountEntity account = accountService.getAccountDetails(accountId);

        return ResponseEntity.ok(account);
    }

    /**
     * Update an existing account.
     *
     * Allows ORG_ADMIN to update account name and settings.
     * Account key cannot be changed once created.
     *
     * PUT /api/org-admin/accounts/{accountId}
     *
     * Request Body:
     * {
     *   "accountName": "Updated Account Name",
     *   "accountSettings": { ... } // optional
     * }
     *
     * @param accountId The account ID to update
     * @param request Update request with new values
     * @param session HTTP session to get current user
     * @return Updated account entity
     */
    @PutMapping("/accounts/{accountId}")
    @RequireOrgAdmin(operation = "Update account")
    public ResponseEntity<AccountEntity> updateAccount(
            @PathVariable String accountId,
            @Valid @RequestBody UpdateAccountRequest request,
            HttpSession session) {

        String username = (String) session.getAttribute(SessionConstants.USERNAME);

        logger.info("ORG_ADMIN updating account: accountId={}, user={}",
            accountId, username);

        AccountEntity updatedAccount = accountService.updateAccount(accountId, request, username);

        auditLogger.info("ORG_ADMIN_UPDATE_ACCOUNT | User: {} | AccountId: {} | AccountKey: {}",
            username, updatedAccount.getAccountId(), updatedAccount.getAccountKey());

        return ResponseEntity.ok(updatedAccount);
    }

    /**
     * Get account role names for Auth0/Ping IdP setup.
     *
     * Returns the list of role names that must be created in the identity provider
     * for the specified account.
     *
     * GET /api/org-admin/accounts/{accountId}/role-names
     *
     * Response:
     * {
     *   "accountId": "123",
     *   "accountKey": "TEST",
     *   "roleNames": [
     *     "DPD_SIGNATURE_STUDIO_TEST_ADMIN",
     *     "DPD_SIGNATURE_STUDIO_TEST_SENDER",
     *     "DPD_SIGNATURE_STUDIO_TEST_AUDIT",
     *     "DPD_SIGNATURE_STUDIO_TEST_READ_ONLY"
     *   ]
     * }
     *
     * @param accountId The account ID
     * @return Account role names for Auth0 setup
     */
    @GetMapping("/accounts/{accountId}/role-names")
    @RequireOrgAdmin(operation = "View account role names for Auth0 setup")
    public ResponseEntity<Object> getAccountRoleNames(@PathVariable String accountId, HttpSession session) {
        String username = (String) session.getAttribute(SessionConstants.USERNAME);

        logger.info("ORG_ADMIN getting role names for Auth0 setup: accountId={}, user={}",
            accountId, username);

        AccountEntity account = accountService.getAccountDetails(accountId);

        // Build response with role names
        List<String> roleNames = account.getRoles().stream()
            .map(role -> role.getRoleName())
            .toList();

        var response = new java.util.HashMap<String, Object>();
        response.put("accountId", account.getAccountId());
        response.put("accountKey", account.getAccountKey());
        response.put("accountName", account.getAccountName());
        response.put("roleNames", roleNames);

        return ResponseEntity.ok(response);
    }
}
