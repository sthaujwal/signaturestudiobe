package com.wellsfargo.signaturestudio.enums;

/**
 * Enum representing available field types for report generation.
 */
public enum FieldType {
    ACCT_NAME("Account Name", "transaction", "accountCode"),
    ACCT_ID("Account ID", "transaction", "accountId"),
    TXN_NAME("Transaction Name", "transaction", "title"),
    TXN_GUID("Transaction ID", "transaction", "id"),
    TXN_STATUS("Transaction Status", "transaction", "status"),
    TXN_STATUS_TMSTP("Transaction Status Timestamp", "transaction", "updatedAt"),
    DOC_TITLE("Document Title", "document", "documents.title"),
    DOC_ID("Document ID", "document", "documents.id"),
    DOC_STATUS("Document Status", "document", "documents.status"),
    SIGNER_EXTERNAL_ID("Signer External ID", "user", "users.externalId"),
    SIGNER_EXTERNAL_ID_TYPE("Signer User Category", "user", "users.userCategory"),
    TXN_SENDER_ID("Transaction Sender ID", "transaction", "creatorEmail");

    private final String displayName;
    private final String category;
    private final String fieldPath;

    FieldType(String displayName, String category, String fieldPath) {
        this.displayName = displayName;
        this.category = category;
        this.fieldPath = fieldPath;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCategory() {
        return category;
    }

    public String getFieldPath() {
        return fieldPath;
    }

    public static FieldType fromFieldPath(String fieldPath) {
        for (FieldType type : values()) {
            if (type.fieldPath.equals(fieldPath)) {
                return type;
            }
        }
        return null;
    }
}
