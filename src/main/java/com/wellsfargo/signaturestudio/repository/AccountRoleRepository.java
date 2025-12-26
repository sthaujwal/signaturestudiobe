package com.wellsfargo.signaturestudio.repository;

import com.wellsfargo.signaturestudio.model.AccountRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRoleRepository extends JpaRepository<AccountRole, String> {
    
    /**
     * Find all roles for an account
     */
    List<AccountRole> findByAccount_AccountId(String accountId);
    
    /**
     * Find role by role name and account ID
     */
    Optional<AccountRole> findByRoleNameAndAccount_AccountId(String roleName, String accountId);
    
    /**
     * Find role by role name (could be used for checking if role exists)
     */
    Optional<AccountRole> findByRoleName(String roleName);
}

