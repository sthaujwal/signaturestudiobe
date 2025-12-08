package com.wellsfargo.signaturestudio.service;

import com.wellsfargo.signaturestudio.dto.LoginRequestDTO;
import com.wellsfargo.signaturestudio.dto.SessionDTO;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthenticationService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);
    
    // Mock authentication - will be replaced with real Ping integration
    public SessionDTO login(LoginRequestDTO loginRequest, HttpSession session) {
        logger.info("Mock login attempt for user: {}", loginRequest.getUsername());
        
        // Mock authentication - accept any username/password
        // In production, this will call Ping Identity
        
        SessionDTO sessionDTO = new SessionDTO();
        sessionDTO.setSessionId(session.getId());
        sessionDTO.setUsername(loginRequest.getUsername());
        sessionDTO.setEmail(loginRequest.getUsername() + "@wellsfargo.com");
        sessionDTO.setAccountId("ACCT_" + loginRequest.getUsername().hashCode());
        sessionDTO.setCreatedAt(LocalDateTime.now());
        sessionDTO.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        
        // Store in session
        session.setAttribute("username", loginRequest.getUsername());
        session.setAttribute("email", sessionDTO.getEmail());
        session.setAttribute("accountId", sessionDTO.getAccountId());
        session.setMaxInactiveInterval(30 * 60); // 30 minutes
        
        logger.info("User logged in successfully: {}", loginRequest.getUsername());
        return sessionDTO;
    }
    
    public void logout(HttpSession session) {
        String username = (String) session.getAttribute("username");
        logger.info("Logging out user: {}", username);
        session.invalidate();
    }
    
    public SessionDTO getSession(HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return null;
        }
        
        SessionDTO sessionDTO = new SessionDTO();
        sessionDTO.setSessionId(session.getId());
        sessionDTO.setUsername(username);
        sessionDTO.setEmail((String) session.getAttribute("email"));
        sessionDTO.setAccountId((String) session.getAttribute("accountId"));
        // Note: CreatedAt and ExpiresAt would need to be stored in session or retrieved from Spring Session
        
        return sessionDTO;
    }
}


