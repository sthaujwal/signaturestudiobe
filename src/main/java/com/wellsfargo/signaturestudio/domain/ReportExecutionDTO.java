package com.wellsfargo.signaturestudio.domain;

import com.wellsfargo.signaturestudio.enums.ReportExecutionStatus;
import java.time.Instant;

/**
 * Response domain object for report execution.
 */
public class ReportExecutionDTO {
    private String id;
    private String reportDefinitionId;
    private String reportName;
    private ReportExecutionStatus status;
    private String fileName;
    private Long fileSize;
    private Integer rowCount;
    private Instant startedAt;
    private Instant completedAt;
    private String errorMessage;
    private String createdBy;
    private Instant createdAt;
    private String executionType;
    private Long durationMs; // Calculated: completedAt - startedAt

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

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }
}
