package com.wellsfargo.signaturestudio.repository;

import com.wellsfargo.signaturestudio.enums.ReportExecutionStatus;
import com.wellsfargo.signaturestudio.model.ReportExecution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository for ReportExecution entities.
 */
@Repository
public interface ReportExecutionRepository extends JpaRepository<ReportExecution, String> {

    /**
     * Find executions by report definition, ordered by creation date descending.
     */
    Page<ReportExecution> findByReportDefinitionIdOrderByCreatedAtDesc(
        String reportDefinitionId, Pageable pageable);

    /**
     * Find executions by creator, ordered by creation date descending.
     */
    Page<ReportExecution> findByCreatedByOrderByCreatedAtDesc(String createdBy, Pageable pageable);

    /**
     * Find executions by status.
     */
    List<ReportExecution> findByStatus(ReportExecutionStatus status);

    /**
     * Find recent executions for a definition (for history/audit).
     */
    @Query("SELECT r FROM ReportExecution r WHERE r.reportDefinitionId = :defId " +
           "ORDER BY r.createdAt DESC")
    List<ReportExecution> findRecentExecutions(@Param("defId") String reportDefinitionId, Pageable pageable);

    /**
     * Find old executions for cleanup.
     * Used by scheduled cleanup job to remove old report files.
     */
    @Query("SELECT r FROM ReportExecution r WHERE r.createdAt < :cutoffDate " +
           "AND r.status IN ('COMPLETED', 'FAILED')")
    List<ReportExecution> findOldExecutionsForCleanup(@Param("cutoffDate") Instant cutoffDate);

    /**
     * Count executions by report definition.
     */
    long countByReportDefinitionId(String reportDefinitionId);

    /**
     * Count executions by creator.
     */
    long countByCreatedBy(String createdBy);

    /**
     * Find executions by creator and report definition.
     */
    Page<ReportExecution> findByCreatedByAndReportDefinitionIdOrderByCreatedAtDesc(
        String createdBy, String reportDefinitionId, Pageable pageable);
}
