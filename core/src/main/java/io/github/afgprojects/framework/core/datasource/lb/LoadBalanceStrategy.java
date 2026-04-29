package io.github.afgprojects.framework.core.datasource.lb;

import java.util.List;

import org.jspecify.annotations.NonNull;

/**
 * 负载均衡策略接口
 *
 * <p>定义从库数据源的选择策略
 *
 * @since 1.0.0
 */
public interface LoadBalanceStrategy {

    /**
     * 选择一个数据源
     *
     * @param candidates 候选数据源列表（非空）
     * @return 选中的数据源名称
     */
    @NonNull
    String select(@NonNull List<String> candidates);

    /**
     * 获取策略名称
     *
     * @return 策略名称
     */
    @NonNull
    String getName();
}
