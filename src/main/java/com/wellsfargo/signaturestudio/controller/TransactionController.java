package com.wellsfargo.signaturestudio.controller;

import com.wellsfargo.signaturestudio.constants.SessionConstants;
import com.wellsfargo.signaturestudio.dto.PaginatedResponseDTO;
import com.wellsfargo.signaturestudio.dto.TransactionDTO;
import com.wellsfargo.signaturestudio.service.TransactionService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {
    
    private final TransactionService transactionService;
    
    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }
    
    /**
     * Get transactions for logged-in user including delegated transactions (with pagination and search)
     * This is the main endpoint to call after user login
     * 
     * Query Parameters:
     * - accountId (optional): Filter by account ID
     * - search (optional): Search text to search in title and description (case-insensitive, partial match)
     * - page (default: 0): Page number (0-indexed)
     * - size (default: 20): Number of items per page
     * - sortBy (default: createdAt): Field to sort by (createdAt, title, status, etc.)
     * - sortDirection (default: desc): Sort direction (asc or desc)
     * 
     * Search Examples:
     * - "contract" - finds transactions with "contract" in title or description
     * - "agreement 2024" - finds transactions containing both words
     * - "John Doe" - finds transactions mentioning "John Doe"
     */
    @GetMapping("/my-transactions")
    public ResponseEntity<PaginatedResponseDTO<TransactionDTO>> getMyTransactions(
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            HttpSession session) {
        String userId = (String) session.getAttribute(SessionConstants.USERNAME);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        PaginatedResponseDTO<TransactionDTO> response = transactionService.getTransactionsWithDelegations(
                accountId, userId, search, page, size, sortBy, sortDirection);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get transactions (legacy endpoint without pagination - kept for backward compatibility)
     */
    @GetMapping
    public ResponseEntity<List<TransactionDTO>> getTransactions(
            @RequestParam(required = false) String accountId,
            HttpSession session) {
        String createdBy = (String) session.getAttribute(SessionConstants.USERNAME);
        List<TransactionDTO> transactions = transactionService.getTransactions(accountId, createdBy);
        return ResponseEntity.ok(transactions);
    }
    
    /**
     * Get transactions with pagination and search (without delegation support)
     */
    @GetMapping("/paginated")
    public ResponseEntity<PaginatedResponseDTO<TransactionDTO>> getTransactionsPaginated(
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            HttpSession session) {
        String createdBy = (String) session.getAttribute(SessionConstants.USERNAME);
        PaginatedResponseDTO<TransactionDTO> response = transactionService.getTransactionsPaginated(
                accountId, createdBy, search, page, size, sortBy, sortDirection);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping
    public ResponseEntity<TransactionDTO> createTransaction(
            @Valid @RequestBody TransactionDTO transactionDTO,
            HttpSession session) {
        String createdBy = (String) session.getAttribute(SessionConstants.USERNAME);
        String creatorEmail = (String) session.getAttribute(SessionConstants.EMAIL);
        
        if (createdBy == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        // If email is not in session, try to get it from the DTO
        if (creatorEmail == null) {
            creatorEmail = transactionDTO.getCreatorEmail();
        }
        
        TransactionDTO created = transactionService.createTransaction(transactionDTO, createdBy, creatorEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<TransactionDTO> getTransaction(@PathVariable String id) {
        TransactionDTO transaction = transactionService.getTransaction(id);
        return ResponseEntity.ok(transaction);
    }
    
    /**
     * Get full transaction details from ESignatureService
     * Includes documents, form fields, attributes, and ICMP objects
     */
    @GetMapping("/{id}/details")
    public ResponseEntity<TransactionDTO> getTransactionDetails(@PathVariable String id) {
        TransactionDTO transaction = transactionService.getTransactionDetails(id);
        return ResponseEntity.ok(transaction);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<TransactionDTO> updateTransaction(
            @PathVariable String id,
            @Valid @RequestBody TransactionDTO transactionDTO) {
        TransactionDTO updated = transactionService.updateTransaction(id, transactionDTO);
        return ResponseEntity.ok(updated);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable String id) {
        transactionService.deleteTransaction(id);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> getTransactionStatus(@PathVariable String id) {
        Map<String, Object> status = transactionService.getTransactionStatus(id);
        return ResponseEntity.ok(status);
    }
}


