package com.wellsfargo.signaturestudio.domain;

import com.wellsfargo.signaturestudio.enums.OutputFormat;
import com.wellsfargo.signaturestudio.enums.ScheduleFrequency;
import com.wellsfargo.signaturestudio.validation.NoXss;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Request domain object for updating an existing report definition.
 * All fields are optional to allow partial updates.
 */
public class UpdateReportDefinitionRequest {

    @NoXss
    @Size(min = 3, max = 255, message = "Report name must be between 3 and 255 characters")
    private String reportName;

    @NoXss
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    private OutputFormat outputFormat;

    @Size(min = 1, message = "At least one field must be selected")
    private List<String> selectedFields;

    @NoXss
    private String accountId;

    private Boolean includeAllAccounts;

    private ScheduleFrequency scheduleFrequency;

    private Boolean scheduleEnabled;

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

    public Boolean getIncludeAllAccounts() {
        return includeAllAccounts;
    }

    public void setIncludeAllAccounts(Boolean includeAllAccounts) {
        this.includeAllAccounts = includeAllAccounts;
    }

    public ScheduleFrequency getScheduleFrequency() {
        return scheduleFrequency;
    }

    public void setScheduleFrequency(ScheduleFrequency scheduleFrequency) {
        this.scheduleFrequency = scheduleFrequency;
    }

    public Boolean getScheduleEnabled() {
        return scheduleEnabled;
    }

    public void setScheduleEnabled(Boolean scheduleEnabled) {
        this.scheduleEnabled = scheduleEnabled;
    }
}
