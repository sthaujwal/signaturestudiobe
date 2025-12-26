package com.wellsfargo.signaturestudio.controller;

import com.wellsfargo.signaturestudio.domain.AccountWithRole;
import com.wellsfargo.signaturestudio.domain.AuthUser;
import com.wellsfargo.signaturestudio.domain.LoginRequest;
import com.wellsfargo.signaturestudio.domain.Session;
import com.wellsfargo.signaturestudio.service.AccountService;
import com.wellsfargo.signaturestudio.service.AuthenticationService;
import com.wellsfargo.signaturestudio.service.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    private final AuthenticationService authenticationService;
    private final SessionService sessionService;
    private final AccountService accountService;
    
    public AuthController(AuthenticationService authenticationService, 
                         SessionService sessionService,
                         AccountService accountService) {
        this.authenticationService = authenticationService;
        this.sessionService = sessionService;
        this.accountService = accountService;
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
     * Authenticated endpoint - receives AuthUser object from Auth0 with roles.
     * Parses roles to extract accounts and user's role in each account.
     * 
     * Role format: DPD_SIGNATURE_STUDIO_{ACCOUNT_KEY}_{ROLE}
     * Examples:
     * - DPD_SIGNATURE_STUDIO_TEST_ADMIN -> Account: TEST, Role: ADMIN
     * - DPD_SIGNATURE_STUDIO_TEST2_SENDER -> Account: TEST2, Role: SENDER
     * - DPD_SIGNATURE_STUDIO_TEST_READ_ONLY -> Account: TEST, Role: READ_ONLY
     * 
     * @param authUser The authenticated user with roles from Auth0
     * @return List of accounts with user's role in each account
     */
    @PostMapping("/authenticated")
    public ResponseEntity<List<AccountWithRole>> authenticated(@Valid @RequestBody AuthUser authUser) {
        List<AccountWithRole> accountsWithRoles = accountService.getAccountsWithRoles(authUser);
        return ResponseEntity.ok(accountsWithRoles);
    }
}


