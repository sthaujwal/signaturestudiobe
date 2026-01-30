package com.wellsfargo.signaturestudio.domain;

/**
 * Domain object representing a field option for report generation.
 * Provides metadata about available fields that can be included in reports.
 */
public class FieldOption {
    private String fieldName;      // e.g., "transaction.id" or "id"
    private String displayName;    // e.g., "Transaction ID"
    private String fieldType;      // e.g., "string", "instant", "number", "list"
    private String category;       // e.g., "transaction", "user", "document"
    private boolean isDefault;     // Include in default field set

    public FieldOption() {
    }

    public FieldOption(String fieldName, String displayName, String fieldType, String category, boolean isDefault) {
        this.fieldName = fieldName;
        this.displayName = displayName;
        this.fieldType = fieldType;
        this.category = category;
        this.isDefault = isDefault;
    }

    // Getters and Setters
    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getFieldType() {
        return fieldType;
    }

    public void setFieldType(String fieldType) {
        this.fieldType = fieldType;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }
}
