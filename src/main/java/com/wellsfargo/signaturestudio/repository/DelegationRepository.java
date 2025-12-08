package com.wellsfargo.signaturestudio.repository;

import com.wellsfargo.signaturestudio.model.Delegation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DelegationRepository extends JpaRepository<Delegation, String> {
    
    // Find active delegations for a delegator
    @Query("SELECT d FROM Delegation d WHERE d.delegatorUserId = :delegatorUserId " +
           "AND d.status = 'active' " +
           "AND (d.endDate IS NULL OR d.endDate >= :now) " +
           "AND d.startDate <= :now")
    List<Delegation> findActiveDelegationsByDelegator(
            @Param("delegatorUserId") String delegatorUserId,
            @Param("now") LocalDateTime now);
    
    // Find active delegations where user is a delegate
    @Query("SELECT d FROM Delegation d WHERE d.delegateUserId = :delegateUserId " +
           "AND d.status = 'active' " +
           "AND (d.endDate IS NULL OR d.endDate >= :now) " +
           "AND d.startDate <= :now")
    List<Delegation> findActiveDelegationsByDelegate(
            @Param("delegateUserId") String delegateUserId,
            @Param("now") LocalDateTime now);
    
    // Find all delegations for a delegator (active and inactive)
    List<Delegation> findByDelegatorUserId(String delegatorUserId);
    
    // Find all delegations where user is a delegate
    List<Delegation> findByDelegateUserId(String delegateUserId);
    
    // Find active delegations for a delegator within account scope
    @Query("SELECT d FROM Delegation d WHERE d.delegatorUserId = :delegatorUserId " +
           "AND (d.accountId IS NULL OR d.accountId = :accountId) " +
           "AND d.status = 'active' " +
           "AND (d.endDate IS NULL OR d.endDate >= :now) " +
           "AND d.startDate <= :now")
    List<Delegation> findActiveDelegationsByDelegatorAndAccount(
            @Param("delegatorUserId") String delegatorUserId,
            @Param("accountId") String accountId,
            @Param("now") LocalDateTime now);
    
    // Find delegations by status
    List<Delegation> findByStatus(String status);
    
    // Check if delegation exists for delegator and delegate
    Optional<Delegation> findByDelegatorUserIdAndDelegateUserIdAndStatus(
            String delegatorUserId, String delegateUserId, String status);
}

