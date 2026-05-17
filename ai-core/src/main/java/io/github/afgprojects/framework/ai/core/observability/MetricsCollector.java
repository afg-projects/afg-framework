package io.github.afgprojects.framework.ai.core.observability;

import org.jspecify.annotations.NonNull;

import java.time.Duration;
import java.util.Map;

/**
 * 指标收集器接口
 *
 * <p>用于收集和记录 AI 操作的各种指标：
 * <ul>
 *   <li>请求计数</li>
 *   <li>响应时间</li>
 *   <li>Token 使用量</li>
 *   <li>错误率</li>
 *   <li>成本统计</li>
 * </ul>
 *
 * @author afg-projects
 * @since 1.0.0
 */
public interface MetricsCollector {

    /**
     * 记录请求开始
     *
     * @param operationType 操作类型
     * @param modelName     模型名称
     * @param tags          标签（用于分类）
     * @return 计时器句柄（用于记录结束时间）
     */
    @NonNull
    Timer startTimer(@NonNull String operationType, @NonNull String modelName, @NonNull Map<String, String> tags);

    /**
     * 记录请求计数
     *
     * @param operationType 操作类型
     * @param modelName     模型名称
     * @param status        状态（success, failure, timeout）
     * @param tags          标签
     */
    void recordCount(
            @NonNull String operationType,
            @NonNull String modelName,
            @NonNull String status,
            @NonNull Map<String, String> tags
    );

    /**
     * 记录 Token 使用量
     *
     * @param modelName     模型名称
     * @param inputTokens   输入 Token 数
     * @param outputTokens  输出 Token 数
     * @param tags          标签
     */
    void recordTokenUsage(
            @NonNull String modelName,
            long inputTokens,
            long outputTokens,
            @NonNull Map<String, String> tags
    );

    /**
     * 记录成本
     *
     * @param modelName 模型名称
     * @param cost      成本（美元）
     * @param tags      标签
     */
    void recordCost(@NonNull String modelName, double cost, @NonNull Map<String, String> tags);

    /**
     * 记录响应大小
     *
     * @param operationType 操作类型
     * @param modelName     模型名称
     * @param sizeBytes     响应大小（字节）
     * @param tags          标签
     */
    void recordResponseSize(
            @NonNull String operationType,
            @NonNull String modelName,
            long sizeBytes,
            @NonNull Map<String, String> tags
    );

    /**
     * 获取指标摘要
     *
     * @return 指标摘要
     */
    @NonNull
    MetricsSummary getSummary();

    /**
     * 计时器接口
     */
    interface Timer {

        /**
         * 停止计时并记录
         *
         * @param status 状态（success, failure, timeout）
         */
        void stop(@NonNull String status);

        /**
         * 停止计时并记录（带额外标签）
         *
         * @param status 状态
         * @param additionalTags 额外标签
         */
        void stop(@NonNull String status, @NonNull Map<String, String> additionalTags);

        /**
         * 获取已耗时
         *
         * @return 已耗时
         */
        @NonNull
        Duration getElapsed();

        /**
         * 获取开始时间戳
         *
         * @return 开始时间戳（毫秒）
         */
        long getStartTimeMs();
    }

    /**
     * 指标摘要接口
     */
    interface MetricsSummary {

        /**
         * 获取总请求数
         *
         * @return 总请求数
         */
        long getTotalRequests();

        /**
         * 获取成功请求数
         *
         * @return 成功请求数
         */
        long getSuccessRequests();

        /**
         * 获取失败请求数
         *
         * @return 失败请求数
         */
        long getFailedRequests();

        /**
         * 获取平均响应时间
         *
         * @return 平均响应时间
         */
        @NonNull
        Duration getAverageResponseTime();

        /**
         * 获取总 Token 使用量
         *
         * @return 总 Token 使用量
         */
        long getTotalTokens();

        /**
         * 获取总成本
         *
         * @return 总成本（美元）
         */
        double getTotalCost();

        /**
         * 获取按模型分组的统计
         *
         * @return 模型统计
         */
        @NonNull
        Map<String, ModelStats> getModelStats();
    }

    /**
     * 模型统计接口
     */
    interface ModelStats {

        /**
         * 获取模型名称
         *
         * @return 模型名称
         */
        @NonNull
        String getModelName();

        /**
         * 获取请求计数
         *
         * @return 请求计数
         */
        long getRequestCount();

        /**
         * 获取平均响应时间
         *
         * @return 平均响应时间
         */
        @NonNull
        Duration getAverageResponseTime();

        /**
         * 获取总输入 Token
         *
         * @return 总输入 Token
         */
        long getTotalInputTokens();

        /**
         * 获取总输出 Token
         *
         * @return 总输出 Token
         */
        long getTotalOutputTokens();

        /**
         * 获取总成本
         *
         * @return 总成本
         */
        double getTotalCost();
    }
}