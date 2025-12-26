package com.wellsfargo.signaturestudio.repository;

import com.wellsfargo.signaturestudio.model.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<AccountEntity, String> {
    
    /**
     * Find account by account ID
     */
    Optional<AccountEntity> findByAccountId(String accountId);
    
    /**
     * Find account by account key
     */
    Optional<AccountEntity> findByAccountKey(String accountKey);
    
    /**
     * Check if account exists by account ID
     */
    boolean existsByAccountId(String accountId);
    
    /**
     * Check if account exists by account key
     */
    boolean existsByAccountKey(String accountKey);
}

