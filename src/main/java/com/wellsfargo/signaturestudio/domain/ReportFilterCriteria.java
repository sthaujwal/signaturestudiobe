package com.wellsfargo.signaturestudio.domain;

import java.time.Instant;
import java.util.List;

/**
 * Filter criteria for report generation.
 * Allows filtering by date ranges, status, and other fields.
 */
public class ReportFilterCriteria {

    // Date range filters
    private Instant createdAfter;      // Transactions created after this date
    private Instant createdBefore;     // Transactions created before this date
    private Instant updatedAfter;      // Transactions updated after this date
    private Instant updatedBefore;     // Transactions updated before this date

    // Status filters
    private List<String> statuses;     // Filter by one or more statuses (pending, in-progress, completed, rejected)

    // Priority filter
    private List<String> priorities;   // Filter by priority (low, medium, high)

    // Form type filter
    private String formType;

    // Text search
    private String searchText;         // Search in title and description

    // Getters and Setters
    public Instant getCreatedAfter() {
        return createdAfter;
    }

    public void setCreatedAfter(Instant createdAfter) {
        this.createdAfter = createdAfter;
    }

    public Instant getCreatedBefore() {
        return createdBefore;
    }

    public void setCreatedBefore(Instant createdBefore) {
        this.createdBefore = createdBefore;
    }

    public Instant getUpdatedAfter() {
        return updatedAfter;
    }

    public void setUpdatedAfter(Instant updatedAfter) {
        this.updatedAfter = updatedAfter;
    }

    public Instant getUpdatedBefore() {
        return updatedBefore;
    }

    public void setUpdatedBefore(Instant updatedBefore) {
        this.updatedBefore = updatedBefore;
    }

    public List<String> getStatuses() {
        return statuses;
    }

    public void setStatuses(List<String> statuses) {
        this.statuses = statuses;
    }

    public List<String> getPriorities() {
        return priorities;
    }

    public void setPriorities(List<String> priorities) {
        this.priorities = priorities;
    }

    public String getFormType() {
        return formType;
    }

    public void setFormType(String formType) {
        this.formType = formType;
    }

    public String getSearchText() {
        return searchText;
    }

    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    /**
     * Check if any filters are applied.
     */
    public boolean hasFilters() {
        return createdAfter != null || createdBefore != null ||
               updatedAfter != null || updatedBefore != null ||
               (statuses != null && !statuses.isEmpty()) ||
               (priorities != null && !priorities.isEmpty()) ||
               formType != null ||
               (searchText != null && !searchText.isEmpty());
    }
}
