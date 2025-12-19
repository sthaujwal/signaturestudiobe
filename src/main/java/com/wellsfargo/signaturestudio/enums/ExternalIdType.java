package com.wellsfargo.signaturestudio.enums;

/**
 * Enum for external ID types.
 * Represents the type of external identifier used for a user.
 */
public enum ExternalIdType {
    AD_ENT("AD-ENT"),
    ECN("ECN");
    
    private final String value;
    
    ExternalIdType(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    /**
     * Converts a string value to ExternalIdType enum.
     * 
     * @param value The string value to convert
     * @return ExternalIdType enum or null if not found
     */
    public static ExternalIdType fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (ExternalIdType type : ExternalIdType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return null;
    }
    
    /**
     * Checks if a string value is a valid ExternalIdType.
     * 
     * @param value The string value to check
     * @return true if valid, false otherwise
     */
    public static boolean isValid(String value) {
        return fromValue(value) != null;
    }
}

