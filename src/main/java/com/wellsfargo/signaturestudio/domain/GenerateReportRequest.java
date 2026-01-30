package com.wellsfargo.signaturestudio.domain;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * Request domain object for generating a report.
 * Can be used to generate a report with optional field overrides.
 */
public class GenerateReportRequest {

    @NotBlank(message = "Report definition ID is required")
    private String reportDefinitionId;

    // Optional overrides for one-time generation
    private List<String> selectedFields; // Override definition's fields
    private String accountId;            // Override definition's account filter
    private ReportFilterCriteria filterCriteria; // Override definition's filters

    // Getters and Setters
    public String getReportDefinitionId() {
        return reportDefinitionId;
    }

    public void setReportDefinitionId(String reportDefinitionId) {
        this.reportDefinitionId = reportDefinitionId;
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

    public ReportFilterCriteria getFilterCriteria() {
        return filterCriteria;
    }

    public void setFilterCriteria(ReportFilterCriteria filterCriteria) {
        this.filterCriteria = filterCriteria;
    }
}
