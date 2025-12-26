package com.wellsfargo.signaturestudio.service;

import com.wellsfargo.signaturestudio.domain.TransactionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Service for managing Server-Sent Events (SSE) streams.
 * Handles real-time updates for transactions and other events.
 */
@Service
public class SseService {
    
    private static final Logger logger = LoggerFactory.getLogger(SseService.class);
    
    private final Map<String, SseEmitter> userStreams = new ConcurrentHashMap<>();
    private final Map<String, SseEmitter> transactionStreams = new ConcurrentHashMap<>();
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(10);
    
    /**
     * Create an event stream for a user.
     * Sends general events and transaction updates.
     * 
     * @param userId The user ID
     * @return SSE emitter
     */
    public SseEmitter createEventStream(String userId) {
        logger.info("Creating SSE stream for user: {}", userId);
        
        SseEmitter emitter = new SseEmitter(300000L); // 5 minutes timeout
        userStreams.put(userId, emitter);
        
        // Handle completion and errors
        emitter.onCompletion(() -> {
            logger.info("SSE stream completed for user: {}", userId);
            userStreams.remove(userId);
        });
        
        emitter.onTimeout(() -> {
            logger.info("SSE stream timeout for user: {}", userId);
            userStreams.remove(userId);
            emitter.complete();
        });
        
        emitter.onError((ex) -> {
            logger.error("SSE stream error for user: {}", userId, ex);
            userStreams.remove(userId);
        });
        
        // Send initial connection event
        try {
            emitter.send(SseEmitter.event()
                .name("connected")
                .data(Map.of("message", "Connected to event stream", "userId", userId)));
        } catch (IOException e) {
            logger.error("Error sending initial SSE event", e);
            emitter.completeWithError(e);
        }
        
        return emitter;
    }
    
    /**
     * Create a transaction-specific event stream.
     * 
     * @param userId The user ID
     * @param transactionId The transaction ID to monitor
     * @return SSE emitter
     */
    public SseEmitter createTransactionStream(String userId, String transactionId) {
        logger.info("Creating SSE stream for transaction: {} by user: {}", transactionId, userId);
        
        String streamKey = userId + ":" + transactionId;
        SseEmitter emitter = new SseEmitter(300000L); // 5 minutes timeout
        transactionStreams.put(streamKey, emitter);
        
        // Handle completion and errors
        emitter.onCompletion(() -> {
            logger.info("SSE stream completed for transaction: {}", transactionId);
            transactionStreams.remove(streamKey);
        });
        
        emitter.onTimeout(() -> {
            logger.info("SSE stream timeout for transaction: {}", transactionId);
            transactionStreams.remove(streamKey);
            emitter.complete();
        });
        
        emitter.onError((ex) -> {
            logger.error("SSE stream error for transaction: {}", transactionId, ex);
            transactionStreams.remove(streamKey);
        });
        
        // Send initial connection event
        try {
            emitter.send(SseEmitter.event()
                .name("connected")
                .data(Map.of("message", "Connected to transaction stream", 
                    "transactionId", transactionId)));
        } catch (IOException e) {
            logger.error("Error sending initial SSE event", e);
            emitter.completeWithError(e);
        }
        
        return emitter;
    }
    
    /**
     * Send a transaction event to all subscribers.
     * 
     * @param event The transaction event
     */
    public void sendTransactionEvent(TransactionEvent event) {
        logger.debug("Sending transaction event: {}", event);
        
        // Send to user-specific streams
        String userId = event.getUserId();
        if (userId != null) {
            SseEmitter userEmitter = userStreams.get(userId);
            if (userEmitter != null) {
                try {
                    userEmitter.send(SseEmitter.event()
                        .name("transaction-update")
                        .data(event));
                } catch (IOException e) {
                    logger.error("Error sending event to user stream", e);
                    userStreams.remove(userId);
                }
            }
        }
        
        // Send to transaction-specific streams
        String transactionId = event.getTransactionId();
        if (transactionId != null && userId != null) {
            String streamKey = userId + ":" + transactionId;
            SseEmitter transactionEmitter = transactionStreams.get(streamKey);
            if (transactionEmitter != null) {
                try {
                    transactionEmitter.send(SseEmitter.event()
                        .name("transaction-update")
                        .data(event));
                } catch (IOException e) {
                    logger.error("Error sending event to transaction stream", e);
                    transactionStreams.remove(streamKey);
                }
            }
        }
    }
}

