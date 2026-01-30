package com.wellsfargo.signaturestudio.domain;

import com.wellsfargo.signaturestudio.enums.OutputFormat;
import com.wellsfargo.signaturestudio.enums.ReportType;
import com.wellsfargo.signaturestudio.enums.ScheduleFrequency;
import java.time.Instant;
import java.util.List;

/**
 * Response domain object for report definition.
 */
public class ReportDefinitionDTO {
    private String id;
    private String reportName;
    private String description;
    private ReportType reportType;
    private OutputFormat outputFormat;
    private List<String> selectedFields;
    private String accountId;
    private boolean includeAllAccounts;
    private ScheduleFrequency scheduleFrequency;
    private Instant nextRunTime;
    private Instant lastRunTime;
    private boolean scheduleEnabled;
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;
    private boolean isActive;

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
