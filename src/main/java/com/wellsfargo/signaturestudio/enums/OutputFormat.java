package com.wellsfargo.signaturestudio.enums;

/**
 * Enum representing different output formats for generated reports.
 */
public enum OutputFormat {
    CSV("csv", "text/csv", ".csv");
    // Future: EXCEL, PDF, JSON

    private final String code;
    private final String mimeType;
    private final String fileExtension;

    OutputFormat(String code, String mimeType, String fileExtension) {
        this.code = code;
        this.mimeType = mimeType;
        this.fileExtension = fileExtension;
    }

    public String getCode() {
        return code;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public static OutputFormat fromCode(String code) {
        for (OutputFormat format : values()) {
            if (format.code.equals(code)) {
                return format;
            }
        }
        throw new IllegalArgumentException("Unknown output format code: " + code);
    }
}
