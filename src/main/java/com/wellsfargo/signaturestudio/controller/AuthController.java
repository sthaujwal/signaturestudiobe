package com.wellsfargo.signaturestudio.controller;

import com.wellsfargo.signaturestudio.dto.LoginRequestDTO;
import com.wellsfargo.signaturestudio.dto.SessionDTO;
import com.wellsfargo.signaturestudio.service.AuthenticationService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    private final AuthenticationService authenticationService;
    
    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }
    
    @PostMapping("/login")
    public ResponseEntity<SessionDTO> login(@Valid @RequestBody LoginRequestDTO loginRequest, HttpSession session) {
        SessionDTO sessionDTO = authenticationService.login(loginRequest, session);
        return ResponseEntity.ok(sessionDTO);
    }
    
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpSession session) {
        authenticationService.logout(session);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/session")
    public ResponseEntity<SessionDTO> getSession(HttpSession session) {
        SessionDTO sessionDTO = authenticationService.getSession(session);
        if (sessionDTO == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(sessionDTO);
    }
    
    @GetMapping("/csrf-token")
    public ResponseEntity<Void> getCsrfToken() {
        // CSRF token is automatically handled by Spring Security
        return ResponseEntity.ok().build();
    }
}


