package io.github.afgprojects.framework.data.core.condition;

import io.github.afgprojects.framework.data.core.query.Condition;
import org.jspecify.annotations.Nullable;

/**
 * 条件构建器（字符串字段名）
 */
public interface ConditionBuilder {

    ConditionBuilder eq(String field, @Nullable Object value);
    ConditionBuilder ne(String field, @Nullable Object value);
    ConditionBuilder gt(String field, @Nullable Comparable<?> value);
    ConditionBuilder ge(String field, @Nullable Comparable<?> value);
    ConditionBuilder lt(String field, @Nullable Comparable<?> value);
    ConditionBuilder le(String field, @Nullable Comparable<?> value);
    ConditionBuilder like(String field, @Nullable String value);
    ConditionBuilder likeLeft(String field, @Nullable String value);
    ConditionBuilder likeRight(String field, @Nullable String value);
    ConditionBuilder notLike(String field, @Nullable String value);
    ConditionBuilder in(String field, @Nullable Iterable<?> values);
    ConditionBuilder notIn(String field, @Nullable Iterable<?> values);
    ConditionBuilder isNull(String field);
    ConditionBuilder isNotNull(String field);
    ConditionBuilder between(String field, @Nullable Comparable<?> from, @Nullable Comparable<?> to);
    ConditionBuilder notBetween(String field, @Nullable Comparable<?> from, @Nullable Comparable<?> to);

    ConditionBuilder and(Condition condition);
    ConditionBuilder or(Condition condition);

    Condition build();
}