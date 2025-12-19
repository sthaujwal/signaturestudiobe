package com.wellsfargo.signaturestudio.enums;

/**
 * Enum for authentication types.
 * Represents the authentication method used for a user.
 */
public enum AuthType {
    OLB("OLB"),
    OLX("OLX"),
    AD_ENT("AD_ENT"),
    OTHER("OTHER");
    
    private final String value;
    
    AuthType(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    /**
     * Converts a string value to AuthType enum.
     * 
     * @param value The string value to convert
     * @return AuthType enum or null if not found
     */
    public static AuthType fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (AuthType type : AuthType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return null;
    }
    
    /**
     * Checks if a string value is a valid AuthType.
     * 
     * @param value The string value to check
     * @return true if valid, false otherwise
     */
    public static boolean isValid(String value) {
        return fromValue(value) != null;
    }
}

