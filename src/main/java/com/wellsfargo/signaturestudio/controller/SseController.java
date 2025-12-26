package com.wellsfargo.signaturestudio.controller;

import com.wellsfargo.signaturestudio.constants.SessionConstants;
import com.wellsfargo.signaturestudio.dto.TransactionEventDTO;
import com.wellsfargo.signaturestudio.service.SseService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * REST controller for Server-Sent Events (SSE).
 * Provides real-time updates for transactions and other events.
 */
@RestController
@RequestMapping("/api/sse")
public class SseController {
    
    private final SseService sseService;
    
    public SseController(SseService sseService) {
        this.sseService = sseService;
    }
    
    /**
     * Subscribe to general events stream.
     * Returns SSE stream with transaction updates and other events.
     * 
     * @param session HTTP session
     * @return SSE emitter for real-time updates
     */
    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvents(HttpSession session) {
        String userId = (String) session.getAttribute(SessionConstants.USERNAME);
        if (userId == null) {
            throw new IllegalStateException("User not authenticated");
        }
        
        return sseService.createEventStream(userId);
    }
    
    /**
     * Subscribe to transaction-specific events.
     * Returns SSE stream with updates for a specific transaction.
     * 
     * @param transactionId The transaction ID to monitor
     * @param session HTTP session
     * @return SSE emitter for real-time updates
     */
    @GetMapping(value = "/transactions/{transactionId}/updates", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamTransactionUpdates(
            @PathVariable String transactionId,
            HttpSession session) {
        String userId = (String) session.getAttribute(SessionConstants.USERNAME);
        if (userId == null) {
            throw new IllegalStateException("User not authenticated");
        }
        
        return sseService.createTransactionStream(userId, transactionId);
    }
}

