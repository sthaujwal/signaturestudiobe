package com.wellsfargo.signaturestudio.model;

import com.wellsfargo.signaturestudio.config.StringListJsonConverter;
import com.wellsfargo.signaturestudio.enums.OutputFormat;
import com.wellsfargo.signaturestudio.enums.ReportType;
import com.wellsfargo.signaturestudio.enums.ScheduleFrequency;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing a report definition with configuration for field selection,
 * scheduling, and account scope.
 */
@Entity
@Table(name = "report_definitions", indexes = {
    @Index(name = "idx_report_def_created_by", columnList = "created_by"),
    @Index(name = "idx_report_def_account_id", columnList = "account_id"),
    @Index(name = "idx_report_def_next_run", columnList = "next_run_time")
})
public class ReportDefinition {

    @Id
    @Column(length = 255)
    private String id;

    @Column(name = "report_name", length = 255, nullable = false)
    private String reportName;

    @Column(name = "description", length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", length = 50, nullable = false)
    private ReportType reportType;

    @Enumerated(EnumType.STRING)
    @Column(name = "output_format", length = 20, nullable = false)
    private OutputFormat outputFormat;

    @Convert(converter = StringListJsonConverter.class)
    @Column(name = "selected_fields", columnDefinition = "CLOB", nullable = false)
    private List<String> selectedFields;

    @Column(name = "account_id", length = 255)
    private String accountId;

    @Column(name = "include_all_accounts")
    private boolean includeAllAccounts;

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_frequency", length = 20, nullable = false)
    private ScheduleFrequency scheduleFrequency;

    @Column(name = "next_run_time")
    private Instant nextRunTime;

    @Column(name = "last_run_time")
    private Instant lastRunTime;

    @Column(name = "schedule_enabled")
    private boolean scheduleEnabled;

    @Column(name = "created_by", length = 255, nullable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "is_active")
    private boolean isActive;

    @PrePersist
    protected void onCreate() {
        if (id == null || id.isEmpty()) {
            id = UUID.randomUUID().toString();
        }
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (scheduleFrequency == null) {
            scheduleFrequency = ScheduleFrequency.NONE;
        }
        isActive = true;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getReportName() {
        return reportName;
    }

    public void setReportName(String reportName) {
        this.reportName = reportName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ReportType getReportType() {
        return reportType;
    }

    public void setReportType(ReportType reportType) {
        this.reportType = reportType;
    }

    public OutputFormat getOutputFormat() {
        return outputFormat;
    }

    public void setOutputFormat(OutputFormat outputFormat) {
        this.outputFormat = outputFormat;
    }

    public List<String> getSelectedFields() {
        return selectedFields;
    }

    public void setSelectedFields(List<String> selectedFields) {
        this.selectedFields = selectedFields;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public boolean isIncludeAllAccounts() {
        return includeAllAccounts;
    }

    public void setIncludeAllAccounts(boolean includeAllAccounts) {
        this.includeAllAccounts = includeAllAccounts;
    }

    public ScheduleFrequency getScheduleFrequency() {
        return scheduleFrequency;
    }

    public void setScheduleFrequency(ScheduleFrequency scheduleFrequency) {
        this.scheduleFrequency = scheduleFrequency;
    }

    public Instant getNextRunTime() {
        return nextRunTime;
    }

    public void setNextRunTime(Instant nextRunTime) {
        this.nextRunTime = nextRunTime;
    }

    public Instant getLastRunTime() {
        return lastRunTime;
    }

    public void setLastRunTime(Instant lastRunTime) {
        this.lastRunTime = lastRunTime;
    }

    public boolean isScheduleEnabled() {
        return scheduleEnabled;
    }

    public void setScheduleEnabled(boolean scheduleEnabled) {
        this.scheduleEnabled = scheduleEnabled;
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

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}
