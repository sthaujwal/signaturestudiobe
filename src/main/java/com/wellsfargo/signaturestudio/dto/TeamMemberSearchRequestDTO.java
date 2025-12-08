package com.wellsfargo.signaturestudio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for team member search request.
 */
public class TeamMemberSearchRequestDTO {
    
    @NotBlank(message = "Search query is required")
    @Size(min = 2, max = 100, message = "Search query must be between 2 and 100 characters")
    private String query;
    
    @Size(max = 50, message = "Max results must not exceed 50")
    private Integer maxResults;
    
    public TeamMemberSearchRequestDTO() {
    }
    
    public TeamMemberSearchRequestDTO(String query, Integer maxResults) {
        this.query = query;
        this.maxResults = maxResults;
    }
    
    public String getQuery() {
        return query;
    }
    
    public void setQuery(String query) {
        this.query = query;
    }
    
    public Integer getMaxResults() {
        return maxResults != null ? maxResults : 20; // Default to 20
    }
    
    public void setMaxResults(Integer maxResults) {
        this.maxResults = maxResults;
    }
}

