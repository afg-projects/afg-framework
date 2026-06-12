package io.github.afgprojects.framework.data.core.condition;

import io.github.afgprojects.framework.data.core.metadata.EntityMetadataCache;
import io.github.afgprojects.framework.data.core.naming.FieldNameResolver;
import io.github.afgprojects.framework.data.core.query.Condition;
import io.github.afgprojects.framework.data.core.query.ConditionImpl;
import io.github.afgprojects.framework.data.core.query.Criterion;
import io.github.afgprojects.framework.data.core.query.DenyAllCondition;
import io.github.afgprojects.framework.data.core.query.LogicalOperator;
import io.github.afgprojects.framework.data.core.query.Operator;
import io.github.afgprojects.framework.data.core.util.TypeDescriptorUtils;
import org.jspecify.annotations.Nullable;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 条件工厂类
 * <p>
 * 提供条件构建的静态工厂方法和快捷方法，是 AFG 框架条件查询的主要入口。
 * 支持两种构建方式：
 * <ul>
 *   <li><b>字符串字段名</b>：{@code Conditions.builder().eq("username", "admin").build()}</li>
 *   <li><b>Lambda 字段引用</b>：{@code Conditions.builder(User.class).eq(User::getUsername, "admin").build()}</li>
 * </ul>
 *
 * <h3>推荐使用 Lambda 方式</h3>
 * <p>
 * Lambda 方式提供编译时字段名检查和运行时类型安全，避免字段名拼写错误和类型不匹配。
 * 字符串方式仅用于动态字段名场景（如泛型查询、用户自定义查询）。
 *
 * <h3>null 值语义</h3>
 * <p>
 * 普通方法（eq、ne、like 等）对 null 值有智能转换：
 * <ul>
 *   <li>{@code eq(field, null)} → {@code IS NULL}</li>
 *   <li>{@code ne(field, null)} → {@code IS NOT NULL}</li>
 *   <li>{@code in(field, emptyList)} → {@code 1 = 0}（不匹配任何记录）</li>
 * </ul>
 *
 * <h3>IfPresent 方法</h3>
 * <p>
 * IfPresent 系列方法用于动态查询场景，null 值时跳过条件（不添加到查询中）：
 * <ul>
 *   <li>{@code eqIfPresent(field, null)} → 跳过（不添加条件）</li>
 *   <li>{@code likeIfPresent(field, "")} → 跳过（空字符串也不添加条件）</li>
 *   <li>{@code inIfPresent(field, emptyList)} → 跳过（空集合也不添加条件）</li>
 * </ul>
 *
 * <h3>组合条件</h3>
 * <p>
 * 使用 {@link #allOf} 和 {@link #anyOf} 组合多个条件：
 * <pre>
 * Condition condition = allOf(nameCondition, statusCondition);  // AND
 * Condition condition = anyOf(cond1, cond2, cond3);            // OR
 * </pre>
 *
 * @see ConditionBuilder 字符串字段名条件构建器
 * @see TypedConditionBuilder Lambda 字段引用条件构建器
 */
@SuppressWarnings("PMD.AvoidCatchingGenericException")
public final class Conditions {

    /**
     * Lambda 字段名解析缓存
     */
    private static final ConcurrentHashMap<String, String> FIELD_NAME_CACHE = new ConcurrentHashMap<>();

    /**
     * 字段名解析器（用于 Lambda → 列名转换）
     * <p>
     * 使用懒加载单例模式，允许通过 {@link #setFieldNameResolver(FieldNameResolver)} 替换。
     */
    private static volatile FieldNameResolver FIELD_NAME_RESOLVER;

    private Conditions() {}

    /**
     * 转义 LIKE 通配符
     * <p>
     * 将用户输入中的 {@code %}、{@code _} 和 {@code !} 转义为 {@code !%}、{@code !_} 和 {@code !!}，
     * 防止这些字符被当作 LIKE 通配符处理。配合 SQL 中的 {@code ESCAPE '!'} 子句使用。
     *
     * @param value 原始值
     * @return 转义后的值，如果输入为 null 则返回 null
     */
    public static String escapeLikeWildcards(@Nullable String value) {
        if (value == null) {
            return null;
        }
        StringBuilder escaped = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '%' || c == '_' || c == '!') {
                escaped.append('!');
            }
            escaped.append(c);
        }
        return escaped.toString();
    }

    /**
     * 获取字段名解析器（懒加载）
     *
     * @return 字段名解析器
     */
    private static FieldNameResolver getFieldNameResolver() {
        if (FIELD_NAME_RESOLVER == null) {
            synchronized (Conditions.class) {
                if (FIELD_NAME_RESOLVER == null) {
                    FIELD_NAME_RESOLVER = new FieldNameResolver(new EntityMetadataCache());
                }
            }
        }
        return FIELD_NAME_RESOLVER;
    }

    /**
     * 设置字段名解析器
     * <p>
     * 允许替换默认的 FieldNameResolver，用于自定义 EntityMetadataCache 配置。
     * 必须在首次使用 Conditions 之前调用。
     *
     * @param resolver 字段名解析器
     */
    public static void setFieldNameResolver(FieldNameResolver resolver) {
        synchronized (Conditions.class) {
            FIELD_NAME_RESOLVER = resolver;
        }
    }

    /**
     * 创建条件构建器
     */
    public static ConditionBuilder builder() {
        return new DefaultConditionBuilder();
    }

    /**
     * 创建类型化条件构建器
     */
    public static <T> TypedConditionBuilder<T> builder(Class<T> entityClass) {
        return new DefaultTypedConditionBuilder<>(entityClass);
    }

    /**
     * 创建空条件
     */
    public static Condition empty() {
        return Condition.empty();
    }

    /**
     * 创建匹配所有条件（WHERE 1=1）
     * <p>
     * 用于需要返回所有记录的场景
     */
    public static Condition all() {
        return builder().build();
    }

    /**
     * 创建一个永假条件（1 = 0），用于拒绝所有数据访问
     * <p>
     * 内部使用特殊的 DENY_ALL 条件类型，不会触发字段名验证。
     *
     * @return 永假条件
     */
    public static Condition none() {
        return new DenyAllCondition();
    }

    /**
     * 创建 OR 组合条件（任意一个条件匹配即可）
     * <p>
     * 等价于 {@code (condition1 OR condition2 OR ...)}
     *
     * @param conditions 要组合的条件
     * @return OR 组合后的条件
     */
    public static Condition anyOf(Condition... conditions) {
        if (conditions == null || conditions.length == 0) {
            return empty();
        }
        if (conditions.length == 1) {
            return conditions[0];
        }
        ConditionBuilder builder = builder();
        for (Condition condition : conditions) {
            builder.or(condition);
        }
        return builder.build();
    }

    /**
     * 创建 AND 组合条件（所有条件都必须匹配）
     * <p>
     * 等价于 {@code (condition1 AND condition2 AND ...)}
     *
     * @param conditions 要组合的条件
     * @return AND 组合后的条件
     */
    public static Condition allOf(Condition... conditions) {
        if (conditions == null || conditions.length == 0) {
            return empty();
        }
        if (conditions.length == 1) {
            return conditions[0];
        }
        ConditionBuilder builder = builder();
        for (Condition condition : conditions) {
            builder.and(condition);
        }
        return builder.build();
    }

    /**
     * 创建等于条件（智能处理 null 值）
     * <p>
     * 如果 value 为 null，自动转换为 IS NULL 条件
     */
    public static Condition eq(String field, @Nullable Object value) {
        if (value == null) {
            return isNull(field);
        }
        return builder().eq(field, value).build();
    }

    /**
     * 创建不等于条件（智能处理 null 值）
     * <p>
     * 如果 value 为 null，自动转换为 IS NOT NULL 条件
     */
    public static Condition ne(String field, @Nullable Object value) {
        if (value == null) {
            return isNotNull(field);
        }
        return builder().ne(field, value).build();
    }

    /**
     * 创建 LIKE 条件（包含匹配，自动转义通配符）
     * <p>
     * 生成 {@code %value%} 形式的 LIKE 条件，用户输入中的 {@code %} 和 {@code _}
     * 会被自动转义，不会作为通配符处理。
     */
    public static Condition like(String field, @Nullable String value) {
        if (value == null) {
            return isNull(field);
        }
        return builder().like(field, value).build();
    }

    /**
     * 创建前缀匹配条件（value%）
     * <p>
     * 匹配以指定值开头的字符串
     *
     * @param field 字段名
     * @param value 匹配值
     * @return LIKE 条件
     */
    public static Condition likeStartsWith(String field, @Nullable String value) {
        if (value == null) {
            return isNull(field);
        }
        return builder().likeStartsWith(field, value).build();
    }

    /**
     * 创建后缀匹配条件（%value）
     * <p>
     * 匹配以指定值结尾的字符串
     *
     * @param field 字段名
     * @param value 匹配值
     * @return LIKE 条件
     */
    public static Condition likeEndsWith(String field, @Nullable String value) {
        if (value == null) {
            return isNull(field);
        }
        return builder().likeEndsWith(field, value).build();
    }

    /**
     * 创建 IN 条件（智能处理空集合）
     * <p>
     * 如果 values 为 null，转换为 IS NULL；
     * 如果 values 为空集合，返回 none()（不匹配任何记录）
     */
    public static Condition in(String field, @Nullable Iterable<?> values) {
        if (values == null) {
            return isNull(field);
        }
        List<Object> list = new ArrayList<>();
        values.forEach(list::add);
        if (list.isEmpty()) {
            return none();
        }
        return builder().in(field, list).build();
    }

    /**
     * 创建 NOT IN 条件（智能处理空集合）
     */
    public static Condition notIn(String field, @Nullable Iterable<?> values) {
        if (values == null) {
            return isNotNull(field);
        }
        List<Object> list = new ArrayList<>();
        values.forEach(list::add);
        if (list.isEmpty()) {
            return all();
        }
        return builder().notIn(field, list).build();
    }

    /**
     * 创建 IS NULL 条件
     */
    public static Condition isNull(String field) {
        return builder().isNull(field).build();
    }

    /**
     * 创建 IS NOT NULL 条件
     */
    public static Condition isNotNull(String field) {
        return builder().isNotNull(field).build();
    }

    /**
     * 创建大于条件（&gt;）
     */
    public static Condition gt(String field, @Nullable Comparable<?> value) {
        return builder().gt(field, value).build();
    }

    /**
     * 创建大于等于条件（&gt;=）
     */
    public static Condition ge(String field, @Nullable Comparable<?> value) {
        return builder().ge(field, value).build();
    }

    /**
     * 创建小于条件（&lt;）
     */
    public static Condition lt(String field, @Nullable Comparable<?> value) {
        return builder().lt(field, value).build();
    }

    /**
     * 创建小于等于条件（&lt;=）
     */
    public static Condition le(String field, @Nullable Comparable<?> value) {
        return builder().le(field, value).build();
    }

    /**
     * 创建 NOT LIKE 条件（智能处理 null 值）
     * <p>
     * 如果 value 为 null，自动转换为 IS NOT NULL 条件
     */
    public static Condition notLike(String field, @Nullable String value) {
        if (value == null) {
            return isNotNull(field);
        }
        return builder().notLike(field, value).build();
    }

    /**
     * 创建 BETWEEN 条件
     */
    public static Condition between(String field, @Nullable Comparable<?> from, @Nullable Comparable<?> to) {
        return builder().between(field, from, to).build();
    }

    /**
     * 创建 NOT BETWEEN 条件
     */
    public static Condition notBetween(String field, @Nullable Comparable<?> from, @Nullable Comparable<?> to) {
        return builder().notBetween(field, from, to).build();
    }

    // ==================== JSON 操作符静态方法 ====================

    /**
     * 创建 JSON CONTAINS 条件（JSON 列包含指定值）
     *
     * @param field     字段名
     * @param jsonValue JSON 值
     */
    public static Condition jsonContains(String field, @Nullable Object jsonValue) {
        return builder().jsonContains(field, jsonValue).build();
    }

    /**
     * 创建 JSON CONTAINED 条件（JSON 列被指定值包含）
     *
     * @param field     字段名
     * @param jsonValue JSON 值
     */
    public static Condition jsonContained(String field, @Nullable Object jsonValue) {
        return builder().jsonContained(field, jsonValue).build();
    }

    /**
     * 创建 JSON PATH 条件（JSON 路径存在）
     * <p>
     * 如果 path 为 null，自动转换为 IS NULL 条件
     *
     * @param field 字段名
     * @param path  JSON 路径表达式
     */
    public static Condition jsonPath(String field, @Nullable String path) {
        return builder().jsonPath(field, path).build();
    }

    // ==================== IfPresent 静态方法（字符串字段名） ====================

    /**
     * 创建等于条件（值存在时），值不存在时返回空条件
     * <p>
     * 当 value 为 null 时，跳过该条件（返回空条件），而非转换为 IS NULL。
     * 适用于动态查询场景，前端搜索条件可能为空时避免手动 null 判断。
     *
     * @param field 字段名
     * @param value 字段值，为 null 时跳过
     * @return 等于条件或空条件
     */
    public static Condition eqIfPresent(String field, @Nullable Object value) {
        if (value == null) {
            return empty();
        }
        return builder().eq(field, value).build();
    }

    /**
     * 创建不等于条件（值存在时），值不存在时返回空条件
     *
     * @param field 字段名
     * @param value 字段值，为 null 时跳过
     * @return 不等于条件或空条件
     */
    public static Condition neIfPresent(String field, @Nullable Object value) {
        if (value == null) {
            return empty();
        }
        return builder().ne(field, value).build();
    }

    /**
     * 创建 LIKE 条件（值存在时），值不存在时返回空条件
     * <p>
     * 当 value 为 null 或空字符串时跳过，不添加任何条件。
     * 空字符串 {@code ""} 也视为"不存在"而跳过。
     *
     * @param field 字段名
     * @param value 匹配值，为 null 或空字符串时跳过
     * @return LIKE 条件或空条件
     */
    public static Condition likeIfPresent(String field, @Nullable String value) {
        if (value == null || value.isEmpty()) {
            return empty();
        }
        return builder().like(field, value).build();
    }

    /**
     * 创建前缀匹配条件（值存在时），值不存在时返回空条件
     * <p>
     * 当 value 为 null 或空字符串时跳过。
     *
     * @param field 字段名
     * @param value 匹配值，为 null 或空字符串时跳过
     * @return LIKE 条件或空条件
     */
    public static Condition likeStartsWithIfPresent(String field, @Nullable String value) {
        if (value == null || value.isEmpty()) {
            return empty();
        }
        return builder().likeStartsWith(field, value).build();
    }

    /**
     * 创建后缀匹配条件（值存在时），值不存在时返回空条件
     * <p>
     * 当 value 为 null 或空字符串时跳过。
     *
     * @param field 字段名
     * @param value 匹配值，为 null 或空字符串时跳过
     * @return LIKE 条件或空条件
     */
    public static Condition likeEndsWithIfPresent(String field, @Nullable String value) {
        if (value == null || value.isEmpty()) {
            return empty();
        }
        return builder().likeEndsWith(field, value).build();
    }

    /**
     * 创建 NOT LIKE 条件（值存在时），值不存在时返回空条件
     * <p>
     * 当 value 为 null 或空字符串时跳过。
     *
     * @param field 字段名
     * @param value 匹配值，为 null 或空字符串时跳过
     * @return NOT LIKE 条件或空条件
     */
    public static Condition notLikeIfPresent(String field, @Nullable String value) {
        if (value == null || value.isEmpty()) {
            return empty();
        }
        return builder().notLike(field, value).build();
    }

    /**
     * 创建 IN 条件（值存在时），值不存在时返回空条件
     * <p>
     * 当 values 为 null 或空集合时跳过。空集合也视为"不存在"而跳过，
     * 与 {@link #in(String, Iterable)} 的空集合返回 none() 语义不同。
     *
     * @param field  字段名
     * @param values 值集合，为 null 或空集合时跳过
     * @return IN 条件或空条件
     */
    public static Condition inIfPresent(String field, @Nullable Iterable<?> values) {
        if (values == null) {
            return empty();
        }
        List<Object> list = new ArrayList<>();
        values.forEach(list::add);
        if (list.isEmpty()) {
            return empty();
        }
        return builder().in(field, list).build();
    }

    /**
     * 创建 NOT IN 条件（值存在时），值不存在时返回空条件
     * <p>
     * 当 values 为 null 或空集合时跳过。
     *
     * @param field  字段名
     * @param values 值集合，为 null 或空集合时跳过
     * @return NOT IN 条件或空条件
     */
    public static Condition notInIfPresent(String field, @Nullable Iterable<?> values) {
        if (values == null) {
            return empty();
        }
        List<Object> list = new ArrayList<>();
        values.forEach(list::add);
        if (list.isEmpty()) {
            return empty();
        }
        return builder().notIn(field, list).build();
    }

    /**
     * 创建 BETWEEN 条件（值存在时），值不存在时返回空条件
     * <p>
     * from 和 to 都不为 null 时才创建条件。部分为 null 时无法构成有效 BETWEEN，直接跳过。
     *
     * @param field 字段名
     * @param from  起始值
     * @param to    结束值
     * @return BETWEEN 条件或空条件
     */
    public static Condition betweenIfPresent(String field, @Nullable Comparable<?> from, @Nullable Comparable<?> to) {
        if (from == null || to == null) {
            return empty();
        }
        return builder().between(field, from, to).build();
    }

    /**
     * 创建 NOT BETWEEN 条件（值存在时），值不存在时返回空条件
     * <p>
     * from 和 to 都不为 null 时才创建条件。部分为 null 时无法构成有效 NOT BETWEEN，直接跳过。
     *
     * @param field 字段名
     * @param from  起始值
     * @param to    结束值
     * @return NOT BETWEEN 条件或空条件
     */
    public static Condition notBetweenIfPresent(String field, @Nullable Comparable<?> from, @Nullable Comparable<?> to) {
        if (from == null || to == null) {
            return empty();
        }
        return builder().notBetween(field, from, to).build();
    }

    /**
     * 创建大于条件（值存在时），值不存在时返回空条件
     *
     * @param field 字段名
     * @param value 比较值，为 null 时跳过
     * @return 大于条件或空条件
     */
    public static Condition gtIfPresent(String field, @Nullable Comparable<?> value) {
        if (value == null) {
            return empty();
        }
        return builder().gt(field, value).build();
    }

    /**
     * 创建大于等于条件（值存在时），值不存在时返回空条件
     *
     * @param field 字段名
     * @param value 比较值，为 null 时跳过
     * @return 大于等于条件或空条件
     */
    public static Condition geIfPresent(String field, @Nullable Comparable<?> value) {
        if (value == null) {
            return empty();
        }
        return builder().ge(field, value).build();
    }

    /**
     * 创建小于条件（值存在时），值不存在时返回空条件
     *
     * @param field 字段名
     * @param value 比较值，为 null 时跳过
     * @return 小于条件或空条件
     */
    public static Condition ltIfPresent(String field, @Nullable Comparable<?> value) {
        if (value == null) {
            return empty();
        }
        return builder().lt(field, value).build();
    }

    /**
     * 创建小于等于条件（值存在时），值不存在时返回空条件
     *
     * @param field 字段名
     * @param value 比较值，为 null 时跳过
     * @return 小于等于条件或空条件
     */
    public static Condition leIfPresent(String field, @Nullable Comparable<?> value) {
        if (value == null) {
            return empty();
        }
        return builder().le(field, value).build();
    }

    // ==================== 类型化静态方法 ====================

    /**
     * 创建类型化等于条件（智能处理 null 值）
     */
    public static <T, R> Condition eq(Class<T> entityClass, SFunction<T, R> getter, @Nullable Object value) {
        if (value == null) {
            return isNull(entityClass, getter);
        }
        return builder(entityClass).eq(getter, value).build();
    }

    /**
     * 创建类型化不等于条件（智能处理 null 值）
     */
    public static <T, R> Condition ne(Class<T> entityClass, SFunction<T, R> getter, @Nullable Object value) {
        if (value == null) {
            return isNotNull(entityClass, getter);
        }
        return builder(entityClass).ne(getter, value).build();
    }

    /**
     * 创建类型化 LIKE 条件（包含匹配，自动转义通配符）
     * <p>
     * 生成 {@code %value%} 形式的 LIKE 条件，用户输入中的 {@code %} 和 {@code _}
     * 会被自动转义，不会作为通配符处理。
     */
    public static <T> Condition like(Class<T> entityClass, SFunction<T, String> getter, @Nullable String value) {
        if (value == null) {
            return isNull(entityClass, getter);
        }
        return builder(entityClass).like(getter, value).build();
    }

    /**
     * 创建类型化前缀匹配条件（value%）
     * <p>
     * 匹配以指定值开头的字符串
     *
     * @param entityClass 实体类
     * @param getter      Lambda 方法引用
     * @param value       匹配值
     * @return LIKE 条件
     */
    public static <T> Condition likeStartsWith(Class<T> entityClass, SFunction<T, String> getter, @Nullable String value) {
        if (value == null) {
            return isNull(entityClass, getter);
        }
        return builder(entityClass).likeStartsWith(getter, value).build();
    }

    /**
     * 创建类型化后缀匹配条件（%value）
     * <p>
     * 匹配以指定值结尾的字符串
     *
     * @param entityClass 实体类
     * @param getter      Lambda 方法引用
     * @param value       匹配值
     * @return LIKE 条件
     */
    public static <T> Condition likeEndsWith(Class<T> entityClass, SFunction<T, String> getter, @Nullable String value) {
        if (value == null) {
            return isNull(entityClass, getter);
        }
        return builder(entityClass).likeEndsWith(getter, value).build();
    }

    /**
     * 创建类型化 IN 条件（智能处理空集合）
     */
    public static <T, R> Condition in(Class<T> entityClass, SFunction<T, R> getter, @Nullable Iterable<?> values) {
        if (values == null) {
            return isNull(entityClass, getter);
        }
        List<Object> list = new ArrayList<>();
        values.forEach(list::add);
        if (list.isEmpty()) {
            return none();
        }
        return builder(entityClass).in(getter, list).build();
    }

    /**
     * 创建类型化 NOT IN 条件（智能处理空集合）
     */
    public static <T, R> Condition notIn(Class<T> entityClass, SFunction<T, R> getter, @Nullable Iterable<?> values) {
        if (values == null) {
            return isNotNull(entityClass, getter);
        }
        List<Object> list = new ArrayList<>();
        values.forEach(list::add);
        if (list.isEmpty()) {
            return all();
        }
        return builder(entityClass).notIn(getter, list).build();
    }

    /**
     * 创建类型化 IS NULL 条件
     */
    public static <T, R> Condition isNull(Class<T> entityClass, SFunction<T, R> getter) {
        return builder(entityClass).isNull(getter).build();
    }

    /**
     * 创建类型化 IS NOT NULL 条件
     */
    public static <T, R> Condition isNotNull(Class<T> entityClass, SFunction<T, R> getter) {
        return builder(entityClass).isNotNull(getter).build();
    }

    /**
     * 创建类型化大于条件（&gt;）
     */
    public static <T, R extends Comparable<?>> Condition gt(Class<T> entityClass, SFunction<T, R> getter, @Nullable R value) {
        return builder(entityClass).gt(getter, value).build();
    }

    /**
     * 创建类型化大于等于条件（&gt;=）
     */
    public static <T, R extends Comparable<?>> Condition ge(Class<T> entityClass, SFunction<T, R> getter, @Nullable R value) {
        return builder(entityClass).ge(getter, value).build();
    }

    /**
     * 创建类型化小于条件（&lt;）
     */
    public static <T, R extends Comparable<?>> Condition lt(Class<T> entityClass, SFunction<T, R> getter, @Nullable R value) {
        return builder(entityClass).lt(getter, value).build();
    }

    /**
     * 创建类型化小于等于条件（&lt;=）
     */
    public static <T, R extends Comparable<?>> Condition le(Class<T> entityClass, SFunction<T, R> getter, @Nullable R value) {
        return builder(entityClass).le(getter, value).build();
    }

    /**
     * 创建类型化 NOT LIKE 条件（智能处理 null 值）
     * <p>
     * 如果 value 为 null，自动转换为 IS NOT NULL 条件
     */
    public static <T> Condition notLike(Class<T> entityClass, SFunction<T, String> getter, @Nullable String value) {
        if (value == null) {
            return isNotNull(entityClass, getter);
        }
        return builder(entityClass).notLike(getter, value).build();
    }

    /**
     * 创建类型化 BETWEEN 条件
     */
    public static <T, R extends Comparable<?>> Condition between(Class<T> entityClass, SFunction<T, R> getter, @Nullable R from, @Nullable R to) {
        return builder(entityClass).between(getter, from, to).build();
    }

    /**
     * 创建类型化 NOT BETWEEN 条件
     */
    public static <T, R extends Comparable<?>> Condition notBetween(Class<T> entityClass, SFunction<T, R> getter, @Nullable R from, @Nullable R to) {
        return builder(entityClass).notBetween(getter, from, to).build();
    }

    // ==================== JSON 操作符类型化静态方法 ====================

    /**
     * 创建类型化 JSON CONTAINS 条件
     *
     * @param entityClass 实体类
     * @param getter      字段 getter 方法引用
     * @param jsonValue   JSON 值
     */
    public static <T, R> Condition jsonContains(Class<T> entityClass, SFunction<T, R> getter, @Nullable Object jsonValue) {
        return builder(entityClass).jsonContains(getter, jsonValue).build();
    }

    /**
     * 创建类型化 JSON CONTAINED 条件
     *
     * @param entityClass 实体类
     * @param getter      字段 getter 方法引用
     * @param jsonValue   JSON 值
     */
    public static <T, R> Condition jsonContained(Class<T> entityClass, SFunction<T, R> getter, @Nullable Object jsonValue) {
        return builder(entityClass).jsonContained(getter, jsonValue).build();
    }

    /**
     * 创建类型化 JSON PATH 条件
     *
     * @param entityClass 实体类
     * @param getter      字段 getter 方法引用
     * @param path        JSON 路径表达式
     */
    public static <T> Condition jsonPath(Class<T> entityClass, SFunction<T, String> getter, @Nullable String path) {
        return builder(entityClass).jsonPath(getter, path).build();
    }

    // ==================== IfPresent 类型化静态方法 ====================

    /**
     * 创建类型化等于条件（值存在时），值不存在时返回空条件
     *
     * @param entityClass 实体类
     * @param getter      Lambda 方法引用
     * @param value       字段值，为 null 时跳过
     * @return 等于条件或空条件
     */
    public static <T, R> Condition eqIfPresent(Class<T> entityClass, SFunction<T, R> getter, @Nullable Object value) {
        if (value == null) {
            return empty();
        }
        return builder(entityClass).eq(getter, value).build();
    }

    /**
     * 创建类型化不等于条件（值存在时），值不存在时返回空条件
     *
     * @param entityClass 实体类
     * @param getter      Lambda 方法引用
     * @param value       字段值，为 null 时跳过
     * @return 不等于条件或空条件
     */
    public static <T, R> Condition neIfPresent(Class<T> entityClass, SFunction<T, R> getter, @Nullable Object value) {
        if (value == null) {
            return empty();
        }
        return builder(entityClass).ne(getter, value).build();
    }

    /**
     * 创建类型化 LIKE 条件（值存在时），值不存在时返回空条件
     * <p>
     * 当 value 为 null 或空字符串时跳过。
     *
     * @param entityClass 实体类
     * @param getter      Lambda 方法引用
     * @param value       匹配值，为 null 或空字符串时跳过
     * @return LIKE 条件或空条件
     */
    public static <T> Condition likeIfPresent(Class<T> entityClass, SFunction<T, String> getter, @Nullable String value) {
        if (value == null || value.isEmpty()) {
            return empty();
        }
        return builder(entityClass).like(getter, value).build();
    }

    /**
     * 创建类型化前缀匹配条件（值存在时），值不存在时返回空条件
     * <p>
     * 当 value 为 null 或空字符串时跳过。
     *
     * @param entityClass 实体类
     * @param getter      Lambda 方法引用
     * @param value       匹配值，为 null 或空字符串时跳过
     * @return LIKE 条件或空条件
     */
    public static <T> Condition likeStartsWithIfPresent(Class<T> entityClass, SFunction<T, String> getter, @Nullable String value) {
        if (value == null || value.isEmpty()) {
            return empty();
        }
        return builder(entityClass).likeStartsWith(getter, value).build();
    }

    /**
     * 创建类型化后缀匹配条件（值存在时），值不存在时返回空条件
     * <p>
     * 当 value 为 null 或空字符串时跳过。
     *
     * @param entityClass 实体类
     * @param getter      Lambda 方法引用
     * @param value       匹配值，为 null 或空字符串时跳过
     * @return LIKE 条件或空条件
     */
    public static <T> Condition likeEndsWithIfPresent(Class<T> entityClass, SFunction<T, String> getter, @Nullable String value) {
        if (value == null || value.isEmpty()) {
            return empty();
        }
        return builder(entityClass).likeEndsWith(getter, value).build();
    }

    /**
     * 创建类型化 NOT LIKE 条件（值存在时），值不存在时返回空条件
     * <p>
     * 当 value 为 null 或空字符串时跳过。
     *
     * @param entityClass 实体类
     * @param getter      Lambda 方法引用
     * @param value       匹配值，为 null 或空字符串时跳过
     * @return NOT LIKE 条件或空条件
     */
    public static <T> Condition notLikeIfPresent(Class<T> entityClass, SFunction<T, String> getter, @Nullable String value) {
        if (value == null || value.isEmpty()) {
            return empty();
        }
        return builder(entityClass).notLike(getter, value).build();
    }

    /**
     * 创建类型化 IN 条件（值存在时），值不存在时返回空条件
     * <p>
     * 当 values 为 null 或空集合时跳过。
     *
     * @param entityClass 实体类
     * @param getter      Lambda 方法引用
     * @param values      值集合，为 null 或空集合时跳过
     * @return IN 条件或空条件
     */
    public static <T, R> Condition inIfPresent(Class<T> entityClass, SFunction<T, R> getter, @Nullable Iterable<?> values) {
        if (values == null) {
            return empty();
        }
        List<Object> list = new ArrayList<>();
        values.forEach(list::add);
        if (list.isEmpty()) {
            return empty();
        }
        return builder(entityClass).in(getter, list).build();
    }

    /**
     * 创建类型化 NOT IN 条件（值存在时），值不存在时返回空条件
     * <p>
     * 当 values 为 null 或空集合时跳过。
     *
     * @param entityClass 实体类
     * @param getter      Lambda 方法引用
     * @param values      值集合，为 null 或空集合时跳过
     * @return NOT IN 条件或空条件
     */
    public static <T, R> Condition notInIfPresent(Class<T> entityClass, SFunction<T, R> getter, @Nullable Iterable<?> values) {
        if (values == null) {
            return empty();
        }
        List<Object> list = new ArrayList<>();
        values.forEach(list::add);
        if (list.isEmpty()) {
            return empty();
        }
        return builder(entityClass).notIn(getter, list).build();
    }

    /**
     * 创建类型化 BETWEEN 条件（值存在时），值不存在时返回空条件
     * <p>
     * from 和 to 都不为 null 时才创建条件。
     *
     * @param entityClass 实体类
     * @param getter      Lambda 方法引用
     * @param from        起始值
     * @param to          结束值
     * @return BETWEEN 条件或空条件
     */
    public static <T, R extends Comparable<?>> Condition betweenIfPresent(Class<T> entityClass, SFunction<T, R> getter, @Nullable R from, @Nullable R to) {
        if (from == null || to == null) {
            return empty();
        }
        return builder(entityClass).between(getter, from, to).build();
    }

    /**
     * 创建类型化 NOT BETWEEN 条件（值存在时），值不存在时返回空条件
     * <p>
     * from 和 to 都不为 null 时才创建条件。
     *
     * @param entityClass 实体类
     * @param getter      Lambda 方法引用
     * @param from        起始值
     * @param to          结束值
     * @return NOT BETWEEN 条件或空条件
     */
    public static <T, R extends Comparable<?>> Condition notBetweenIfPresent(Class<T> entityClass, SFunction<T, R> getter, @Nullable R from, @Nullable R to) {
        if (from == null || to == null) {
            return empty();
        }
        return builder(entityClass).notBetween(getter, from, to).build();
    }

    /**
     * 创建类型化大于条件（值存在时），值不存在时返回空条件
     *
     * @param entityClass 实体类
     * @param getter      Lambda 方法引用
     * @param value       比较值，为 null 时跳过
     * @return 大于条件或空条件
     */
    public static <T, R extends Comparable<?>> Condition gtIfPresent(Class<T> entityClass, SFunction<T, R> getter, @Nullable R value) {
        if (value == null) {
            return empty();
        }
        return builder(entityClass).gt(getter, value).build();
    }

    /**
     * 创建类型化大于等于条件（值存在时），值不存在时返回空条件
     *
     * @param entityClass 实体类
     * @param getter      Lambda 方法引用
     * @param value       比较值，为 null 时跳过
     * @return 大于等于条件或空条件
     */
    public static <T, R extends Comparable<?>> Condition geIfPresent(Class<T> entityClass, SFunction<T, R> getter, @Nullable R value) {
        if (value == null) {
            return empty();
        }
        return builder(entityClass).ge(getter, value).build();
    }

    /**
     * 创建类型化小于条件（值存在时），值不存在时返回空条件
     *
     * @param entityClass 实体类
     * @param getter      Lambda 方法引用
     * @param value       比较值，为 null 时跳过
     * @return 小于条件或空条件
     */
    public static <T, R extends Comparable<?>> Condition ltIfPresent(Class<T> entityClass, SFunction<T, R> getter, @Nullable R value) {
        if (value == null) {
            return empty();
        }
        return builder(entityClass).lt(getter, value).build();
    }

    /**
     * 创建类型化小于等于条件（值存在时），值不存在时返回空条件
     *
     * @param entityClass 实体类
     * @param getter      Lambda 方法引用
     * @param value       比较值，为 null 时跳过
     * @return 小于等于条件或空条件
     */
    public static <T, R extends Comparable<?>> Condition leIfPresent(Class<T> entityClass, SFunction<T, R> getter, @Nullable R value) {
        if (value == null) {
            return empty();
        }
        return builder(entityClass).le(getter, value).build();
    }

    /**
     * 从 Lambda 获取字段名（带缓存）
     */
    public static <T, R> String getFieldName(SFunction<T, R> getter) {
        String cacheKey = getter.getClass().getName();
        return FIELD_NAME_CACHE.computeIfAbsent(cacheKey, k -> doGetFieldName(getter));
    }

    /**
     * 实际解析 Lambda 字段名
     */
    private static <T, R> String doGetFieldName(SFunction<T, R> getter) {
        try {
            Method writeReplace = getter.getClass().getDeclaredMethod("writeReplace");
            writeReplace.setAccessible(true);
            SerializedLambda lambda = (SerializedLambda) writeReplace.invoke(getter);
            String methodName = lambda.getImplMethodName();
            if (methodName.startsWith("get")) {
                return Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
            } else if (methodName.startsWith("is")) {
                return Character.toLowerCase(methodName.charAt(2)) + methodName.substring(3);
            }
            return methodName;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to get field name from lambda", e);
        }
    }

    /**
     * 默认条件构建器实现
     */
    private static final class DefaultConditionBuilder implements ConditionBuilder {
        private final List<Criterion> criteria = new ArrayList<>();
        private LogicalOperator operator = LogicalOperator.AND;

        @Override
        public ConditionBuilder eq(String field, @Nullable Object value) {
            criteria.add(new Criterion(field, Operator.EQ, value, null, null));
            return this;
        }

        @Override
        public ConditionBuilder ne(String field, @Nullable Object value) {
            criteria.add(new Criterion(field, Operator.NE, value, null, null));
            return this;
        }

        @Override
        public ConditionBuilder gt(String field, @Nullable Comparable<?> value) {
            criteria.add(new Criterion(field, Operator.GT, value, null, null));
            return this;
        }

        @Override
        public ConditionBuilder ge(String field, @Nullable Comparable<?> value) {
            criteria.add(new Criterion(field, Operator.GE, value, null, null));
            return this;
        }

        @Override
        public ConditionBuilder lt(String field, @Nullable Comparable<?> value) {
            criteria.add(new Criterion(field, Operator.LT, value, null, null));
            return this;
        }

        @Override
        public ConditionBuilder le(String field, @Nullable Comparable<?> value) {
            criteria.add(new Criterion(field, Operator.LE, value, null, null));
            return this;
        }

        @Override
        public ConditionBuilder like(String field, @Nullable String value) {
            String v = value;
            if (v != null) {
                v = "%" + escapeLikeWildcards(v) + "%";
            }
            criteria.add(new Criterion(field, Operator.LIKE, v, null, null));
            return this;
        }

        @Override
        public ConditionBuilder likeStartsWith(String field, @Nullable String value) {
            String v = value;
            if (v != null) {
                v = escapeLikeWildcards(v) + "%";
            }
            criteria.add(new Criterion(field, Operator.LIKE_STARTS_WITH, v, null, null));
            return this;
        }

        @Override
        public ConditionBuilder likeEndsWith(String field, @Nullable String value) {
            String v = value;
            if (v != null) {
                v = "%" + escapeLikeWildcards(v);
            }
            criteria.add(new Criterion(field, Operator.LIKE_ENDS_WITH, v, null, null));
            return this;
        }

        @Override
        public ConditionBuilder notLike(String field, @Nullable String value) {
            String v = value;
            if (v != null) {
                v = "%" + escapeLikeWildcards(v) + "%";
            }
            criteria.add(new Criterion(field, Operator.NOT_LIKE, v, null, null));
            return this;
        }

        @Override
        public ConditionBuilder in(String field, @Nullable Iterable<?> values) {
            criteria.add(new Criterion(field, Operator.IN, values, null, null));
            return this;
        }

        @Override
        public ConditionBuilder notIn(String field, @Nullable Iterable<?> values) {
            criteria.add(new Criterion(field, Operator.NOT_IN, values, null, null));
            return this;
        }

        @Override
        public ConditionBuilder isNull(String field) {
            criteria.add(new Criterion(field, Operator.IS_NULL, null, null, null));
            return this;
        }

        @Override
        public ConditionBuilder isNotNull(String field) {
            criteria.add(new Criterion(field, Operator.IS_NOT_NULL, null, null, null));
            return this;
        }

        @Override
        public ConditionBuilder between(String field, @Nullable Comparable<?> from, @Nullable Comparable<?> to) {
            criteria.add(new Criterion(field, Operator.BETWEEN, new Comparable<?>[]{from, to}, null, null));
            return this;
        }

        @Override
        public ConditionBuilder notBetween(String field, @Nullable Comparable<?> from, @Nullable Comparable<?> to) {
            criteria.add(new Criterion(field, Operator.NOT_BETWEEN, new Comparable<?>[]{from, to}, null, null));
            return this;
        }

        @Override
        public ConditionBuilder jsonContains(String field, @Nullable Object jsonValue) {
            criteria.add(new Criterion(field, Operator.JSON_CONTAINS, jsonValue, null, null));
            return this;
        }

        @Override
        public ConditionBuilder jsonContained(String field, @Nullable Object jsonValue) {
            criteria.add(new Criterion(field, Operator.JSON_CONTAINED, jsonValue, null, null));
            return this;
        }

        @Override
        public ConditionBuilder jsonPath(String field, @Nullable String path) {
            if (path == null) {
                criteria.add(new Criterion(field, Operator.IS_NULL, null, null, null));
            } else {
                criteria.add(new Criterion(field, Operator.JSON_PATH, path, null, null));
            }
            return this;
        }

        // ==================== IfPresent 操作符实现 ====================

        @Override
        public ConditionBuilder eqIfPresent(String field, @Nullable Object value) {
            if (value == null) {
                return this;
            }
            return eq(field, value);
        }

        @Override
        public ConditionBuilder neIfPresent(String field, @Nullable Object value) {
            if (value == null) {
                return this;
            }
            return ne(field, value);
        }

        @Override
        public ConditionBuilder likeIfPresent(String field, @Nullable String value) {
            if (value == null || value.isEmpty()) {
                return this;
            }
            return like(field, value);
        }

        @Override
        public ConditionBuilder likeStartsWithIfPresent(String field, @Nullable String value) {
            if (value == null || value.isEmpty()) {
                return this;
            }
            return likeStartsWith(field, value);
        }

        @Override
        public ConditionBuilder likeEndsWithIfPresent(String field, @Nullable String value) {
            if (value == null || value.isEmpty()) {
                return this;
            }
            return likeEndsWith(field, value);
        }

        @Override
        public ConditionBuilder notLikeIfPresent(String field, @Nullable String value) {
            if (value == null || value.isEmpty()) {
                return this;
            }
            return notLike(field, value);
        }

        @Override
        public ConditionBuilder inIfPresent(String field, @Nullable Iterable<?> values) {
            if (values == null) {
                return this;
            }
            List<Object> list = new ArrayList<>();
            values.forEach(list::add);
            if (list.isEmpty()) {
                return this;
            }
            return in(field, list);
        }

        @Override
        public ConditionBuilder notInIfPresent(String field, @Nullable Iterable<?> values) {
            if (values == null) {
                return this;
            }
            List<Object> list = new ArrayList<>();
            values.forEach(list::add);
            if (list.isEmpty()) {
                return this;
            }
            return notIn(field, list);
        }

        @Override
        public ConditionBuilder betweenIfPresent(String field, @Nullable Comparable<?> from, @Nullable Comparable<?> to) {
            if (from == null || to == null) {
                return this;
            }
            return between(field, from, to);
        }

        @Override
        public ConditionBuilder notBetweenIfPresent(String field, @Nullable Comparable<?> from, @Nullable Comparable<?> to) {
            if (from == null || to == null) {
                return this;
            }
            return notBetween(field, from, to);
        }

        @Override
        public ConditionBuilder gtIfPresent(String field, @Nullable Comparable<?> value) {
            if (value == null) {
                return this;
            }
            return gt(field, value);
        }

        @Override
        public ConditionBuilder geIfPresent(String field, @Nullable Comparable<?> value) {
            if (value == null) {
                return this;
            }
            return ge(field, value);
        }

        @Override
        public ConditionBuilder ltIfPresent(String field, @Nullable Comparable<?> value) {
            if (value == null) {
                return this;
            }
            return lt(field, value);
        }

        @Override
        public ConditionBuilder leIfPresent(String field, @Nullable Comparable<?> value) {
            if (value == null) {
                return this;
            }
            return le(field, value);
        }

        @Override
        public ConditionBuilder and(Condition condition) {
            if (!condition.isEmpty()) {
                criteria.add(Criterion.nested(condition, LogicalOperator.AND));
            }
            return this;
        }

        @Override
        public ConditionBuilder or(Condition condition) {
            if (!condition.isEmpty()) {
                criteria.add(Criterion.nested(condition, LogicalOperator.OR));
            }
            return this;
        }

        @Override
        public Condition build() {
            if (criteria.isEmpty()) {
                return Condition.empty();
            }
            // 设置最后一个条件的 nextOperator 为 null
            List<Criterion> finalCriteria = new ArrayList<>();
            for (int i = 0; i < criteria.size(); i++) {
                Criterion c = criteria.get(i);
                if (i < criteria.size() - 1) {
                    if (c.nextOperator() == null) {
                        finalCriteria.add(new Criterion(c.field(), c.operator(), c.value(), operator, c.nestedCondition()));
                    } else {
                        finalCriteria.add(c);
                    }
                } else {
                    finalCriteria.add(c);
                }
            }
            return new ConditionImpl(operator, finalCriteria);
        }
    }

    /**
     * 默认类型化条件构建器实现
     */
    private static final class DefaultTypedConditionBuilder<T> implements TypedConditionBuilder<T> {
        private final Class<T> entityClass;
        private final DefaultConditionBuilder delegate = new DefaultConditionBuilder();

        DefaultTypedConditionBuilder(Class<T> entityClass) {
            this.entityClass = entityClass;
        }

        @Override
        public <R> TypedConditionBuilder<T> eq(SFunction<T, R> getter, @Nullable Object value) {
            String fieldName = getFieldName(getter);
            Class<?> fieldType = io.github.afgprojects.framework.data.core.util.TypeDescriptorUtils.resolveFieldTypeFromLambda(getter);
            TypedConditionBuilder.checkFieldType(fieldName, fieldType, value);
            delegate.eq(resolveColumnName(getter), value);
            return this;
        }

        @Override
        public <R> TypedConditionBuilder<T> ne(SFunction<T, R> getter, @Nullable Object value) {
            String fieldName = getFieldName(getter);
            Class<?> fieldType = io.github.afgprojects.framework.data.core.util.TypeDescriptorUtils.resolveFieldTypeFromLambda(getter);
            TypedConditionBuilder.checkFieldType(fieldName, fieldType, value);
            delegate.ne(resolveColumnName(getter), value);
            return this;
        }

        @Override
        public <R extends Comparable<?>> TypedConditionBuilder<T> gt(SFunction<T, R> getter, @Nullable R value) {
            delegate.gt(resolveColumnName(getter), value);
            return this;
        }

        @Override
        public <R extends Comparable<?>> TypedConditionBuilder<T> ge(SFunction<T, R> getter, @Nullable R value) {
            delegate.ge(resolveColumnName(getter), value);
            return this;
        }

        @Override
        public <R extends Comparable<?>> TypedConditionBuilder<T> lt(SFunction<T, R> getter, @Nullable R value) {
            delegate.lt(resolveColumnName(getter), value);
            return this;
        }

        @Override
        public <R extends Comparable<?>> TypedConditionBuilder<T> le(SFunction<T, R> getter, @Nullable R value) {
            delegate.le(resolveColumnName(getter), value);
            return this;
        }

        @Override
        public TypedConditionBuilder<T> like(SFunction<T, String> getter, @Nullable String value) {
            delegate.like(resolveColumnName(getter), value);
            return this;
        }

        @Override
        public TypedConditionBuilder<T> likeStartsWith(SFunction<T, String> getter, @Nullable String value) {
            delegate.likeStartsWith(resolveColumnName(getter), value);
            return this;
        }

        @Override
        public TypedConditionBuilder<T> likeEndsWith(SFunction<T, String> getter, @Nullable String value) {
            delegate.likeEndsWith(resolveColumnName(getter), value);
            return this;
        }

        @Override
        public TypedConditionBuilder<T> notLike(SFunction<T, String> getter, @Nullable String value) {
            delegate.notLike(resolveColumnName(getter), value);
            return this;
        }

        @Override
        public <R> TypedConditionBuilder<T> in(SFunction<T, R> getter, @Nullable Iterable<?> values) {
            delegate.in(resolveColumnName(getter), values);
            return this;
        }

        @Override
        public <R> TypedConditionBuilder<T> notIn(SFunction<T, R> getter, @Nullable Iterable<?> values) {
            delegate.notIn(resolveColumnName(getter), values);
            return this;
        }

        @Override
        public <R> TypedConditionBuilder<T> isNull(SFunction<T, R> getter) {
            delegate.isNull(resolveColumnName(getter));
            return this;
        }

        @Override
        public <R> TypedConditionBuilder<T> isNotNull(SFunction<T, R> getter) {
            delegate.isNotNull(resolveColumnName(getter));
            return this;
        }

        @Override
        public <R extends Comparable<?>> TypedConditionBuilder<T> between(SFunction<T, R> getter, @Nullable R from, @Nullable R to) {
            delegate.between(resolveColumnName(getter), from, to);
            return this;
        }

        @Override
        public <R extends Comparable<?>> TypedConditionBuilder<T> notBetween(SFunction<T, R> getter, @Nullable R from, @Nullable R to) {
            delegate.notBetween(resolveColumnName(getter), from, to);
            return this;
        }

        @Override
        public <R> TypedConditionBuilder<T> jsonContains(SFunction<T, R> getter, @Nullable Object jsonValue) {
            delegate.jsonContains(resolveColumnName(getter), jsonValue);
            return this;
        }

        @Override
        public <R> TypedConditionBuilder<T> jsonContained(SFunction<T, R> getter, @Nullable Object jsonValue) {
            delegate.jsonContained(resolveColumnName(getter), jsonValue);
            return this;
        }

        @Override
        public TypedConditionBuilder<T> jsonPath(SFunction<T, String> getter, @Nullable String path) {
            delegate.jsonPath(resolveColumnName(getter), path);
            return this;
        }

        // ==================== IfPresent 操作符实现 ====================

        @Override
        public <R> TypedConditionBuilder<T> eqIfPresent(SFunction<T, R> getter, @Nullable Object value) {
            if (value == null) {
                return this;
            }
            return eq(getter, value);
        }

        @Override
        public <R> TypedConditionBuilder<T> neIfPresent(SFunction<T, R> getter, @Nullable Object value) {
            if (value == null) {
                return this;
            }
            return ne(getter, value);
        }

        @Override
        public TypedConditionBuilder<T> likeIfPresent(SFunction<T, String> getter, @Nullable String value) {
            if (value == null || value.isEmpty()) {
                return this;
            }
            return like(getter, value);
        }

        @Override
        public TypedConditionBuilder<T> likeStartsWithIfPresent(SFunction<T, String> getter, @Nullable String value) {
            if (value == null || value.isEmpty()) {
                return this;
            }
            return likeStartsWith(getter, value);
        }

        @Override
        public TypedConditionBuilder<T> likeEndsWithIfPresent(SFunction<T, String> getter, @Nullable String value) {
            if (value == null || value.isEmpty()) {
                return this;
            }
            return likeEndsWith(getter, value);
        }

        @Override
        public TypedConditionBuilder<T> notLikeIfPresent(SFunction<T, String> getter, @Nullable String value) {
            if (value == null || value.isEmpty()) {
                return this;
            }
            return notLike(getter, value);
        }

        @Override
        public <R> TypedConditionBuilder<T> inIfPresent(SFunction<T, R> getter, @Nullable Iterable<?> values) {
            if (values == null) {
                return this;
            }
            List<Object> list = new ArrayList<>();
            values.forEach(list::add);
            if (list.isEmpty()) {
                return this;
            }
            return in(getter, list);
        }

        @Override
        public <R> TypedConditionBuilder<T> notInIfPresent(SFunction<T, R> getter, @Nullable Iterable<?> values) {
            if (values == null) {
                return this;
            }
            List<Object> list = new ArrayList<>();
            values.forEach(list::add);
            if (list.isEmpty()) {
                return this;
            }
            return notIn(getter, list);
        }

        @Override
        public <R extends Comparable<?>> TypedConditionBuilder<T> betweenIfPresent(SFunction<T, R> getter, @Nullable R from, @Nullable R to) {
            if (from == null || to == null) {
                return this;
            }
            return between(getter, from, to);
        }

        @Override
        public <R extends Comparable<?>> TypedConditionBuilder<T> notBetweenIfPresent(SFunction<T, R> getter, @Nullable R from, @Nullable R to) {
            if (from == null || to == null) {
                return this;
            }
            return notBetween(getter, from, to);
        }

        @Override
        public <R extends Comparable<?>> TypedConditionBuilder<T> gtIfPresent(SFunction<T, R> getter, @Nullable R value) {
            if (value == null) {
                return this;
            }
            return gt(getter, value);
        }

        @Override
        public <R extends Comparable<?>> TypedConditionBuilder<T> geIfPresent(SFunction<T, R> getter, @Nullable R value) {
            if (value == null) {
                return this;
            }
            return ge(getter, value);
        }

        @Override
        public <R extends Comparable<?>> TypedConditionBuilder<T> ltIfPresent(SFunction<T, R> getter, @Nullable R value) {
            if (value == null) {
                return this;
            }
            return lt(getter, value);
        }

        @Override
        public <R extends Comparable<?>> TypedConditionBuilder<T> leIfPresent(SFunction<T, R> getter, @Nullable R value) {
            if (value == null) {
                return this;
            }
            return le(getter, value);
        }

        @Override
        public TypedConditionBuilder<T> and(Condition condition) {
            delegate.and(condition);
            return this;
        }

        @Override
        public TypedConditionBuilder<T> or(Condition condition) {
            delegate.or(condition);
            return this;
        }

        @Override
        public Condition build() {
            return delegate.build();
        }

        /**
         * 使用 FieldNameResolver 解析 Lambda 对应的数据库列名
         *
         * @param getter Lambda 方法引用
         * @return 数据库列名
         */
        private <R> String resolveColumnName(SFunction<T, R> getter) {
            return getFieldNameResolver().resolveColumnName(entityClass, getter);
        }

    }
}