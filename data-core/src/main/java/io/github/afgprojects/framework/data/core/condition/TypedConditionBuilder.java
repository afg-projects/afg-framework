package io.github.afgprojects.framework.data.core.condition;

import io.github.afgprojects.framework.data.core.query.Condition;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * 类型化条件构建器（Lambda 字段引用）
 * <p>
 * 提供基于 Lambda 方法引用的类型安全条件构建。通过泛型参数确保字段引用的正确性，
 * 同时对 {@code eq} 和 {@code ne} 方法的 {@code value} 参数进行运行时类型检查，
 * 以尽量提前发现类型不匹配的问题。
 * <p>
 * <b>类型安全说明：</b>由于 Java 类型擦除和 Lambda 限制，{@code eq} 和 {@code ne} 方法
 * 的 {@code value} 参数类型为 {@code Object}，但调用方应确保传入的值与字段类型一致。
 * 例如，对于 {@code Integer} 类型的字段，不应传入 {@code String} 值。
 * 运行时将检查值类型是否与字段声明类型兼容，不兼容时抛出 {@link IllegalArgumentException}。
 *
 * @param <T> 实体类型
 */
public interface TypedConditionBuilder<T> {

    /**
     * 添加等于条件（=）
     * <p>
     * <b>类型安全说明：</b>{@code value} 应与字段 {@code getter} 的返回类型一致。
     * 例如，如果字段类型为 {@code Integer}，则 {@code value} 应为 {@code Integer} 或 {@code null}。
     * 运行时会检查值类型与字段声明类型的兼容性。
     *
     * @param getter 字段 getter 方法引用
     * @param value  字段值，应与字段类型一致，可以为 null（null 会转换为 IS NULL 条件）
     * @param <R>    字段类型
     * @return 条件构建器（支持链式调用）
     * @throws IllegalArgumentException 如果 value 的运行时类型与字段声明类型不兼容
     */
    <R> TypedConditionBuilder<T> eq(SFunction<T, R> getter, @Nullable Object value);

    /**
     * 添加不等于条件（!=）
     * <p>
     * <b>类型安全说明：</b>{@code value} 应与字段 {@code getter} 的返回类型一致。
     * 例如，如果字段类型为 {@code Integer}，则 {@code value} 应为 {@code Integer} 或 {@code null}。
     * 运行时会检查值类型与字段声明类型的兼容性。
     *
     * @param getter 字段 getter 方法引用
     * @param value  字段值，应与字段类型一致，可以为 null（null 会转换为 IS NOT NULL 条件）
     * @param <R>    字段类型
     * @return 条件构建器（支持链式调用）
     * @throws IllegalArgumentException 如果 value 的运行时类型与字段声明类型不兼容
     */
    <R> TypedConditionBuilder<T> ne(SFunction<T, R> getter, @Nullable Object value);

    /**
     * 添加大于条件（&gt;）
     *
     * @param getter 字段 getter 方法引用
     * @param value  比较值，类型安全（与字段类型一致）
     * @param <R>    可比较的字段类型
     * @return 条件构建器（支持链式调用）
     */
    <R extends Comparable<?>> TypedConditionBuilder<T> gt(SFunction<T, R> getter, @Nullable R value);

    /**
     * 添加大于等于条件（&gt;=）
     *
     * @param getter 字段 getter 方法引用
     * @param value  比较值，类型安全（与字段类型一致）
     * @param <R>    可比较的字段类型
     * @return 条件构建器（支持链式调用）
     */
    <R extends Comparable<?>> TypedConditionBuilder<T> ge(SFunction<T, R> getter, @Nullable R value);

    /**
     * 添加小于条件（&lt;）
     *
     * @param getter 字段 getter 方法引用
     * @param value  比较值，类型安全（与字段类型一致）
     * @param <R>    可比较的字段类型
     * @return 条件构建器（支持链式调用）
     */
    <R extends Comparable<?>> TypedConditionBuilder<T> lt(SFunction<T, R> getter, @Nullable R value);

    /**
     * 添加小于等于条件（&lt;=）
     *
     * @param getter 字段 getter 方法引用
     * @param value  比较值，类型安全（与字段类型一致）
     * @param <R>    可比较的字段类型
     * @return 条件构建器（支持链式调用）
     */
    <R extends Comparable<?>> TypedConditionBuilder<T> le(SFunction<T, R> getter, @Nullable R value);

    /**
     * 添加 LIKE 条件（包含匹配）
     *
     * @param getter 字段 getter 方法引用（字段类型为 String）
     * @param value  匹配值
     * @return 条件构建器（支持链式调用）
     */
    TypedConditionBuilder<T> like(SFunction<T, String> getter, @Nullable String value);

    /**
     * 添加前缀匹配条件（value%）
     * <p>
     * 匹配以指定值开头的字符串。
     *
     * @param getter 字段 getter 方法引用（字段类型为 String）
     * @param value  匹配值
     * @return 条件构建器（支持链式调用）
     */
    TypedConditionBuilder<T> likeStartsWith(SFunction<T, String> getter, @Nullable String value);

    /**
     * 添加后缀匹配条件（%value）
     * <p>
     * 匹配以指定值结尾的字符串。
     *
     * @param getter 字段 getter 方法引用（字段类型为 String）
     * @param value  匹配值
     * @return 条件构建器（支持链式调用）
     */
    TypedConditionBuilder<T> likeEndsWith(SFunction<T, String> getter, @Nullable String value);

    /**
     * 添加 NOT LIKE 条件
     *
     * @param getter 字段 getter 方法引用（字段类型为 String）
     * @param value  匹配值
     * @return 条件构建器（支持链式调用）
     */
    TypedConditionBuilder<T> notLike(SFunction<T, String> getter, @Nullable String value);

    /**
     * 添加 IN 条件
     *
     * @param getter 字段 getter 方法引用
     * @param values 值集合
     * @param <R>    字段类型
     * @return 条件构建器（支持链式调用）
     */
    <R> TypedConditionBuilder<T> in(SFunction<T, R> getter, @Nullable Iterable<?> values);

    /**
     * 添加 NOT IN 条件
     *
     * @param getter 字段 getter 方法引用
     * @param values 值集合
     * @param <R>    字段类型
     * @return 条件构建器（支持链式调用）
     */
    <R> TypedConditionBuilder<T> notIn(SFunction<T, R> getter, @Nullable Iterable<?> values);

    /**
     * 添加 IS NULL 条件
     *
     * @param getter 字段 getter 方法引用
     * @param <R>    字段类型
     * @return 条件构建器（支持链式调用）
     */
    <R> TypedConditionBuilder<T> isNull(SFunction<T, R> getter);

    /**
     * 添加 IS NOT NULL 条件
     *
     * @param getter 字段 getter 方法引用
     * @param <R>    字段类型
     * @return 条件构建器（支持链式调用）
     */
    <R> TypedConditionBuilder<T> isNotNull(SFunction<T, R> getter);

    /**
     * 添加 BETWEEN 条件
     *
     * @param getter 字段 getter 方法引用
     * @param from   起始值
     * @param to     结束值
     * @param <R>    可比较的字段类型
     * @return 条件构建器（支持链式调用）
     */
    <R extends Comparable<?>> TypedConditionBuilder<T> between(SFunction<T, R> getter, @Nullable R from, @Nullable R to);

    /**
     * 添加 NOT BETWEEN 条件
     *
     * @param getter 字段 getter 方法引用
     * @param from   起始值
     * @param to     结束值
     * @param <R>    可比较的字段类型
     * @return 条件构建器（支持链式调用）
     */
    <R extends Comparable<?>> TypedConditionBuilder<T> notBetween(SFunction<T, R> getter, @Nullable R from, @Nullable R to);

    // ==================== JSON 操作符 ====================

    /**
     * 添加 JSON CONTAINS 条件（Lambda 方式）
     * <p>
     * JSON 列包含指定值。getter 的字段类型可能是 String、Map 或自定义 JSON 类型。
     *
     * @param getter    字段 getter 方法引用
     * @param jsonValue JSON 值（字符串或对象）
     * @param <R>       字段类型
     * @return 条件构建器（支持链式调用）
     */
    <R> TypedConditionBuilder<T> jsonContains(SFunction<T, R> getter, @Nullable Object jsonValue);

    /**
     * 添加 JSON CONTAINED 条件（Lambda 方式）
     * <p>
     * JSON 列被指定值包含。getter 的字段类型可能是 String、Map 或自定义 JSON 类型。
     *
     * @param getter    字段 getter 方法引用
     * @param jsonValue JSON 值（字符串或对象）
     * @param <R>       字段类型
     * @return 条件构建器（支持链式调用）
     */
    <R> TypedConditionBuilder<T> jsonContained(SFunction<T, R> getter, @Nullable Object jsonValue);

    /**
     * 添加 JSON PATH 条件（Lambda 方式）
     * <p>
     * JSON 路径存在。路径表达式始终是字符串类型。
     *
     * @param getter 字段 getter 方法引用（字段类型为 String）
     * @param path   JSON 路径表达式
     * @return 条件构建器（支持链式调用）
     */
    TypedConditionBuilder<T> jsonPath(SFunction<T, String> getter, @Nullable String path);

    /**
     * 添加 AND 嵌套条件
     *
     * @param condition 嵌套条件
     * @return 条件构建器（支持链式调用）
     */
    TypedConditionBuilder<T> and(Condition condition);

    /**
     * 添加 OR 嵌套条件
     *
     * @param condition 嵌套条件
     * @return 条件构建器（支持链式调用）
     */
    TypedConditionBuilder<T> or(Condition condition);

    /**
     * 构建最终条件
     *
     * @return 构建完成的条件
     */
    Condition build();

    // ==================== 运行时类型检查工具方法 ====================

    /**
     * 检查值类型是否与字段声明类型兼容
     * <p>
     * 对于 {@code eq} 和 {@code ne} 等接受 {@code Object} 参数的方法，
     * 提供运行时类型安全检查。如果值不为 null 且类型与字段类型不兼容，
     * 抛出 {@link IllegalArgumentException}。
     * <p>
     * 类型兼容规则：
     * <ul>
     *   <li>null 值始终兼容（null 会转换为 IS NULL / IS NOT NULL 条件）</li>
     *   <li>值类型与字段类型完全匹配</li>
     *   <li>值类型是字段类型的子类</li>
     *   <li>原始类型与包装类型互相兼容（int/Integer, long/Long 等）</li>
     * </ul>
     *
     * @param fieldName  字段名（用于错误消息）
     * @param fieldType  字段声明类型
     * @param value      要检查的值
     * @throws IllegalArgumentException 如果值类型与字段类型不兼容
     */
    static void checkFieldType(String fieldName, Class<?> fieldType, @Nullable Object value) {
        if (value == null) {
            return;
        }
        Class<?> valueType = value.getClass();
        // 完全匹配或子类
        if (fieldType.isAssignableFrom(valueType)) {
            return;
        }
        // 原始类型与包装类型互相兼容
        if (isPrimitiveWrapper(fieldType, valueType) || isPrimitiveWrapper(valueType, fieldType)) {
            return;
        }
        throw new IllegalArgumentException(
            String.format("Type mismatch for field '%s': expected %s but got %s (value: %s)",
                fieldName, fieldType.getSimpleName(), valueType.getSimpleName(), value)
        );
    }

    /**
     * 判断两个类型是否为原始类型与包装类型的关系
     */
    private static boolean isPrimitiveWrapper(Class<?> primitiveOrWrapper, Class<?> other) {
        Class<?> expectedWrapper = primitiveWrapperMap().get(primitiveOrWrapper);
        if (expectedWrapper != null && expectedWrapper == other) return true;
        Class<?> expectedPrimitive = primitiveWrapperMap().get(other);
        return expectedPrimitive != null && expectedPrimitive == primitiveOrWrapper;
    }

    /**
     * 返回原始类型到包装类型的映射
     */
    private static Map<Class<?>, Class<?>> primitiveWrapperMap() {
        return Map.of(
            int.class, Integer.class,
            long.class, Long.class,
            double.class, Double.class,
            float.class, Float.class,
            boolean.class, Boolean.class,
            short.class, Short.class,
            byte.class, Byte.class,
            char.class, Character.class
        );
    }
}