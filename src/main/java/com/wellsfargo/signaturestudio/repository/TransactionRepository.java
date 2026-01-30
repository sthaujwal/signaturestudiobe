package com.wellsfargo.signaturestudio.repository;

import com.wellsfargo.signaturestudio.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String>, JpaSpecificationExecutor<Transaction> {
    
    List<Transaction> findByAccountId(String accountId);
    
    List<Transaction> findByCreatedBy(String createdBy);
    
    List<Transaction> findByStatus(String status);
    
    // Paginated queries
    Page<Transaction> findByAccountId(String accountId, Pageable pageable);
    
    Page<Transaction> findByCreatedBy(String createdBy, Pageable pageable);
    
    Page<Transaction> findByStatus(String status, Pageable pageable);
    
    @Query("SELECT t FROM Transaction t WHERE t.accountId = :accountId AND t.status = :status")
    List<Transaction> findByAccountIdAndStatus(@Param("accountId") String accountId, 
                                                @Param("status") String status);
    
    @Query("SELECT t FROM Transaction t WHERE t.accountId = :accountId AND t.status = :status")
    Page<Transaction> findByAccountIdAndStatus(@Param("accountId") String accountId, 
                                                @Param("status") String status,
                                                Pageable pageable);
    
    @Query("SELECT t FROM Transaction t WHERE t.createdBy = :createdBy AND t.accountId = :accountId")
    List<Transaction> findByCreatedByAndAccountId(@Param("createdBy") String createdBy, 
                                                  @Param("accountId") String accountId);
    
    @Query("SELECT t FROM Transaction t WHERE t.createdBy = :createdBy AND t.accountId = :accountId")
    Page<Transaction> findByCreatedByAndAccountId(@Param("createdBy") String createdBy, 
                                                  @Param("accountId") String accountId,
                                                  Pageable pageable);
    
    // Query for transactions created by any of the provided user IDs (for delegation support)
    @Query("SELECT t FROM Transaction t WHERE t.createdBy IN :userIds")
    Page<Transaction> findByCreatedByIn(@Param("userIds") List<String> userIds, Pageable pageable);
    
    @Query("SELECT t FROM Transaction t WHERE t.createdBy IN :userIds AND t.accountId = :accountId")
    Page<Transaction> findByCreatedByInAndAccountId(@Param("userIds") List<String> userIds, 
                                                     @Param("accountId") String accountId,
                                                     Pageable pageable);
    
    // Search queries with text similarity (case-insensitive LIKE search)
    @Query("SELECT t FROM Transaction t WHERE " +
           "t.createdBy IN :userIds AND " +
           "(UPPER(t.title) LIKE UPPER(CONCAT('%', :searchText, '%')) OR " +
           "UPPER(t.description) LIKE UPPER(CONCAT('%', :searchText, '%')))")
    Page<Transaction> findByCreatedByInAndSearchText(@Param("userIds") List<String> userIds,
                                                      @Param("searchText") String searchText,
                                                      Pageable pageable);
    
    @Query("SELECT t FROM Transaction t WHERE " +
           "t.createdBy IN :userIds AND t.accountId = :accountId AND " +
           "(UPPER(t.title) LIKE UPPER(CONCAT('%', :searchText, '%')) OR " +
           "UPPER(t.description) LIKE UPPER(CONCAT('%', :searchText, '%')))")
    Page<Transaction> findByCreatedByInAndAccountIdAndSearchText(@Param("userIds") List<String> userIds,
                                                                   @Param("accountId") String accountId,
                                                                   @Param("searchText") String searchText,
                                                                   Pageable pageable);
    
    // Search queries for single user (non-delegation)
    @Query("SELECT t FROM Transaction t WHERE " +
           "t.createdBy = :createdBy AND " +
           "(UPPER(t.title) LIKE UPPER(CONCAT('%', :searchText, '%')) OR " +
           "UPPER(t.description) LIKE UPPER(CONCAT('%', :searchText, '%')))")
    Page<Transaction> findByCreatedByAndSearchText(@Param("createdBy") String createdBy,
                                                    @Param("searchText") String searchText,
                                                    Pageable pageable);
    
    @Query("SELECT t FROM Transaction t WHERE " +
           "t.createdBy = :createdBy AND t.accountId = :accountId AND " +
           "(UPPER(t.title) LIKE UPPER(CONCAT('%', :searchText, '%')) OR " +
           "UPPER(t.description) LIKE UPPER(CONCAT('%', :searchText, '%')))")
    Page<Transaction> findByCreatedByAndAccountIdAndSearchText(@Param("createdBy") String createdBy,
                                                                @Param("accountId") String accountId,
                                                                @Param("searchText") String searchText,
                                                                Pageable pageable);
    
    // Oracle Text search (requires CONTEXT index - more advanced similarity search)
    // Uncomment and use if Oracle Text is configured:
    // @Query(value = "SELECT t.* FROM transaction_metadata t WHERE " +
    //        "t.created_by IN :userIds AND " +
    //        "(CONTAINS(t.title, :searchText, 1) > 0 OR CONTAINS(t.description, :searchText, 1) > 0) " +
    //        "ORDER BY SCORE(1) DESC", nativeQuery = true)
    // Page<Transaction> findByCreatedByInWithOracleText(@Param("userIds") List<String> userIds,
    //                                                    @Param("searchText") String searchText,
    //                                                    Pageable pageable);
    
    Optional<Transaction> findByESignatureTransactionId(String eSignatureTransactionId);
}


