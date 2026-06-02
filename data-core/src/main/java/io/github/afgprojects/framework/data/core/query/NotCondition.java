package io.github.afgprojects.framework.data.core.query;

import org.jspecify.annotations.NonNull;

import java.util.Collections;
import java.util.List;

/**
 * 条件取反包装器
 * <p>
 * 用于对条件进行取反操作，生成 NOT (condition) 形式的 SQL。
 */
public final class NotCondition implements Condition {

    private final Condition original;

    /**
     * 构造取反条件
     *
     * @param original 原始条件
     */
    public NotCondition(@NonNull Condition original) {
        this.original = original;
    }

    /**
     * 获取原始条件
     *
     * @return 原始条件
     */
    public @NonNull Condition getOriginal() {
        return original;
    }

    @Override
    public @NonNull List<Criterion> getCriteria() {
        return Collections.singletonList(
            Criterion.notNested(original)
        );
    }

    @Override
    public @NonNull LogicalOperator getOperator() {
        return LogicalOperator.AND;
    }

    @Override
    public boolean isEmpty() {
        return original.isEmpty();
    }

    @Override
    public @NonNull Condition add(@NonNull Criterion criterion) {
        return new NotCondition(original.add(criterion));
    }

    @Override
    public @NonNull Condition and(@NonNull Condition other) {
        if (this.isEmpty()) return other;
        if (other.isEmpty()) return this;
        return new ConditionImpl(LogicalOperator.AND, List.of(
            Criterion.nested(this, LogicalOperator.AND),
            Criterion.nested(other, LogicalOperator.AND)
        ));
    }

    @Override
    public @NonNull Condition or(@NonNull Condition other) {
        if (this.isEmpty()) return other;
        if (other.isEmpty()) return this;
        return new ConditionImpl(LogicalOperator.OR, List.of(
            Criterion.nested(this, LogicalOperator.OR),
            Criterion.nested(other, LogicalOperator.OR)
        ));
    }

    @Override
    public @NonNull Condition not() {
        // 双重取反等于原条件
        return original;
    }

    @Override
    public String toString() {
        return "NOT(" + original + ")";
    }
}
