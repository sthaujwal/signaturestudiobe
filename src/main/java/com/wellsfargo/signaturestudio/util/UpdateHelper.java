package com.wellsfargo.signaturestudio.util;

import java.util.function.Consumer;

/**
 * Utility class for safely updating entity fields.
 * Helps avoid SonarQube code smells related to repetitive null checks.
 */
public class UpdateHelper {
    
    /**
     * Safely updates a field if the value is not null.
     * 
     * @param value the value to check and apply
     * @param setter the setter function to call if value is not null
     * @param <T> the type of the value
     */
    public static <T> void setIfNotNull(T value, Consumer<T> setter) {
        if (value != null) {
            setter.accept(value);
        }
    }
    
    /**
     * Safely updates a field if the value is not null, with a custom action.
     * Useful when additional logic is needed beyond just setting the value.
     * 
     * @param value the value to check
     * @param action the action to perform if value is not null
     * @param <T> the type of the value
     */
    public static <T> void ifNotNull(T value, Consumer<T> action) {
        if (value != null) {
            action.accept(value);
        }
    }
}

