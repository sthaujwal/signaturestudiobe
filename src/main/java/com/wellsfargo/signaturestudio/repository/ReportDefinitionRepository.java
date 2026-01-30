package com.wellsfargo.signaturestudio.repository;

import com.wellsfargo.signaturestudio.model.ReportDefinition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository for ReportDefinition entities.
 */
@Repository
public interface ReportDefinitionRepository extends JpaRepository<ReportDefinition, String> {

    /**
     * Find active report definitions by creator.
     */
    List<ReportDefinition> findByCreatedByAndIsActive(String createdBy, boolean isActive);

    /**
     * Find active report definitions by creator with pagination.
     */
    Page<ReportDefinition> findByCreatedByAndIsActive(String createdBy, boolean isActive, Pageable pageable);

    /**
     * Find active report definitions by account.
     */
    List<ReportDefinition> findByAccountIdAndIsActive(String accountId, boolean isActive);

    /**
     * Find active report definitions by creator and account.
     */
    List<ReportDefinition> findByCreatedByAndAccountIdAndIsActive(
        String createdBy, String accountId, boolean isActive);

    /**
     * Find scheduled reports that are due for execution.
     * Used by scheduler to find reports that need to be run.
     */
    @Query("SELECT r FROM ReportDefinition r WHERE r.scheduleEnabled = true " +
           "AND r.scheduleFrequency != 'NONE' AND r.isActive = true " +
           "AND r.nextRunTime <= :currentTime")
    List<ReportDefinition> findScheduledReportsDueForExecution(@Param("currentTime") Instant currentTime);

    /**
     * Find all active report definitions (for org admins).
     */
    List<ReportDefinition> findByIsActive(boolean isActive);

    /**
     * Find all active report definitions with pagination (for org admins).
     */
    Page<ReportDefinition> findByIsActive(boolean isActive, Pageable pageable);
}
