package com.wellsfargo.signaturestudio.controller;

import com.wellsfargo.signaturestudio.dto.LoginRequestDTO;
import com.wellsfargo.signaturestudio.dto.SessionDTO;
import com.wellsfargo.signaturestudio.service.AuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    private final AuthenticationService authenticationService;
    
    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }
    
    /**
     * Login endpoint - creates new session with session fixation protection.
     */
    @PostMapping("/login")
    public ResponseEntity<SessionDTO> login(@Valid @RequestBody LoginRequestDTO loginRequest, 
                                            HttpServletRequest request) {
        SessionDTO sessionDTO = authenticationService.login(loginRequest, request);
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
    public ResponseEntity<SessionDTO> getSession(HttpSession session) {
        SessionDTO sessionDTO = authenticationService.getSession(session);
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
        boolean valid = authenticationService.isSessionValid(request);
        return ResponseEntity.ok(Map.of(
            "valid", valid,
            "timestamp", System.currentTimeMillis()
        ));
    }
}


