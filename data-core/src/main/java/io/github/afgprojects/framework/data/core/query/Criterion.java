package io.github.afgprojects.framework.data.core.query;

import org.jspecify.annotations.Nullable;

/**
 * 查询条件项
 *
 * @param field        字段名
 * @param operator     操作符
 * @param value        值（可为 null，用于 IS_NULL/IS_NOT_NULL）
 * @param nextOperator 下一个条件的逻辑操作符（AND/OR），null 表示最后一个条件
 */
public record Criterion(
    String field,
    Operator operator,
    @Nullable Object value,
    @Nullable LogicalOperator nextOperator
) {
    /**
     * 创建简单条件项（无 nextOperator）
     */
    public static Criterion of(String field, Operator operator, @Nullable Object value) {
        return new Criterion(field, operator, value, null);
    }

    /**
     * 兼容旧 API 的构造方法
     */
    public Criterion(String field, Operator operator, @Nullable Object value) {
        this(field, operator, value, null);
    }

    /**
     * 判断是否为一元操作符（无需值）
     */
    public boolean isUnary() {
        return operator == Operator.IS_NULL || operator == Operator.IS_NOT_NULL;
    }

    /**
     * 判断是否为范围操作符
     */
    public boolean isRange() {
        return operator == Operator.BETWEEN || operator == Operator.NOT_BETWEEN;
    }

    /**
     * 判断是否为集合操作符
     */
    public boolean isCollection() {
        return operator == Operator.IN || operator == Operator.NOT_IN;
    }
}