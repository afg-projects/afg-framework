package io.github.afgprojects.framework.data.core.query;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Condition 默认实现
 */
public final class ConditionImpl implements Condition {

    public static final Condition EMPTY = new ConditionImpl(LogicalOperator.AND, Collections.emptyList());

    private final LogicalOperator operator;
    private final List<Criterion> criteria;

    public ConditionImpl(@NonNull LogicalOperator operator, @NonNull List<Criterion> criteria) {
        this.operator = operator;
        this.criteria = Collections.unmodifiableList(new ArrayList<>(criteria));
    }

    @Override
    public @NonNull List<Criterion> getCriteria() {
        return criteria;
    }

    @Override
    public @NonNull LogicalOperator getOperator() {
        return operator;
    }

    @Override
    public boolean isEmpty() {
        return criteria.isEmpty();
    }

    @Override
    public @NonNull Condition add(@NonNull Criterion criterion) {
        List<Criterion> newCriteria = new ArrayList<>(this.criteria);
        newCriteria.add(criterion);
        return new ConditionImpl(this.operator, newCriteria);
    }

    @Override
    public @NonNull Condition and(@NonNull Condition other) {
        if (this.isEmpty()) return other;
        if (other.isEmpty()) return this;

        // 如果两个条件的 operator 相同且都是 AND，可以直接合并 criteria
        // 否则必须使用嵌套条件保持逻辑结构
        if (this.operator == LogicalOperator.AND && other.getOperator() == LogicalOperator.AND) {
            List<Criterion> combined = new ArrayList<>(this.criteria);
            combined.addAll(other.getCriteria());
            return new ConditionImpl(LogicalOperator.AND, combined);
        }

        // 使用嵌套条件保持逻辑结构：(this) AND (other)
        return new ConditionImpl(LogicalOperator.AND, List.of(
            Criterion.nested(this, null),
            Criterion.nested(other, null)
        ));
    }

    @Override
    public @NonNull Condition or(@NonNull Condition other) {
        if (this.isEmpty()) return other;
        if (other.isEmpty()) return this;

        // 如果两个条件的 operator 相同且都是 OR，可以直接合并 criteria
        // 否则必须使用嵌套条件保持逻辑结构
        if (this.operator == LogicalOperator.OR && other.getOperator() == LogicalOperator.OR) {
            List<Criterion> combined = new ArrayList<>(this.criteria);
            combined.addAll(other.getCriteria());
            return new ConditionImpl(LogicalOperator.OR, combined);
        }

        // 使用嵌套条件保持逻辑结构：(this) OR (other)
        return new ConditionImpl(LogicalOperator.OR, List.of(
            Criterion.nested(this, null),
            Criterion.nested(other, null)
        ));
    }

    @Override
    public String toString() {
        return "Condition{" +
                "operator=" + operator +
                ", criteria=" + criteria +
                '}';
    }
}