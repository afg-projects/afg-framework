package io.github.afgprojects.framework.ai.core.rag;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a filter expression for vector store queries.
 * <p>
 * FilterExpression enables metadata filtering in similarity searches:
 * <ul>
 *   <li>Equality: field == value</li>
 *   <li>Comparison: field > value, field < value, etc.</li>
 *   <li>Logical: AND, OR, NOT combinations</li>
 *   <li>In: field in [value1, value2, ...]</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * // Simple equality filter
 * FilterExpression filter = FilterExpression.eq("category", "tech");
 *
 * // Comparison filter
 * FilterExpression filter = FilterExpression.gt("year", 2020);
 *
 * // Combined filters
 * FilterExpression filter = FilterExpression.and(
 *     FilterExpression.eq("category", "tech"),
 *     FilterExpression.gt("year", 2020)
 * );
 *
 * // In filter
 * FilterExpression filter = FilterExpression.in("status", "active", "pending");
 *
 * // Not filter
 * FilterExpression filter = FilterExpression.not(
 *     FilterExpression.eq("deleted", true)
 * );
 * }</pre>
 *
 * @author AFG Projects
 * @since 1.0.0
 */
public sealed interface FilterExpression {

    /**
     * Gets the field name for this filter.
     *
     * @return the field name
     */
    @NonNull
    String field();

    /**
     * Creates an equality filter.
     *
     * @param field the field name
     * @param value the value to match
     * @return a new filter expression
     */
    @NonNull
    static FilterExpression eq(@NonNull String field, @Nullable Object value) {
        return new Comparison(field, Operator.EQ, value);
    }

    /**
     * Creates a not-equal filter.
     *
     * @param field the field name
     * @param value the value to not match
     * @return a new filter expression
     */
    @NonNull
    static FilterExpression ne(@NonNull String field, @Nullable Object value) {
        return new Comparison(field, Operator.NE, value);
    }

    /**
     * Creates a greater-than filter.
     *
     * @param field the field name
     * @param value the value to compare
     * @return a new filter expression
     */
    @NonNull
    static FilterExpression gt(@NonNull String field, @NonNull Comparable<?> value) {
        return new Comparison(field, Operator.GT, value);
    }

    /**
     * Creates a greater-than-or-equal filter.
     *
     * @param field the field name
     * @param value the value to compare
     * @return a new filter expression
     */
    @NonNull
    static FilterExpression gte(@NonNull String field, @NonNull Comparable<?> value) {
        return new Comparison(field, Operator.GTE, value);
    }

    /**
     * Creates a less-than filter.
     *
     * @param field the field name
     * @param value the value to compare
     * @return a new filter expression
     */
    @NonNull
    static FilterExpression lt(@NonNull String field, @NonNull Comparable<?> value) {
        return new Comparison(field, Operator.LT, value);
    }

    /**
     * Creates a less-than-or-equal filter.
     *
     * @param field the field name
     * @param value the value to compare
     * @return a new filter expression
     */
    @NonNull
    static FilterExpression lte(@NonNull String field, @NonNull Comparable<?> value) {
        return new Comparison(field, Operator.LTE, value);
    }

    /**
     * Creates an IN filter.
     *
     * @param field  the field name
     * @param values the values to match
     * @return a new filter expression
     */
    @NonNull
    static FilterExpression in(@NonNull String field, @NonNull Object... values) {
        return new InFilter(field, List.of(values));
    }

    /**
     * Creates an IN filter with a list.
     *
     * @param field  the field name
     * @param values the values to match
     * @return a new filter expression
     */
    @NonNull
    static FilterExpression in(@NonNull String field, @NonNull List<?> values) {
        return new InFilter(field, values);
    }

    /**
     * Creates a NOT IN filter.
     *
     * @param field  the field name
     * @param values the values to not match
     * @return a new filter expression
     */
    @NonNull
    static FilterExpression notIn(@NonNull String field, @NonNull Object... values) {
        return new NotInFilter(field, List.of(values));
    }

    /**
     * Creates an AND filter combining multiple filters.
     *
     * @param filters the filters to combine
     * @return a new filter expression
     */
    @NonNull
    static FilterExpression and(@NonNull FilterExpression... filters) {
        return new AndFilter(List.of(filters));
    }

    /**
     * Creates an OR filter combining multiple filters.
     *
     * @param filters the filters to combine
     * @return a new filter expression
     */
    @NonNull
    static FilterExpression or(@NonNull FilterExpression... filters) {
        return new OrFilter(List.of(filters));
    }

    /**
     * Creates a NOT filter negating a filter.
     *
     * @param filter the filter to negate
     * @return a new filter expression
     */
    @NonNull
    static FilterExpression not(@NonNull FilterExpression filter) {
        return new NotFilter(filter);
    }

    /**
     * Filter operator types.
     */
    enum Operator {
        EQ,
        NE,
        GT,
        GTE,
        LT,
        LTE,
        IN,
        NOT_IN,
        LIKE,
        IS_NULL,
        IS_NOT_NULL
    }

    /**
     * Comparison filter (eq, ne, gt, gte, lt, lte).
     *
     * @param field    the field name
     * @param operator the comparison operator
     * @param value    the value to compare
     */
    record Comparison(
        @NonNull String field,
        @NonNull Operator operator,
        @Nullable Object value
    ) implements FilterExpression {
        public Comparison {
            if (field == null || field.isBlank()) {
                throw new IllegalArgumentException("field cannot be null or blank");
            }
            if (operator == null) {
                throw new IllegalArgumentException("operator cannot be null");
            }
        }
    }

    /**
     * IN filter.
     *
     * @param field  the field name
     * @param values the values to match
     */
    record InFilter(
        @NonNull String field,
        @NonNull List<?> values
    ) implements FilterExpression {
        public InFilter {
            if (field == null || field.isBlank()) {
                throw new IllegalArgumentException("field cannot be null or blank");
            }
            if (values == null || values.isEmpty()) {
                throw new IllegalArgumentException("values cannot be null or empty");
            }
        }
    }

    /**
     * NOT IN filter.
     *
     * @param field  the field name
     * @param values the values to not match
     */
    record NotInFilter(
        @NonNull String field,
        @NonNull List<?> values
    ) implements FilterExpression {
        public NotInFilter {
            if (field == null || field.isBlank()) {
                throw new IllegalArgumentException("field cannot be null or blank");
            }
            if (values == null || values.isEmpty()) {
                throw new IllegalArgumentException("values cannot be null or empty");
            }
        }
    }

    /**
     * AND filter combining multiple filters.
     *
     * @param filters the filters to combine
     */
    record AndFilter(@NonNull List<FilterExpression> filters) implements FilterExpression {
        public AndFilter {
            if (filters == null || filters.isEmpty()) {
                throw new IllegalArgumentException("filters cannot be null or empty");
            }
            filters = new ArrayList<>(filters); // Defensive copy
        }

        @Override
        public @NonNull String field() {
            return filters.getFirst().field();
        }
    }

    /**
     * OR filter combining multiple filters.
     *
     * @param filters the filters to combine
     */
    record OrFilter(@NonNull List<FilterExpression> filters) implements FilterExpression {
        public OrFilter {
            if (filters == null || filters.isEmpty()) {
                throw new IllegalArgumentException("filters cannot be null or empty");
            }
            filters = new ArrayList<>(filters); // Defensive copy
        }

        @Override
        public @NonNull String field() {
            return filters.getFirst().field();
        }
    }

    /**
     * NOT filter negating a filter.
     *
     * @param filter the filter to negate
     */
    record NotFilter(@NonNull FilterExpression filter) implements FilterExpression {
        public NotFilter {
            if (filter == null) {
                throw new IllegalArgumentException("filter cannot be null");
            }
        }

        @Override
        public @NonNull String field() {
            return filter.field();
        }
    }
}
