package io.github.afgprojects.framework.data.core.query;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 查询条件准则，表示一个字段-运算符-值的原子条件或嵌套条件。
 *
 * @param field           属性名（实体字段名或数据库列名），嵌套条件时为 null
 * @param operator        运算符
 * @param value           条件值，嵌套条件时为嵌套的 Condition 对象
 * @param nextOperator    与下一个准则之间的逻辑运算符
 * @param nestedCondition 嵌套条件（替代 __nested__ hack）
 * @param negated         是否取反（NOT 条件），为 true 时表示对嵌套条件取反
 */
public record Criterion(
        @Nullable String field,
        @NonNull Operator operator,
        @Nullable Object value,
        @Nullable LogicalOperator nextOperator,
        @Nullable Condition nestedCondition,
        boolean negated
) {

    /**
     * 兼容旧构造的简易工厂（negated = false）
     */
    public Criterion(@Nullable String field, @NonNull Operator operator, @Nullable Object value,
                     @Nullable LogicalOperator nextOperator, @Nullable Condition nestedCondition) {
        this(field, operator, value, nextOperator, nestedCondition, false);
    }

    /**
     * 创建普通条件准则
     */
    public static @NonNull Criterion of(@NonNull String field, @NonNull Operator operator, @Nullable Object value) {
        return new Criterion(field, operator, value, null, null, false);
    }

    /**
     * 创建带逻辑运算符的条件准则
     */
    public static @NonNull Criterion of(@NonNull String field, @NonNull Operator operator, @Nullable Object value, @Nullable LogicalOperator nextOperator) {
        return new Criterion(field, operator, value, nextOperator, null, false);
    }

    /**
     * 创建嵌套条件准则
     */
    public static @NonNull Criterion nested(@NonNull Condition nestedCondition, @Nullable LogicalOperator nextOperator) {
        return new Criterion(null, Operator.EQ, nestedCondition, nextOperator, nestedCondition, false);
    }

    /**
     * 创建 NOT 嵌套条件准则
     * <p>
     * 表示对嵌套条件取反，生成 NOT (condition) 形式的 SQL。
     *
     * @param nestedCondition 要取反的嵌套条件
     * @return NOT 嵌套条件准则
     */
    public static @NonNull Criterion notNested(@NonNull Condition nestedCondition) {
        return new Criterion(null, Operator.EQ, nestedCondition, null, nestedCondition, true);
    }

    /**
     * 判断是否为嵌套条件
     */
    public boolean isNested() {
        return nestedCondition != null;
    }

    /**
     * 判断是否为取反条件（NOT）
     */
    public boolean isNegated() {
        return negated;
    }

    /**
     * 判断是否为一元运算符（不需要值）
     */
    public boolean isUnary() {
        return operator == Operator.IS_NULL || operator == Operator.IS_NOT_NULL;
    }

    /**
     * 判断是否为范围运算符（需要两个值）
     */
    public boolean isRange() {
        return operator == Operator.BETWEEN || operator == Operator.NOT_BETWEEN;
    }

    /**
     * 判断是否为集合运算符（需要集合值）
     */
    public boolean isCollection() {
        return operator == Operator.IN || operator == Operator.NOT_IN;
    }
}
