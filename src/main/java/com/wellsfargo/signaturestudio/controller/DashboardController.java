package com.wellsfargo.signaturestudio.controller;

import com.wellsfargo.signaturestudio.constants.SessionConstants;
import com.wellsfargo.signaturestudio.domain.DashboardStats;
import com.wellsfargo.signaturestudio.service.DashboardService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for dashboard operations.
 * Provides statistics and aggregated data for the dashboard view.
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    
    private final DashboardService dashboardService;
    
    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }
    
    /**
     * Get dashboard statistics for the logged-in user.
     * Includes transaction counts by status, priority, and activity metrics.
     * 
     * Query Parameters:
     * - accountId (optional): Filter statistics by account ID
     * 
     * @param accountId Optional account ID to filter statistics
     * @param session HTTP session
     * @return Dashboard statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<DashboardStats> getDashboardStats(
            @RequestParam(required = false) String accountId,
            HttpSession session) {
        String userId = (String) session.getAttribute(SessionConstants.USERNAME);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        DashboardStats stats = dashboardService.getDashboardStats(userId, accountId);
        return ResponseEntity.ok(stats);
    }
}

