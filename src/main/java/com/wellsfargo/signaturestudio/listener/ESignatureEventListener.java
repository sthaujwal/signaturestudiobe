package com.wellsfargo.signaturestudio.listener;

import com.wellsfargo.signaturestudio.domain.TransactionEvent;
import com.wellsfargo.signaturestudio.exception.ErrorCode;
import com.wellsfargo.signaturestudio.exception.ServiceException;
import com.wellsfargo.signaturestudio.model.Transaction;
import com.wellsfargo.signaturestudio.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Kafka listener for events from ESignatureService
 * Updates transaction status in the database based on events
 */
@Component
public class ESignatureEventListener {
    
    private static final Logger logger = LoggerFactory.getLogger(ESignatureEventListener.class);
    
    private final TransactionRepository transactionRepository;
    
    public ESignatureEventListener(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }
    
    @KafkaListener(
            topics = "${kafka.topic.esignature.events:esignature-transaction-events}",
            groupId = "${spring.kafka.consumer.group-id:signaturestudio-bff-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void handleTransactionEvent(
            @Payload TransactionEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        
        logger.info("Received transaction event: type={}, transactionId={}, status={}, from topic={}, partition={}, offset={}",
                event.getEventType(), event.getTransactionId(), event.getStatus(), topic, partition, offset);
        
        try {
            // Find transaction by eSignature transaction ID
            Optional<Transaction> transactionOpt = transactionRepository.findByESignatureTransactionId(
                    event.getTransactionId());
            
            if (transactionOpt.isEmpty()) {
                // Try to find by BFF transaction ID if provided
                if (event.getBffTransactionId() != null) {
                    transactionOpt = transactionRepository.findById(event.getBffTransactionId());
                }
            }
            
            if (transactionOpt.isEmpty()) {
                logger.warn("Transaction not found for eSignature transaction ID: {} or BFF transaction ID: {}. Event will be skipped.",
                        event.getTransactionId(), event.getBffTransactionId());
                if (acknowledgment != null) {
                    acknowledgment.acknowledge();
                }
                return;
            }
            
            Transaction transaction = transactionOpt.get();
            String previousStatus = transaction.getStatus();
            
            // Update transaction status based on event
            if (event.getStatus() != null && !event.getStatus().isEmpty()) {
                transaction.setStatus(event.getStatus());
                logger.info("Updated transaction {} status from {} to {} based on event type: {}",
                        transaction.getId(), previousStatus, event.getStatus(), event.getEventType());
            }
            
            // Update transaction metadata if provided
            if (event.getMetadata() != null) {
                // Store additional metadata in transaction attributes if needed
                // For now, we'll just log it
                logger.debug("Event metadata received: {}", event.getMetadata());
            }
            
            // Save updated transaction
            transaction = transactionRepository.save(transaction);
            logger.info("Transaction {} updated successfully. New status: {}", transaction.getId(), transaction.getStatus());
            
            // Acknowledge the message
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
            
        } catch (Exception e) {
            logger.error("Error processing transaction event: type={}, transactionId={}", 
                    event.getEventType(), event.getTransactionId(), e);
            
            // In production, you might want to:
            // 1. Send to a dead letter queue
            // 2. Retry with exponential backoff
            // 3. Alert monitoring system
            
            // For now, we'll acknowledge to prevent message redelivery loop
            // In production, consider implementing retry logic
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
            
            throw new ServiceException(ErrorCode.INTERNAL_ERROR, 
                    "Failed to process transaction event: " + e.getMessage());
        }
    }
    
}

