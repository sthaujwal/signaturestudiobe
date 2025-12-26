package com.wellsfargo.signaturestudio.controller;

import com.wellsfargo.signaturestudio.constants.SessionConstants;
import com.wellsfargo.signaturestudio.dto.PaginatedResponseDTO;
import com.wellsfargo.signaturestudio.dto.TransactionDTO;
import com.wellsfargo.signaturestudio.service.TransactionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for search operations.
 * Provides unified search across transactions, users, and other entities.
 */
@RestController
@RequestMapping("/api/search")
public class SearchController {
    
    private final TransactionService transactionService;
    
    public SearchController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }
    
    /**
     * Search transactions, signers, and IDs.
     * This is the main search endpoint used by the top navigation search bar.
     * 
     * Query Parameters:
     * - q (required): Search query text
     * - accountId (optional): Filter by account ID
     * - page (default: 0): Page number
     * - size (default: 20): Page size
     * 
     * @param query Search query text
     * @param accountId Optional account ID filter
     * @param page Page number
     * @param size Page size
     * @param session HTTP session
     * @return Paginated search results
     */
    @GetMapping
    public ResponseEntity<PaginatedResponseDTO<TransactionDTO>> search(
            @RequestParam("q") String query,
            @RequestParam(required = false) String accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpSession session) {
        String userId = (String) session.getAttribute(SessionConstants.USERNAME);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        // Use the existing transaction search with delegation support
        PaginatedResponseDTO<TransactionDTO> results = transactionService.getTransactionsWithDelegations(
            accountId, userId, query, page, size, "createdAt", "desc");
        
        return ResponseEntity.ok(results);
    }
}

