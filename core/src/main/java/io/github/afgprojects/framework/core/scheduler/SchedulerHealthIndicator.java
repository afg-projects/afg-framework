package io.github.afgprojects.framework.core.scheduler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import io.github.afgprojects.framework.core.api.scheduler.TaskExecutionLogStorage;
import io.github.afgprojects.framework.core.api.scheduler.TaskScheduler;

/**
 * 调度器健康检查
 *
 * <p>提供任务调度器的健康检查能力，监控任务执行状态
 *
 * <h3>检查项目</h3>
 * <ul>
 *   <li>调度器是否正常运行</li>
 *   <li>任务执行失败率</li>
 *   <li>最近执行失败的任务</li>
 * </ul>
 *
 * <h3>配置</h3>
 * <pre>
 * afg:
 *   scheduler:
 *     health:
 *       enabled: true
 *       failure-rate-threshold: 0.5
 * </pre>
 *
 * @since 1.0.0
 */
@Component
@ConditionalOnClass(HealthIndicator.class)
@ConditionalOnProperty(prefix = "afg.scheduler.health", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SchedulerHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(SchedulerHealthIndicator.class);

    private final TaskScheduler taskScheduler;
    private final TaskExecutionLogStorage logStorage;
    private final ConcurrentMap<String, TaskHealthInfo> taskHealthInfo = new ConcurrentHashMap<>();

    /**
     * 失败率阈值（超过此值则标记为不健康）
     */
    private final double failureRateThreshold;

    /**
     * 检查的时间窗口（毫秒）
     */
    private final long timeWindowMs;

    /**
     * 创建调度器健康检查实例
     *
     * @param taskScheduler 任务调度器
     * @param logStorage    日志存储
     */
    public SchedulerHealthIndicator(@NonNull TaskScheduler taskScheduler,
                                     @NonNull TaskExecutionLogStorage logStorage) {
        this(taskScheduler, logStorage, 0.5, 3600000); // 默认1小时窗口
    }

    /**
     * 创建调度器健康检查实例
     *
     * @param taskScheduler       任务调度器
     * @param logStorage          日志存储
     * @param failureRateThreshold 失败率阈值
     * @param timeWindowMs        时间窗口（毫秒）
     */
    public SchedulerHealthIndicator(@NonNull TaskScheduler taskScheduler,
                                     @NonNull TaskExecutionLogStorage logStorage,
                                     double failureRateThreshold,
                                     long timeWindowMs) {
        this.taskScheduler = taskScheduler;
        this.logStorage = logStorage;
        this.failureRateThreshold = failureRateThreshold;
        this.timeWindowMs = timeWindowMs;

        log.info("SchedulerHealthIndicator initialized with failureRateThreshold={}, timeWindowMs={}",
            failureRateThreshold, timeWindowMs);
    }

    @Override
    public Health health() {
        Health.Builder builder = Health.up();

        try {
            // 检查基本状态
            builder.withDetail("schedulerType", taskScheduler.getClass().getSimpleName());

            // 获取执行统计
            long totalExecutions = getTotalExecutions();
            long failedExecutions = getFailedExecutions();
            double failureRate = totalExecutions > 0 ? (double) failedExecutions / totalExecutions : 0;

            builder.withDetail("totalExecutions", totalExecutions)
                   .withDetail("failedExecutions", failedExecutions)
                   .withDetail("failureRate", String.format("%.2f%%", failureRate * 100));

            // 检查失败率
            if (totalExecutions > 10 && failureRate > failureRateThreshold) {
                builder = Health.down()
                    .withDetail("reason", "High failure rate: " + String.format("%.2f%%", failureRate * 100))
                    .withDetail("threshold", String.format("%.2f%%", failureRateThreshold * 100));
            }

            // 记录任务健康信息
            builder.withDetail("taskHealth", taskHealthInfo);

        } catch (Exception e) {
            log.error("Health check failed: {}", e.getMessage());
            builder = Health.down()
                .withDetail("error", e.getMessage());
        }

        return builder.build();
    }

    /**
     * 更新任务健康信息
     *
     * @param taskId      任务 ID
     * @param success     是否成功
     * @param errorMessage 错误信息（可选）
     */
    public void updateTaskHealth(@NonNull String taskId, boolean success, String errorMessage) {
        TaskHealthInfo info = taskHealthInfo.computeIfAbsent(taskId, k -> new TaskHealthInfo());
        info.update(success, errorMessage);
    }

    /**
     * 获取任务健康信息
     *
     * @param taskId 任务 ID
     * @return 任务健康信息
     */
    public TaskHealthInfo getTaskHealth(@NonNull String taskId) {
        return taskHealthInfo.get(taskId);
    }

    /**
     * 清理任务健康信息
     *
     * @param taskId 任务 ID
     */
    public void clearTaskHealth(@NonNull String taskId) {
        taskHealthInfo.remove(taskId);
    }

    private long getTotalExecutions() {
        // 尝试获取总体执行次数
        // 由于 TaskExecutionLogStorage 接口不提供全局统计，这里返回简化值
        return taskHealthInfo.values().stream()
            .mapToLong(info -> info.totalCount)
            .sum();
    }

    private long getFailedExecutions() {
        return taskHealthInfo.values().stream()
            .mapToLong(info -> info.failedCount)
            .sum();
    }

    /**
     * 任务健康信息
     */
    public static class TaskHealthInfo {
        private long totalCount = 0;
        private long failedCount = 0;
        private long lastSuccessTime = 0;
        private long lastFailureTime = 0;
        private String lastError;

        public void update(boolean success, String errorMessage) {
            totalCount++;
            if (success) {
                lastSuccessTime = System.currentTimeMillis();
            } else {
                failedCount++;
                lastFailureTime = System.currentTimeMillis();
                lastError = errorMessage;
            }
        }

        public double getFailureRate() {
            return totalCount > 0 ? (double) failedCount / totalCount : 0;
        }

        public long getTotalCount() {
            return totalCount;
        }

        public long getFailedCount() {
            return failedCount;
        }

        public long getLastSuccessTime() {
            return lastSuccessTime;
        }

        public long getLastFailureTime() {
            return lastFailureTime;
        }

        public String getLastError() {
            return lastError;
        }
    }
}
