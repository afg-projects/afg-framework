package io.github.afgprojects.framework.data.core.query;

import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * 查询条件接口
 */
public interface Condition {

    /**
     * 获取所有条件项
     */
    @NonNull List<Criterion> getCriteria();

    /**
     * 获取逻辑操作符（AND / OR）
     */
    @NonNull LogicalOperator getOperator();

    /**
     * 是否为空条件
     */
    boolean isEmpty();

    /**
     * 添加条件项
     */
    @NonNull Condition add(@NonNull Criterion criterion);

    /**
     * AND 连接条件
     */
    @NonNull Condition and(@NonNull Condition other);

    /**
     * OR 连接条件
     */
    @NonNull Condition or(@NonNull Condition other);

    /**
     * 条件取反
     * <p>
     * 返回一个新的条件，该条件匹配原条件不匹配的记录。
     * 例如：{@code eq("status", 1).not()} 等价于 {@code ne("status", 1)}
     *
     * @return 取反后的条件
     */
    default @NonNull Condition not() {
        return new NotCondition(this);
    }

    /**
     * 创建空条件
     */
    static @NonNull Condition empty() {
        return ConditionImpl.EMPTY;
    }
}