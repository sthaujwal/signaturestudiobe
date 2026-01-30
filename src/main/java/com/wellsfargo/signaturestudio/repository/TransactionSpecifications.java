package com.wellsfargo.signaturestudio.repository;

import com.wellsfargo.signaturestudio.domain.ReportFilterCriteria;
import com.wellsfargo.signaturestudio.model.Transaction;
import com.wellsfargo.signaturestudio.util.SpecificationBuilder;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

/**
 * JPA Specifications for building dynamic Transaction queries.
 * Used for report generation with complex filter criteria.
 */
public class TransactionSpecifications {

    /**
     * Build a specification for report filtering based on ReportFilterCriteria.
     * Applies all filters at the database level for optimal performance.
     *
     * @param accountIds List of accessible account IDs (null for user's transactions only)
     * @param userId User ID for createdBy filter
     * @param criteria Optional filter criteria (date ranges, status, priority, etc.)
     * @return Specification for Transaction query
     */
    public static Specification<Transaction> forReport(List<String> accountIds,
                                                       String userId,
                                                       ReportFilterCriteria criteria) {
        SpecificationBuilder<Transaction> builder = SpecificationBuilder.builder();

        // Account or user filter
        if (accountIds != null && !accountIds.isEmpty()) {
            builder.withIn("accountId", accountIds);
        } else {
            builder.withEquals("createdBy", userId);
        }

        // Apply filter criteria if provided
        if (criteria != null) {
            builder
                // Date range filters
                .withDateRange("createdAt", criteria.getCreatedAfter(), criteria.getCreatedBefore())
                .withDateRange("updatedAt", criteria.getUpdatedAfter(), criteria.getUpdatedBefore())
                // Status filter
                .withIn("status", criteria.getStatuses())
                // Form type filter
                .withEquals("formType", criteria.getFormType())
                // Search text across title and description
                .withLikeOr(criteria.getSearchText(), "title", "description");
        }

        return builder.build();
    }

    /**
     * Specification for transactions by account ID.
     */
    public static Specification<Transaction> byAccountId(String accountId) {
        return (root, query, criteriaBuilder) ->
            criteriaBuilder.equal(root.get("accountId"), accountId);
    }

    /**
     * Specification for transactions by multiple account IDs.
     */
    public static Specification<Transaction> byAccountIds(List<String> accountIds) {
        return (root, query, criteriaBuilder) ->
            root.get("accountId").in(accountIds);
    }

    /**
     * Specification for transactions created by user.
     */
    public static Specification<Transaction> byCreatedBy(String userId) {
        return (root, query, criteriaBuilder) ->
            criteriaBuilder.equal(root.get("createdBy"), userId);
    }

    /**
     * Specification for transactions by status.
     */
    public static Specification<Transaction> byStatus(String status) {
        return (root, query, criteriaBuilder) ->
            criteriaBuilder.equal(root.get("status"), status);
    }

    /**
     * Specification for transactions by multiple statuses.
     */
    public static Specification<Transaction> byStatuses(List<String> statuses) {
        return (root, query, criteriaBuilder) ->
            root.get("status").in(statuses);
    }

    /**
     * Specification for transactions created after a date.
     */
    public static Specification<Transaction> createdAfter(java.time.Instant instant) {
        return (root, query, criteriaBuilder) ->
            criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), instant);
    }

    /**
     * Specification for transactions created before a date.
     */
    public static Specification<Transaction> createdBefore(java.time.Instant instant) {
        return (root, query, criteriaBuilder) ->
            criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), instant);
    }

    /**
     * Specification for text search in title and description.
     */
    public static Specification<Transaction> searchText(String searchText) {
        return (root, query, criteriaBuilder) -> {
            String searchPattern = "%" + searchText.toLowerCase() + "%";
            Predicate titleMatch = criteriaBuilder.like(
                criteriaBuilder.lower(root.get("title")), searchPattern);
            Predicate descriptionMatch = criteriaBuilder.like(
                criteriaBuilder.lower(root.get("description")), searchPattern);
            return criteriaBuilder.or(titleMatch, descriptionMatch);
        };
    }
}
