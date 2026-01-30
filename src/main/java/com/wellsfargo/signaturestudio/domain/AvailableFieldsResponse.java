package com.wellsfargo.signaturestudio.domain;

import com.wellsfargo.signaturestudio.enums.ReportType;
import java.util.List;

/**
 * Response domain object containing available fields for a report type.
 */
public class AvailableFieldsResponse {
    private ReportType reportType;
    private List<FieldOption> fields;
    private List<String> defaultFields; // Recommended default selection

    public AvailableFieldsResponse() {
    }

    public AvailableFieldsResponse(ReportType reportType, List<FieldOption> fields, List<String> defaultFields) {
        this.reportType = reportType;
        this.fields = fields;
        this.defaultFields = defaultFields;
    }

    // Getters and Setters
    public ReportType getReportType() {
        return reportType;
    }

    public void setReportType(ReportType reportType) {
        this.reportType = reportType;
    }

    public List<FieldOption> getFields() {
        return fields;
    }

    public void setFields(List<FieldOption> fields) {
        this.fields = fields;
    }

    public List<String> getDefaultFields() {
        return defaultFields;
    }

    public void setDefaultFields(List<String> defaultFields) {
        this.defaultFields = defaultFields;
    }
}
