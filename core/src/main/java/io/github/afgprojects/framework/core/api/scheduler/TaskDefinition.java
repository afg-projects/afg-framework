package io.github.afgprojects.framework.core.api.scheduler;

import java.time.Instant;
import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 任务定义
 *
 * <p>描述一个定时任务的完整定义，支持动态配置
 *
 * @param taskId        任务唯一标识
 * @param taskGroup     任务分组
 * @param cron          Cron 表达式
 * @param fixedRate     固定速率（毫秒）
 * @param fixedDelay    固定延迟（毫秒）
 * @param initialDelay  初始延迟（毫秒）
 * @param description   任务描述
 * @param enabled       是否启用
 * @param timeout       超时时间（毫秒），-1 表示不超时
 * @param maxRetries    最大重试次数
 * @param retryDelay    重试延迟（毫秒）
 * @param metadata      扩展元数据
 */
public record TaskDefinition(
    @NonNull String taskId,
    @NonNull String taskGroup,
    @Nullable String cron,
    long fixedRate,
    long fixedDelay,
    long initialDelay,
    @Nullable String description,
    boolean enabled,
    long timeout,
    int maxRetries,
    long retryDelay,
    @Nullable Map<String, String> metadata
) {
    /**
     * 创建 Cron 任务定义
     */
    public static TaskDefinition ofCron(@NonNull String taskId, @NonNull String cron) {
        return new TaskDefinition(taskId, "default", cron, -1, -1, 0, null, true, -1, 0, 0, null);
    }

    /**
     * 创建固定速率任务定义
     */
    public static TaskDefinition ofFixedRate(@NonNull String taskId, long fixedRateMs) {
        return new TaskDefinition(taskId, "default", null, fixedRateMs, -1, 0, null, true, -1, 0, 0, null);
    }

    /**
     * 创建固定延迟任务定义
     */
    public static TaskDefinition ofFixedDelay(@NonNull String taskId, long fixedDelayMs) {
        return new TaskDefinition(taskId, "default", null, -1, fixedDelayMs, 0, null, true, -1, 0, 0, null);
    }

    /**
     * 创建一次性任务定义
     */
    public static TaskDefinition ofOnce(@NonNull String taskId, @NonNull Instant executeTime) {
        return new TaskDefinition(taskId, "default", null, -1, -1, 0, null, true, -1, 0, 0,
            Map.of("executeTime", executeTime.toString()));
    }

    /**
     * 设置任务分组
     */
    public TaskDefinition withGroup(@NonNull String group) {
        return new TaskDefinition(taskId, group, cron, fixedRate, fixedDelay, initialDelay,
            description, enabled, timeout, maxRetries, retryDelay, metadata);
    }

    /**
     * 设置任务描述
     */
    public TaskDefinition withDescription(@NonNull String desc) {
        return new TaskDefinition(taskId, taskGroup, cron, fixedRate, fixedDelay, initialDelay,
            desc, enabled, timeout, maxRetries, retryDelay, metadata);
    }

    /**
     * 设置是否启用
     */
    public TaskDefinition withEnabled(boolean enable) {
        return new TaskDefinition(taskId, taskGroup, cron, fixedRate, fixedDelay, initialDelay,
            description, enable, timeout, maxRetries, retryDelay, metadata);
    }

    /**
     * 设置超时时间
     */
    public TaskDefinition withTimeout(long timeoutMs) {
        return new TaskDefinition(taskId, taskGroup, cron, fixedRate, fixedDelay, initialDelay,
            description, enabled, timeoutMs, maxRetries, retryDelay, metadata);
    }

    /**
     * 设置重试配置
     */
    public TaskDefinition withRetry(int maxRetries, long retryDelayMs) {
        return new TaskDefinition(taskId, taskGroup, cron, fixedRate, fixedDelay, initialDelay,
            description, enabled, timeout, maxRetries, retryDelayMs, metadata);
    }

    /**
     * 设置元数据
     */
    public TaskDefinition withMetadata(@NonNull Map<String, String> meta) {
        return new TaskDefinition(taskId, taskGroup, cron, fixedRate, fixedDelay, initialDelay,
            description, enabled, timeout, maxRetries, retryDelay, meta);
    }

    /**
     * 是否为 Cron 任务
     */
    public boolean isCronTask() {
        return cron != null && !cron.isEmpty();
    }

    /**
     * 是否为固定速率任务
     */
    public boolean isFixedRateTask() {
        return fixedRate > 0;
    }

    /**
     * 是否为固定延迟任务
     */
    public boolean isFixedDelayTask() {
        return fixedDelay > 0;
    }
}
