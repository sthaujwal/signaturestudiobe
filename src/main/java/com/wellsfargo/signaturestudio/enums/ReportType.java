package com.wellsfargo.signaturestudio.enums;

/**
 * Enum representing different types of reports that can be generated.
 */
public enum ReportType {
    TRANSACTION_REPORT("transaction_report", "Transaction Report");
    // Future: DOCUMENT_REPORT, USER_ACTIVITY_REPORT, AUDIT_REPORT

    private final String code;
    private final String displayName;

    ReportType(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static ReportType fromCode(String code) {
        for (ReportType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown report type code: " + code);
    }
}
