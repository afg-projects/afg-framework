package io.github.afgprojects.framework.data.core.condition;

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

/**
 * 条件工厂类
 */
@SuppressWarnings("PMD.AvoidCatchingGenericException")
public final class Conditions {

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
     * 创建等于条件
     */
    public static Condition eq(String field, @Nullable Object value) {
        return builder().eq(field, value).build();
    }

    /**
     * 创建 LIKE 条件
     */
    public static Condition like(String field, @Nullable String value) {
        return builder().like(field, value).build();
    }

    /**
     * 创建 IN 条件
     */
    public static Condition in(String field, @Nullable Iterable<?> values) {
        return builder().in(field, values).build();
    }

    /**
     * 从 Lambda 获取字段名
     */
    public static <T, R> String getFieldName(SFunction<T, R> getter) {
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
            delegate.eq(getFieldName(getter), value);
            return this;
        }

        @Override
        public <R> TypedConditionBuilder<T> ne(SFunction<T, R> getter, @Nullable Object value) {
            delegate.ne(getFieldName(getter), value);
            return this;
        }

        @Override
        public <R extends Comparable<?>> TypedConditionBuilder<T> gt(SFunction<T, R> getter, @Nullable R value) {
            delegate.gt(getFieldName(getter), value);
            return this;
        }

        @Override
        public <R extends Comparable<?>> TypedConditionBuilder<T> ge(SFunction<T, R> getter, @Nullable R value) {
            delegate.ge(getFieldName(getter), value);
            return this;
        }

        @Override
        public <R extends Comparable<?>> TypedConditionBuilder<T> lt(SFunction<T, R> getter, @Nullable R value) {
            delegate.lt(getFieldName(getter), value);
            return this;
        }

        @Override
        public <R extends Comparable<?>> TypedConditionBuilder<T> le(SFunction<T, R> getter, @Nullable R value) {
            delegate.le(getFieldName(getter), value);
            return this;
        }

        @Override
        public TypedConditionBuilder<T> like(SFunction<T, String> getter, @Nullable String value) {
            delegate.like(getFieldName(getter), value);
            return this;
        }

        @Override
        public TypedConditionBuilder<T> likeLeft(SFunction<T, String> getter, @Nullable String value) {
            delegate.likeLeft(getFieldName(getter), value);
            return this;
        }

        @Override
        public TypedConditionBuilder<T> likeRight(SFunction<T, String> getter, @Nullable String value) {
            delegate.likeRight(getFieldName(getter), value);
            return this;
        }

        @Override
        public TypedConditionBuilder<T> notLike(SFunction<T, String> getter, @Nullable String value) {
            delegate.notLike(getFieldName(getter), value);
            return this;
        }

        @Override
        public <R> TypedConditionBuilder<T> in(SFunction<T, R> getter, @Nullable Iterable<?> values) {
            delegate.in(getFieldName(getter), values);
            return this;
        }

        @Override
        public <R> TypedConditionBuilder<T> notIn(SFunction<T, R> getter, @Nullable Iterable<?> values) {
            delegate.notIn(getFieldName(getter), values);
            return this;
        }

        @Override
        public <R> TypedConditionBuilder<T> isNull(SFunction<T, R> getter) {
            delegate.isNull(getFieldName(getter));
            return this;
        }

        @Override
        public <R> TypedConditionBuilder<T> isNotNull(SFunction<T, R> getter) {
            delegate.isNotNull(getFieldName(getter));
            return this;
        }

        @Override
        public <R extends Comparable<?>> TypedConditionBuilder<T> between(SFunction<T, R> getter, @Nullable R from, @Nullable R to) {
            delegate.between(getFieldName(getter), from, to);
            return this;
        }

        @Override
        public <R extends Comparable<?>> TypedConditionBuilder<T> notBetween(SFunction<T, R> getter, @Nullable R from, @Nullable R to) {
            delegate.notBetween(getFieldName(getter), from, to);
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
    }
}