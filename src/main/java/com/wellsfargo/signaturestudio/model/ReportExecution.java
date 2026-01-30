package com.wellsfargo.signaturestudio.model;

import com.wellsfargo.signaturestudio.enums.ReportExecutionStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing a report execution with status tracking and file information.
 */
@Entity
@Table(name = "report_executions", indexes = {
    @Index(name = "idx_report_exec_def_id", columnList = "report_definition_id"),
    @Index(name = "idx_report_exec_created_by", columnList = "created_by"),
    @Index(name = "idx_report_exec_status", columnList = "status"),
    @Index(name = "idx_report_exec_created_at", columnList = "created_at")
})
public class ReportExecution {

    @Id
    @Column(length = 255)
    private String id;

    @Column(name = "report_definition_id", length = 255, nullable = false)
    private String reportDefinitionId;

    @Column(name = "report_name", length = 255, nullable = false)
    private String reportName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private ReportExecutionStatus status;

    @Column(name = "file_path", length = 1000)
    private String filePath;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "row_count")
    private Integer rowCount;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "created_by", length = 255, nullable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "execution_type", length = 20)
    private String executionType; // "manual" or "scheduled"

    @PrePersist
    protected void onCreate() {
        if (id == null || id.isEmpty()) {
            id = UUID.randomUUID().toString();
        }
        createdAt = Instant.now();
        if (status == null) {
            status = ReportExecutionStatus.PENDING;
        }
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getReportDefinitionId() {
        return reportDefinitionId;
    }

    public void setReportDefinitionId(String reportDefinitionId) {
        this.reportDefinitionId = reportDefinitionId;
    }

    public String getReportName() {
        return reportName;
    }

    public void setReportName(String reportName) {
        this.reportName = reportName;
    }

    public ReportExecutionStatus getStatus() {
        return status;
    }

    public void setStatus(ReportExecutionStatus status) {
        this.status = status;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public Integer getRowCount() {
        return rowCount;
    }

    public void setRowCount(Integer rowCount) {
        this.rowCount = rowCount;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getExecutionType() {
        return executionType;
    }

    public void setExecutionType(String executionType) {
        this.executionType = executionType;
    }
}
