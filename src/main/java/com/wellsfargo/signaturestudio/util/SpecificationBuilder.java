package com.wellsfargo.signaturestudio.util;

import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for building JPA Specifications with reduced verbosity.
 * Provides fluent API for constructing complex queries.
 *
 * @param <T> Entity type
 */
public class SpecificationBuilder<T> {

    private final List<Specification<T>> specifications = new ArrayList<>();

    private SpecificationBuilder() {
    }

    /**
     * Create a new SpecificationBuilder.
     */
    public static <T> SpecificationBuilder<T> builder() {
        return new SpecificationBuilder<>();
    }

    /**
     * Add an equals predicate if value is not null.
     */
    public SpecificationBuilder<T> withEquals(String field, Object value) {
        if (value != null) {
            specifications.add((root, query, cb) ->
                cb.equal(root.get(field), value));
        }
        return this;
    }

    /**
     * Add a case-insensitive equals predicate for string values.
     */
    public SpecificationBuilder<T> withEqualsIgnoreCase(String field, String value) {
        if (value != null && !value.isEmpty()) {
            specifications.add((root, query, cb) ->
                cb.equal(cb.lower(root.get(field)), value.toLowerCase()));
        }
        return this;
    }

    /**
     * Add an IN predicate if values list is not empty.
     */
    public SpecificationBuilder<T> withIn(String field, List<?> values) {
        if (values != null && !values.isEmpty()) {
            specifications.add((root, query, cb) ->
                root.get(field).in(values));
        }
        return this;
    }

    /**
     * Add a greater than or equal predicate if value is not null.
     */
    public SpecificationBuilder<T> withGreaterThanOrEqual(String field, Comparable<?> value) {
        if (value != null) {
            specifications.add((root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get(field), (Comparable) value));
        }
        return this;
    }

    /**
     * Add a less than or equal predicate if value is not null.
     */
    public SpecificationBuilder<T> withLessThanOrEqual(String field, Comparable<?> value) {
        if (value != null) {
            specifications.add((root, query, cb) ->
                cb.lessThanOrEqualTo(root.get(field), (Comparable) value));
        }
        return this;
    }

    /**
     * Add a LIKE predicate (case-insensitive) if search text is not null/empty.
     */
    public SpecificationBuilder<T> withLike(String field, String searchText) {
        if (searchText != null && !searchText.isEmpty()) {
            String pattern = "%" + searchText.toLowerCase() + "%";
            specifications.add((root, query, cb) ->
                cb.like(cb.lower(root.get(field)), pattern));
        }
        return this;
    }

    /**
     * Add an OR predicate for multiple LIKE searches (case-insensitive).
     * Useful for searching across multiple fields.
     */
    public SpecificationBuilder<T> withLikeOr(String searchText, String... fields) {
        if (searchText != null && !searchText.isEmpty() && fields.length > 0) {
            String pattern = "%" + searchText.toLowerCase() + "%";
            specifications.add((root, query, cb) -> {
                Predicate[] predicates = new Predicate[fields.length];
                for (int i = 0; i < fields.length; i++) {
                    predicates[i] = cb.like(cb.lower(root.get(fields[i])), pattern);
                }
                return cb.or(predicates);
            });
        }
        return this;
    }

    /**
     * Add date range predicate (between two Instants).
     */
    public SpecificationBuilder<T> withDateRange(String field, Instant after, Instant before) {
        if (after != null) {
            withGreaterThanOrEqual(field, after);
        }
        if (before != null) {
            withLessThanOrEqual(field, before);
        }
        return this;
    }

    /**
     * Add a custom specification.
     */
    public SpecificationBuilder<T> with(Specification<T> spec) {
        if (spec != null) {
            specifications.add(spec);
        }
        return this;
    }

    /**
     * Add an OR condition between two specifications.
     */
    public SpecificationBuilder<T> withOr(Specification<T> spec1, Specification<T> spec2) {
        if (spec1 != null && spec2 != null) {
            specifications.add(spec1.or(spec2));
        }
        return this;
    }

    /**
     * Build the final Specification by ANDing all added specifications.
     */
    public Specification<T> build() {
        if (specifications.isEmpty()) {
            return (root, query, cb) -> cb.conjunction(); // Returns all records (always true)
        }

        Specification<T> result = specifications.get(0);
        for (int i = 1; i < specifications.size(); i++) {
            result = result.and(specifications.get(i));
        }
        return result;
    }
}
