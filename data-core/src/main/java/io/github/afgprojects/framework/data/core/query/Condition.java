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
     * 创建空条件
     */
    static @NonNull Condition empty() {
        return ConditionImpl.EMPTY;
    }
}