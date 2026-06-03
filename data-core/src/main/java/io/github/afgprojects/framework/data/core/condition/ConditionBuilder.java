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
    ConditionBuilder likeStartsWith(String field, @Nullable String value);
    ConditionBuilder likeEndsWith(String field, @Nullable String value);
    ConditionBuilder notLike(String field, @Nullable String value);
    ConditionBuilder in(String field, @Nullable Iterable<?> values);
    ConditionBuilder notIn(String field, @Nullable Iterable<?> values);
    ConditionBuilder isNull(String field);
    ConditionBuilder isNotNull(String field);
    ConditionBuilder between(String field, @Nullable Comparable<?> from, @Nullable Comparable<?> to);
    ConditionBuilder notBetween(String field, @Nullable Comparable<?> from, @Nullable Comparable<?> to);

    // ==================== JSON 操作符 ====================

    /**
     * 添加 JSON CONTAINS 条件（JSON 列包含指定值）
     * <p>
     * PostgreSQL: {@code column @> ?::jsonb}<br>
     * MySQL: {@code JSON_CONTAINS(column, ?)}
     *
     * @param field     字段名
     * @param jsonValue JSON 值（字符串或对象）
     * @return 条件构建器（支持链式调用）
     */
    ConditionBuilder jsonContains(String field, @Nullable Object jsonValue);

    /**
     * 添加 JSON CONTAINED 条件（JSON 列被指定值包含）
     * <p>
     * PostgreSQL: {@code column <@ ?::jsonb}<br>
     * MySQL: {@code JSON_CONTAINS(?, column)}
     *
     * @param field     字段名
     * @param jsonValue JSON 值（字符串或对象）
     * @return 条件构建器（支持链式调用）
     */
    ConditionBuilder jsonContained(String field, @Nullable Object jsonValue);

    /**
     * 添加 JSON PATH 条件（JSON 路径存在）
     * <p>
     * PostgreSQL: {@code column ?? ?}<br>
     * MySQL: {@code JSON_EXTRACT(column, ?) IS NOT NULL}
     *
     * @param field 字段名
     * @param path  JSON 路径表达式
     * @return 条件构建器（支持链式调用）
     */
    ConditionBuilder jsonPath(String field, @Nullable String path);

    ConditionBuilder and(Condition condition);
    ConditionBuilder or(Condition condition);

    Condition build();
}