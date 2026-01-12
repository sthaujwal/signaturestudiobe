package com.wellsfargo.signaturestudio.controller;

import com.wellsfargo.signaturestudio.domain.*;
import com.wellsfargo.signaturestudio.constants.SessionConstants;
import com.wellsfargo.signaturestudio.service.AccountService;
import com.wellsfargo.signaturestudio.service.AuthenticationService;
import com.wellsfargo.signaturestudio.service.AuthenticationTokenService;
import com.wellsfargo.signaturestudio.service.CsrfTokenService;
import com.wellsfargo.signaturestudio.service.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    private final AuthenticationService authenticationService;
    private final SessionService sessionService;
    private final AccountService accountService;
    private final CsrfTokenService csrfTokenService;
    private final AuthenticationTokenService authenticationTokenService;

    public AuthController(AuthenticationService authenticationService,
                         SessionService sessionService,
                         AccountService accountService,
                         CsrfTokenService csrfTokenService,
                         AuthenticationTokenService authenticationTokenService) {
        this.authenticationService = authenticationService;
        this.sessionService = sessionService;
        this.accountService = accountService;
        this.csrfTokenService = csrfTokenService;
        this.authenticationTokenService = authenticationTokenService;
    }
    
    /**
     * Login endpoint - creates new session with session fixation protection.
     */
    @PostMapping("/login")
    public ResponseEntity<Session> login(@Valid @RequestBody LoginRequest loginRequest, 
                                            HttpServletRequest request) {
        Session sessionDTO = authenticationService.login(loginRequest, request);
        return ResponseEntity.ok(sessionDTO);
    }
    
    /**
     * Logout endpoint - invalidates session securely.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        authenticationService.logout(request);
        return ResponseEntity.ok().build();
    }
    
    /**
     * Get current session information.
     */
    @GetMapping("/session")
    public ResponseEntity<Session> getSession(HttpSession session) {
        Session sessionDTO = sessionService.getSessionInfo(session);
        if (sessionDTO == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(sessionDTO);
    }
    
    /**
     * Get CSRF token for frontend.
     * Returns the token in response body for SPA applications.
     */
    @GetMapping("/csrf-token")
    public ResponseEntity<Map<String, String>> getCsrfToken(HttpServletRequest request) {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            return ResponseEntity.ok(Map.of(
                "token", csrfToken.getToken(),
                "headerName", csrfToken.getHeaderName(),
                "parameterName", csrfToken.getParameterName()
            ));
        }
        return ResponseEntity.ok(Map.of());
    }
    
    /**
     * Validate if current session is valid.
     */
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateSession(HttpServletRequest request) {
        boolean valid = sessionService.isSessionValid(request);
        return ResponseEntity.ok(Map.of(
            "valid", valid,
            "timestamp", System.currentTimeMillis()
        ));
    }
    
    /**
     * Token exchange endpoint - exchanges authorization code for access token.
     *
     * Flow:
     * 1. Frontend receives authorization code from redirect URL
     * 2. Frontend POSTs code to this endpoint
     * 3. Backend validates code (one-time use, checks expiration)
     * 4. Backend generates long-lived access token
     * 5. Frontend uses access token for all subsequent API calls
     *
     * Security:
     * - Authorization code is single-use (prevents replay attacks)
     * - Authorization code expires in 60 seconds (limits exposure window)
     * - Access token is longer (64 bytes vs 43 bytes)
     * - Access token auto-extends on each use (sliding expiration)
     */
    @PostMapping("/exchange")
    public ResponseEntity<TokenResponse> exchangeCodeForToken(
            @Valid @RequestBody TokenExchangeRequest request) {

        // 1. Validate and consume authorization code (ONE-TIME USE)
        Optional<String> sessionIdOpt = authenticationTokenService.validateAndConsumeAuthorizationCode(
            request.getCode()
        );

        if (sessionIdOpt.isEmpty()) {
            logger.warn("Token exchange failed: Invalid, expired, or already used authorization code");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new TokenResponse(null, "Invalid or expired authorization code"));
        }

        String sessionId = sessionIdOpt.get();

        // 2. Generate LONG-LIVED access token (type: ACCESS_TOKEN)
        String accessToken = authenticationTokenService.generateAccessToken(sessionId);

        logger.info("Token exchange successful for session: {}", sessionId);

        return ResponseEntity.ok(new TokenResponse(accessToken, null));
    }

    /**
     * Authenticated endpoint - receives AuthUser object from Ping IdP with roles.
     * Parses roles to extract accounts and user's role in each account.
     * Stores accounts with roles in session for quick access throughout the session.
     *
     * NOTE: This endpoint would be called after Ping IdP callback to set up session.
     * After this, generateAuthorizationCode() should be called before redirect.
     *
     * Role format: DPD_SIGNATURE_STUDIO_{ACCOUNT_KEY}_{ROLE}
     * Examples:
     * - DPD_SIGNATURE_STUDIO_TEST_ADMIN -> Account: TEST, Role: ADMIN
     * - DPD_SIGNATURE_STUDIO_TEST2_SENDER -> Account: TEST2, Role: SENDER
     * - DPD_SIGNATURE_STUDIO_TEST_READ_ONLY -> Account: TEST, Role: READ_ONLY
     * - DPD_SIGNATURE_STUDIO_ORG_ADMIN -> Organization-level admin (no account key)
     *
     * @param authUser The authenticated user with roles from Ping IdP
     * @param session The HTTP session
     * @return List of accounts with user's role in each account
     */
    @PostMapping("/authenticated")
    public ResponseEntity<List<AccountWithRole>> authenticated(
            @Valid @RequestBody AuthUser authUser,
            HttpServletRequest request,
            HttpServletResponse response,
            HttpSession session) {

        // CRITICAL: Check for ORG_ADMIN role FIRST, before parsing account-level roles
        boolean isOrgAdmin = authUser.getRoles().stream()
            .anyMatch(role -> "DPD_SIGNATURE_STUDIO_ORG_ADMIN".equals(role));

        if (isOrgAdmin) {
            // Set ORG_ADMIN session attributes
            session.setAttribute(SessionConstants.IS_ORG_ADMIN, true);
            session.setAttribute(SessionConstants.ROLE, "ORG_ADMIN");
            session.setAttribute(SessionConstants.AUTHENTICATED, true);
            session.setAttribute(SessionConstants.USER_ID, authUser.getUserId());
            session.setAttribute(SessionConstants.USERNAME, authUser.getUsername());

            // Ensure CSRF token is generated
            csrfTokenService.getOrGenerateToken(request, response);

            logger.info("ORG_ADMIN role detected for user: {} | Session configured for organization-level access",
                authUser.getUserId());

            // ORG_ADMIN gets empty account list (they can access all accounts via org-admin APIs)
            session.setAttribute(SessionConstants.ACCOUNTS_WITH_ROLES, new ArrayList<AccountWithRole>());

            return ResponseEntity.ok(new ArrayList<>());
        }

        // Regular user flow - parse account-level roles
        session.setAttribute(SessionConstants.IS_ORG_ADMIN, false);

        List<AccountWithRole> accountsWithRoles = accountService.getAccountsWithRoles(authUser);

        // Store in session for quick access throughout the session
        session.setAttribute(SessionConstants.ACCOUNTS_WITH_ROLES, accountsWithRoles);

        // Set default account if not already set
        if (!accountsWithRoles.isEmpty() && session.getAttribute(SessionConstants.ACCOUNT_ID) == null) {
            AccountWithRole defaultAccount = accountsWithRoles.get(0);
            session.setAttribute(SessionConstants.ACCOUNT_ID, defaultAccount.getAccountId());
            logger.debug("Set default account for user: {} -> {}",
                authUser.getUserId(), defaultAccount.getAccountId());
        }

        // Ensure CSRF token is generated and available
        // This ensures the token is created when session attributes are first set
        csrfTokenService.getOrGenerateToken(request, response);

        logger.info("Stored {} accounts with roles in session for user: {} | CSRF token generated",
            accountsWithRoles.size(), authUser.getUserId());

        return ResponseEntity.ok(accountsWithRoles);
    }
}


