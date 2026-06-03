package io.github.afgprojects.framework.data.core.query;

import org.jspecify.annotations.NonNull;

import java.util.Collections;
import java.util.List;

/**
 * 永假条件（DENY_ALL），表示拒绝所有数据访问
 * <p>
 * 生成 SQL：{@code 1 = 0}
 * <p>
 * 与 {@code Conditions.builder().eq("1", "0")} 不同，此条件不依赖字段名，
 * 因此不会触发字段名验证异常。
 *
 * @see io.github.afgprojects.framework.data.core.condition.Conditions#none()
 */
public final class DenyAllCondition implements Condition {

    @Override
    public @NonNull List<Criterion> getCriteria() {
        return Collections.emptyList();
    }

    @Override
    public @NonNull LogicalOperator getOperator() {
        return LogicalOperator.AND;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public @NonNull Condition add(@NonNull Criterion criterion) {
        // DENY_ALL AND anything = DENY_ALL
        return this;
    }

    @Override
    public @NonNull Condition and(@NonNull Condition other) {
        // DENY_ALL AND anything = DENY_ALL
        return this;
    }

    @Override
    public @NonNull Condition or(@NonNull Condition other) {
        // DENY_ALL OR other = other
        return other;
    }

    @Override
    public @NonNull Condition not() {
        // NOT (1 = 0) 即全部允许，返回空条件
        return Condition.empty();
    }

    @Override
    public String toString() {
        return "DENY_ALL";
    }
}
