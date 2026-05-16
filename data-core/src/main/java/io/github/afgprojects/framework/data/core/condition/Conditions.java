package io.github.afgprojects.framework.data.core.condition;

import io.github.afgprojects.framework.data.core.metadata.EntityMetadataCache;
import io.github.afgprojects.framework.data.core.naming.FieldNameResolver;
import io.github.afgprojects.framework.data.core.query.Condition;
import io.github.afgprojects.framework.data.core.query.ConditionImpl;
import io.github.afgprojects.framework.data.core.query.Criterion;
import io.github.afgprojects.framework.data.core.query.LogicalOperator;
import io.github.afgprojects.framework.data.core.query.Operator;
import org.jspecify.annotations.Nullable;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 条件工厂类
 */
@SuppressWarnings("PMD.AvoidCatchingGenericException")
public final class Conditions {

    /**
     * Lambda 字段名解析缓存
     */
    private static final ConcurrentHashMap<String, String> FIELD_NAME_CACHE = new ConcurrentHashMap<>();

    /**
     * 字段名解析器（用于 Lambda → 列名转换）
     */
    private static final FieldNameResolver FIELD_NAME_RESOLVER = new FieldNameResolver(new EntityMetadataCache());

    private Conditions() {}

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
     * 创建不匹配任何条件（WHERE 1=0）
     * <p>
     * 用于需要返回空结果集的场景
     */
    public static Condition none() {
        return builder().eq("1", "0").build();
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
        for (int i = 0; i < conditions.length; i++) {
            if (i == 0) {
                builder.and(conditions[i]);
            } else {
                builder.or(conditions[i]);
            }
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
     * 创建 LIKE 条件
     */
    public static Condition like(String field, @Nullable String value) {
        if (value == null) {
            return isNull(field);
        }
        return builder().like(field, value).build();
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
     * 创建类型化 LIKE 条件
     */
    public static <T> Condition like(Class<T> entityClass, SFunction<T, String> getter, @Nullable String value) {
        if (value == null) {
            return isNull(entityClass, getter);
        }
        return builder(entityClass).like(getter, value).build();
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
            criteria.add(new Criterion(field, Operator.EQ, value, null));
            return this;
        }

        @Override
        public ConditionBuilder ne(String field, @Nullable Object value) {
            criteria.add(new Criterion(field, Operator.NE, value, null));
            return this;
        }

        @Override
        public ConditionBuilder gt(String field, @Nullable Comparable<?> value) {
            criteria.add(new Criterion(field, Operator.GT, value, null));
            return this;
        }

        @Override
        public ConditionBuilder ge(String field, @Nullable Comparable<?> value) {
            criteria.add(new Criterion(field, Operator.GE, value, null));
            return this;
        }

        @Override
        public ConditionBuilder lt(String field, @Nullable Comparable<?> value) {
            criteria.add(new Criterion(field, Operator.LT, value, null));
            return this;
        }

        @Override
        public ConditionBuilder le(String field, @Nullable Comparable<?> value) {
            criteria.add(new Criterion(field, Operator.LE, value, null));
            return this;
        }

        @Override
        public ConditionBuilder like(String field, @Nullable String value) {
            String v = value;
            if (v != null && !v.contains("%")) {
                v = "%" + v + "%";
            }
            criteria.add(new Criterion(field, Operator.LIKE, v, null));
            return this;
        }

        @Override
        public ConditionBuilder likeLeft(String field, @Nullable String value) {
            String v = value;
            if (v != null && !v.endsWith("%")) {
                v = v + "%";
            }
            criteria.add(new Criterion(field, Operator.LIKE_LEFT, v, null));
            return this;
        }

        @Override
        public ConditionBuilder likeRight(String field, @Nullable String value) {
            String v = value;
            if (v != null && !v.startsWith("%")) {
                v = "%" + v;
            }
            criteria.add(new Criterion(field, Operator.LIKE_RIGHT, v, null));
            return this;
        }

        @Override
        public ConditionBuilder notLike(String field, @Nullable String value) {
            criteria.add(new Criterion(field, Operator.NOT_LIKE, value, null));
            return this;
        }

        @Override
        public ConditionBuilder in(String field, @Nullable Iterable<?> values) {
            criteria.add(new Criterion(field, Operator.IN, values, null));
            return this;
        }

        @Override
        public ConditionBuilder notIn(String field, @Nullable Iterable<?> values) {
            criteria.add(new Criterion(field, Operator.NOT_IN, values, null));
            return this;
        }

        @Override
        public ConditionBuilder isNull(String field) {
            criteria.add(new Criterion(field, Operator.IS_NULL, null, null));
            return this;
        }

        @Override
        public ConditionBuilder isNotNull(String field) {
            criteria.add(new Criterion(field, Operator.IS_NOT_NULL, null, null));
            return this;
        }

        @Override
        public ConditionBuilder between(String field, @Nullable Comparable<?> from, @Nullable Comparable<?> to) {
            criteria.add(new Criterion(field, Operator.BETWEEN, new Comparable<?>[]{from, to}, null));
            return this;
        }

        @Override
        public ConditionBuilder notBetween(String field, @Nullable Comparable<?> from, @Nullable Comparable<?> to) {
            criteria.add(new Criterion(field, Operator.NOT_BETWEEN, new Comparable<?>[]{from, to}, null));
            return this;
        }

        @Override
        public ConditionBuilder and(Condition condition) {
            if (!condition.isEmpty()) {
                criteria.add(new Criterion("__nested__", Operator.EQ, condition, LogicalOperator.AND));
            }
            return this;
        }

        @Override
        public ConditionBuilder or(Condition condition) {
            if (!condition.isEmpty()) {
                criteria.add(new Criterion("__nested__", Operator.EQ, condition, LogicalOperator.OR));
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
                        finalCriteria.add(new Criterion(c.field(), c.operator(), c.value(), operator));
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
            delegate.eq(resolveColumnName(getter), value);
            return this;
        }

        @Override
        public <R> TypedConditionBuilder<T> ne(SFunction<T, R> getter, @Nullable Object value) {
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
        public TypedConditionBuilder<T> likeLeft(SFunction<T, String> getter, @Nullable String value) {
            delegate.likeLeft(resolveColumnName(getter), value);
            return this;
        }

        @Override
        public TypedConditionBuilder<T> likeRight(SFunction<T, String> getter, @Nullable String value) {
            delegate.likeRight(resolveColumnName(getter), value);
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
            return FIELD_NAME_RESOLVER.resolveColumnName(entityClass, getter);
        }
    }
}