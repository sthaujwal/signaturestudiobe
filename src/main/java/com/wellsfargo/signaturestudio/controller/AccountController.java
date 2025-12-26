package com.wellsfargo.signaturestudio.controller;

import com.wellsfargo.signaturestudio.domain.Account;
import com.wellsfargo.signaturestudio.domain.Session;
import com.wellsfargo.signaturestudio.domain.SwitchAccountRequest;
import com.wellsfargo.signaturestudio.service.AccountService;
import com.wellsfargo.signaturestudio.service.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for account management operations.
 * 
 * Handles:
 * - Getting user's available accounts
 * - Switching between accounts
 * - Getting current account context
 */
@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    
    private final AccountService accountService;
    private final SessionService sessionService;
    
    public AccountController(AccountService accountService, SessionService sessionService) {
        this.accountService = accountService;
        this.sessionService = sessionService;
    }
    
    /**
     * Get all accounts that the current user has access to.
     * 
     * @param session The HTTP session
     * @return List of accounts the user can access
     */
    @GetMapping
    public ResponseEntity<List<Account>> getUserAccounts(HttpSession session) {
        String username = sessionService.getUsername(session);
        if (username == null) {
            return ResponseEntity.status(401).build();
        }
        
        List<Account> accounts = accountService.getUserAccounts(username);
        return ResponseEntity.ok(accounts);
    }
    
    /**
     * Get the current account context from session.
     * 
     * @param session The HTTP session
     * @return Current account information
     */
    @GetMapping("/current")
    public ResponseEntity<Account> getCurrentAccount(HttpSession session) {
        String username = sessionService.getUsername(session);
        if (username == null) {
            return ResponseEntity.status(401).build();
        }
        
        String accountId = sessionService.getAccountId(session);
        if (accountId == null) {
            // Return default account if no account is set in session
            Account defaultAccount = accountService.getDefaultAccount(username);
            return ResponseEntity.ok(defaultAccount);
        }
        
        Account account = accountService.getAccount(username, accountId);
        return ResponseEntity.ok(account);
    }
    
    /**
     * Switch to a different account.
     * 
     * This updates the accountId in the session, which will affect all subsequent
     * queries that filter by account. The session remains valid, only the account
     * context changes.
     * 
     * Security:
     * - Validates user has access to the requested account
     * - Logs account switch for audit purposes
     * - Does not invalidate session (maintains user's login state)
     * 
     * @param requestDTO The account switch request containing the new account ID
     * @param request The HTTP request
     * @return Updated session information with new account context
     */
    @PostMapping("/switch")
    public ResponseEntity<Session> switchAccount(
            @Valid @RequestBody SwitchAccountRequest requestDTO,
            HttpServletRequest request) {
        
        HttpSession session = request.getSession(false);
        if (session == null) {
            return ResponseEntity.status(401).build();
        }
        
        String username = sessionService.getUsername(session);
        if (username == null) {
            return ResponseEntity.status(401).build();
        }
        
        String newAccountId = requestDTO.getAccountId();
        
        // Validate user has access to the requested account
        if (!accountService.hasAccountAccess(username, newAccountId)) {
            return ResponseEntity.status(403).build();
        }
        
        // Switch account in session (logs previous account for audit)
        sessionService.switchAccount(session, newAccountId, username);
        
        // Get updated session info
        Session sessionDTO = sessionService.getSessionInfo(session);
        
        return ResponseEntity.ok(sessionDTO);
    }
    
    /**
     * Validate if user has access to a specific account.
     * 
     * @param accountId The account ID to validate
     * @param session The HTTP session
     * @return Validation result
     */
    @GetMapping("/{accountId}/validate")
    public ResponseEntity<Map<String, Object>> validateAccountAccess(
            @PathVariable String accountId,
            HttpSession session) {
        
        String username = sessionService.getUsername(session);
        if (username == null) {
            return ResponseEntity.status(401).build();
        }
        
        boolean hasAccess = accountService.hasAccountAccess(username, accountId);
        
        return ResponseEntity.ok(Map.of(
            "accountId", accountId,
            "hasAccess", hasAccess
        ));
    }
}

