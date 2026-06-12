package io.github.afgprojects.framework.data.core.condition;

import io.github.afgprojects.framework.data.core.query.Condition;
import org.jspecify.annotations.Nullable;

/**
 * 条件构建器（字符串字段名）
 * <p>
 * 提供基于字符串字段名的条件构建能力。适用于动态字段名场景
 * （如泛型查询、用户自定义查询）。
 * <p>
 * 推荐使用 {@link TypedConditionBuilder}（Lambda 方式）获取编译时字段名检查和运行时类型安全。
 * <p>
 * <b>null 值语义：</b>
 * <ul>
 *   <li>{@code eq(field, null)} → {@code IS NULL}</li>
 *   <li>{@code ne(field, null)} → {@code IS NOT NULL}</li>
 *   <li>{@code in(field, emptyList)} → 不匹配任何记录</li>
 * </ul>
 * <p>
 * <b>IfPresent 方法：</b>null/空值时跳过条件，用于动态查询场景。
 * <pre>
 * Condition condition = Conditions.builder()
 *     .eqIfPresent("status", statusParam)     // statusParam 为 null 时跳过
 *     .likeIfPresent("username", nameParam)   // nameParam 为 null 或 "" 时跳过
 *     .inIfPresent("dept_id", deptIds)        // deptIds 为 null 或空集合时跳过
 *     .build();
 * </pre>
 *
 * @see TypedConditionBuilder Lambda 字段引用条件构建器
 * @see Conditions 条件工厂类
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

    // ==================== IfPresent 操作符 ====================

    /**
     * 添加等于条件（值存在时），值不存在时跳过该条件
     * <p>
     * 与 {@link #eq(String, Object)} 不同：当 value 为 null 时，
     * {@code eq} 会转换为 IS NULL 条件，而 {@code eqIfPresent} 会直接跳过（不添加任何条件）。
     * 适用于动态查询场景，前端搜索条件可能为空时避免手动 null 判断。
     *
     * @param field 字段名
     * @param value 字段值，为 null 时跳过该条件
     * @return 条件构建器（支持链式调用）
     */
    ConditionBuilder eqIfPresent(String field, @Nullable Object value);

    /**
     * 添加不等于条件（值存在时），值不存在时跳过该条件
     * <p>
     * 与 {@link #ne(String, Object)} 不同：当 value 为 null 时，
     * {@code ne} 会转换为 IS NOT NULL 条件，而 {@code neIfPresent} 会直接跳过。
     *
     * @param field 字段名
     * @param value 字段值，为 null 时跳过该条件
     * @return 条件构建器（支持链式调用）
     */
    ConditionBuilder neIfPresent(String field, @Nullable Object value);

    /**
     * 添加 LIKE 条件（值存在时），值不存在时跳过该条件
     * <p>
     * 与 {@link #like(String, String)} 不同：当 value 为 null 或空字符串时，
     * {@code like} 会转换为 IS NULL 条件，而 {@code likeIfPresent} 会直接跳过。
     * 空字符串 {@code ""} 也视为"不存在"而跳过，与 MyBatis-Plus IfPresent 行为一致。
     *
     * @param field 字段名
     * @param value 匹配值，为 null 或空字符串时跳过该条件
     * @return 条件构建器（支持链式调用）
     */
    ConditionBuilder likeIfPresent(String field, @Nullable String value);

    /**
     * 添加前缀匹配条件（值存在时），值不存在时跳过该条件
     * <p>
     * 当 value 为 null 或空字符串时直接跳过，不添加任何条件。
     *
     * @param field 字段名
     * @param value 匹配值，为 null 或空字符串时跳过该条件
     * @return 条件构建器（支持链式调用）
     */
    ConditionBuilder likeStartsWithIfPresent(String field, @Nullable String value);

    /**
     * 添加后缀匹配条件（值存在时），值不存在时跳过该条件
     * <p>
     * 当 value 为 null 或空字符串时直接跳过，不添加任何条件。
     *
     * @param field 字段名
     * @param value 匹配值，为 null 或空字符串时跳过该条件
     * @return 条件构建器（支持链式调用）
     */
    ConditionBuilder likeEndsWithIfPresent(String field, @Nullable String value);

    /**
     * 添加 NOT LIKE 条件（值存在时），值不存在时跳过该条件
     * <p>
     * 当 value 为 null 或空字符串时直接跳过，不添加任何条件。
     *
     * @param field 字段名
     * @param value 匹配值，为 null 或空字符串时跳过该条件
     * @return 条件构建器（支持链式调用）
     */
    ConditionBuilder notLikeIfPresent(String field, @Nullable String value);

    /**
     * 添加 IN 条件（值存在时），值不存在时跳过该条件
     * <p>
     * 与 {@link #in(String, Iterable)} 不同：当 values 为 null 或空集合时，
     * {@code in} 会转换为 IS NULL 或 none()，而 {@code inIfPresent} 会直接跳过。
     * 空集合也视为"不存在"而跳过。
     *
     * @param field  字段名
     * @param values 值集合，为 null 或空集合时跳过该条件
     * @return 条件构建器（支持链式调用）
     */
    ConditionBuilder inIfPresent(String field, @Nullable Iterable<?> values);

    /**
     * 添加 NOT IN 条件（值存在时），值不存在时跳过该条件
     * <p>
     * 当 values 为 null 或空集合时直接跳过，不添加任何条件。
     *
     * @param field  字段名
     * @param values 值集合，为 null 或空集合时跳过该条件
     * @return 条件构建器（支持链式调用）
     */
    ConditionBuilder notInIfPresent(String field, @Nullable Iterable<?> values);

    /**
     * 添加 BETWEEN 条件（值存在时），值不存在时跳过该条件
     * <p>
     * from 和 to 都不为 null 时才添加条件。部分为 null 时无法构成有效 BETWEEN，直接跳过。
     *
     * @param field 字段名
     * @param from  起始值，from 和 to 都不为 null 时才添加条件
     * @param to    结束值，from 和 to 都不为 null 时才添加条件
     * @return 条件构建器（支持链式调用）
     */
    ConditionBuilder betweenIfPresent(String field, @Nullable Comparable<?> from, @Nullable Comparable<?> to);

    /**
     * 添加 NOT BETWEEN 条件（值存在时），值不存在时跳过该条件
     * <p>
     * from 和 to 都不为 null 时才添加条件。部分为 null 时无法构成有效 NOT BETWEEN，直接跳过。
     *
     * @param field 字段名
     * @param from  起始值，from 和 to 都不为 null 时才添加条件
     * @param to    结束值，from 和 to 都不为 null 时才添加条件
     * @return 条件构建器（支持链式调用）
     */
    ConditionBuilder notBetweenIfPresent(String field, @Nullable Comparable<?> from, @Nullable Comparable<?> to);

    /**
     * 添加大于条件（值存在时），值不存在时跳过该条件
     * <p>
     * 当 value 为 null 时直接跳过，不添加任何条件。
     *
     * @param field 字段名
     * @param value 比较值，为 null 时跳过该条件
     * @return 条件构建器（支持链式调用）
     */
    ConditionBuilder gtIfPresent(String field, @Nullable Comparable<?> value);

    /**
     * 添加大于等于条件（值存在时），值不存在时跳过该条件
     * <p>
     * 当 value 为 null 时直接跳过，不添加任何条件。
     *
     * @param field 字段名
     * @param value 比较值，为 null 时跳过该条件
     * @return 条件构建器（支持链式调用）
     */
    ConditionBuilder geIfPresent(String field, @Nullable Comparable<?> value);

    /**
     * 添加小于条件（值存在时），值不存在时跳过该条件
     * <p>
     * 当 value 为 null 时直接跳过，不添加任何条件。
     *
     * @param field 字段名
     * @param value 比较值，为 null 时跳过该条件
     * @return 条件构建器（支持链式调用）
     */
    ConditionBuilder ltIfPresent(String field, @Nullable Comparable<?> value);

    /**
     * 添加小于等于条件（值存在时），值不存在时跳过该条件
     * <p>
     * 当 value 为 null 时直接跳过，不添加任何条件。
     *
     * @param field 字段名
     * @param value 比较值，为 null 时跳过该条件
     * @return 条件构建器（支持链式调用）
     */
    ConditionBuilder leIfPresent(String field, @Nullable Comparable<?> value);

    ConditionBuilder and(Condition condition);
    ConditionBuilder or(Condition condition);

    Condition build();
}