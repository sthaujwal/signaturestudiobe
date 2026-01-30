package com.wellsfargo.signaturestudio.domain;

import com.wellsfargo.signaturestudio.enums.OutputFormat;
import com.wellsfargo.signaturestudio.enums.ReportType;
import com.wellsfargo.signaturestudio.enums.ScheduleFrequency;
import com.wellsfargo.signaturestudio.validation.NoXss;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Request domain object for creating a new report definition.
 */
public class CreateReportDefinitionRequest {

    @NotBlank(message = "Report name is required")
    @NoXss
    @Size(min = 3, max = 255, message = "Report name must be between 3 and 255 characters")
    private String reportName;

    @NoXss
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @NotNull(message = "Report type is required")
    private ReportType reportType;

    @NotNull(message = "Output format is required")
    private OutputFormat outputFormat;

    @NotNull(message = "Selected fields are required")
    @Size(min = 1, message = "At least one field must be selected")
    private List<@NotBlank String> selectedFields;

    @NoXss
    private String accountId; // Optional: null = current user's accessible accounts

    private boolean includeAllAccounts; // Org admin only

    @NotNull(message = "Schedule frequency is required")
    private ScheduleFrequency scheduleFrequency;

    private boolean scheduleEnabled;

    // Optional filter criteria
    private ReportFilterCriteria filterCriteria;

    // Getters and Setters
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

    public boolean isScheduleEnabled() {
        return scheduleEnabled;
    }

    public void setScheduleEnabled(boolean scheduleEnabled) {
        this.scheduleEnabled = scheduleEnabled;
    }

    public ReportFilterCriteria getFilterCriteria() {
        return filterCriteria;
    }

    public void setFilterCriteria(ReportFilterCriteria filterCriteria) {
        this.filterCriteria = filterCriteria;
    }
}
