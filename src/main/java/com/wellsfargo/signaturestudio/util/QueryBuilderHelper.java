package com.wellsfargo.signaturestudio.util;

import org.springframework.lang.NonNull;
import org.springframework.data.domain.Sort;

/**
 * Helper class for building query-related objects.
 * Reduces cyclomatic complexity in query building methods.
 */
public final class QueryBuilderHelper {
    
    private static final String DEFAULT_SORT_FIELD = "createdAt";
    private static final String DESC_DIRECTION = "desc";
    
    private QueryBuilderHelper() {
        // Utility class - prevent instantiation
    }
    
    /**
     * Creates a Sort object from sort parameters.
     * 
     * @param sortBy Field to sort by (defaults to "createdAt")
     * @param sortDirection Sort direction "asc" or "desc" (defaults to "desc")
     * @return Sort object (never null)
     */
    @NonNull
    public static Sort createSort(String sortBy, String sortDirection) {
        String field = (sortBy != null && !sortBy.trim().isEmpty()) ? sortBy : DEFAULT_SORT_FIELD;
        Sort.Direction direction = (sortDirection != null && 
            sortDirection.equalsIgnoreCase(DESC_DIRECTION)) 
            ? Sort.Direction.DESC 
            : Sort.Direction.ASC;
        return Sort.by(direction, field);
    }
    
    /**
     * Normalizes search text by trimming whitespace.
     * Returns null if the result would be empty.
     */
    public static String normalizeSearchText(String searchText) {
        if (searchText == null) {
            return null;
        }
        String trimmed = searchText.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

