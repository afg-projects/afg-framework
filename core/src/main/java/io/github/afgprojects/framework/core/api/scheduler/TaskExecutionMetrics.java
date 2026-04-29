package io.github.afgprojects.framework.core.api.scheduler;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/**
 * 任务执行监控指标
 *
 * <p>收集和报告任务执行的监控指标，基于 Micrometer 实现
 *
 * <h3>指标列表</h3>
 * <ul>
 *   <li>{@code afg.scheduler.execution.count} - 执行次数计数器</li>
 *   <li>{@code afg.scheduler.execution.duration} - 执行时间分布</li>
 *   <li>{@code afg.scheduler.execution.active} - 当前活跃执行数</li>
 *   <li>{@code afg.scheduler.execution.errors} - 错误次数计数器</li>
 *   <li>{@code afg.scheduler.execution.retries} - 重试次数计数器</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class TaskExecutionMetrics {

    private static final Logger log = LoggerFactory.getLogger(TaskExecutionMetrics.class);

    private static final String METRIC_EXECUTION_COUNT = "execution.count";
    private static final String METRIC_EXECUTION_DURATION = "execution.duration";
    private static final String METRIC_EXECUTION_ACTIVE = "execution.active";
    private static final String METRIC_EXECUTION_ERRORS = "execution.errors";
    private static final String METRIC_EXECUTION_RETRIES = "execution.retries";
    private static final String METRIC_EXECUTION_TIMEOUT = "execution.timeout";
    private static final String METRIC_EXECUTION_SKIPPED = "execution.skipped";

    private final MeterRegistry meterRegistry;
    private final SchedulerProperties.MetricsConfig config;

    private final ConcurrentMap<String, AtomicLong> activeExecutions = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Timer.Sample> activeTimers = new ConcurrentHashMap<>();

    /**
     * 创建任务执行监控实例
     *
     * @param meterRegistry Micrometer 注册表
     * @param config        监控配置
     */
    public TaskExecutionMetrics(@NonNull MeterRegistry meterRegistry,
                                SchedulerProperties.@NonNull MetricsConfig config) {
        this.meterRegistry = meterRegistry;
        this.config = config;
    }

    /**
     * 记录任务开始执行
     *
     * @param taskId    任务 ID
     * @param taskGroup 任务分组
     * @return 执行 ID
     */
    @NonNull
    public String recordStart(@NonNull String taskId, @NonNull String taskGroup) {
        String executionId = generateExecutionId(taskId);

        // 增加活跃执行数
        activeExecutions.computeIfAbsent(taskId, k -> new AtomicLong(0)).incrementAndGet();

        // 注册活跃执行数 Gauge
        Gauge.builder(metricName(METRIC_EXECUTION_ACTIVE), activeExecutions, map -> {
            AtomicLong count = map.get(taskId);
            return count != null ? count.get() : 0;
        })
        .tag("taskId", taskId)
        .tag("taskGroup", taskGroup)
        .register(meterRegistry);

        // 开始计时
        Timer.Sample sample = Timer.start(meterRegistry);
        activeTimers.put(executionId, sample);

        log.debug("Task execution started: taskId={}, executionId={}", taskId, executionId);
        return executionId;
    }

    /**
     * 记录任务执行成功
     *
     * @param executionId 执行 ID
     * @param taskId      任务 ID
     * @param taskGroup   任务分组
     */
    public void recordSuccess(@NonNull String executionId, @NonNull String taskId, @NonNull String taskGroup) {
        recordCompletion(executionId, taskId, taskGroup, "success");
        log.debug("Task execution succeeded: taskId={}, executionId={}", taskId, executionId);
    }

    /**
     * 记录任务执行失败
     *
     * @param executionId 执行 ID
     * @param taskId      任务 ID
     * @param taskGroup   任务分组
     * @param errorType   错误类型
     */
    public void recordFailure(@NonNull String executionId, @NonNull String taskId,
                              @NonNull String taskGroup, @NonNull String errorType) {
        recordCompletion(executionId, taskId, taskGroup, "failed");

        // 增加错误计数
        Counter.builder(metricName(METRIC_EXECUTION_ERRORS))
            .tag("taskId", taskId)
            .tag("taskGroup", taskGroup)
            .tag("errorType", errorType)
            .register(meterRegistry)
            .increment();

        log.debug("Task execution failed: taskId={}, executionId={}, errorType={}",
            taskId, executionId, errorType);
    }

    /**
     * 记录任务执行超时
     *
     * @param executionId 执行 ID
     * @param taskId      任务 ID
     * @param taskGroup   任务分组
     */
    public void recordTimeout(@NonNull String executionId, @NonNull String taskId, @NonNull String taskGroup) {
        recordCompletion(executionId, taskId, taskGroup, "timeout");

        // 增加超时计数
        Counter.builder(metricName(METRIC_EXECUTION_TIMEOUT))
            .tag("taskId", taskId)
            .tag("taskGroup", taskGroup)
            .register(meterRegistry)
            .increment();

        log.debug("Task execution timeout: taskId={}, executionId={}", taskId, executionId);
    }

    /**
     * 记录任务执行被跳过
     *
     * @param taskId    任务 ID
     * @param taskGroup 任务分组
     * @param reason    跳过原因
     */
    public void recordSkipped(@NonNull String taskId, @NonNull String taskGroup, @NonNull String reason) {
        Counter.builder(metricName(METRIC_EXECUTION_SKIPPED))
            .tag("taskId", taskId)
            .tag("taskGroup", taskGroup)
            .tag("reason", reason)
            .register(meterRegistry)
            .increment();

        log.debug("Task execution skipped: taskId={}, reason={}", taskId, reason);
    }

    /**
     * 记录任务重试
     *
     * @param taskId    任务 ID
     * @param taskGroup 任务分组
     * @param attempt   第几次重试
     */
    public void recordRetry(@NonNull String taskId, @NonNull String taskGroup, int attempt) {
        Counter.builder(metricName(METRIC_EXECUTION_RETRIES))
            .tag("taskId", taskId)
            .tag("taskGroup", taskGroup)
            .tag("attempt", String.valueOf(attempt))
            .register(meterRegistry)
            .increment();

        log.debug("Task execution retry: taskId={}, attempt={}", taskId, attempt);
    }

    /**
     * 记录自定义执行时间
     *
     * @param taskId    任务 ID
     * @param taskGroup 任务分组
     * @param duration  执行时间
     */
    public void recordDuration(@NonNull String taskId, @NonNull String taskGroup, @NonNull Duration duration) {
        Timer.builder(metricName(METRIC_EXECUTION_DURATION))
            .tag("taskId", taskId)
            .tag("taskGroup", taskGroup)
            .publishPercentiles(0.5, 0.95, 0.99)
            .publishPercentileHistogram(config.isRecordDurationHistogram())
            .register(meterRegistry)
            .record(duration);
    }

    private void recordCompletion(@NonNull String executionId, @NonNull String taskId,
                                  @NonNull String taskGroup, @NonNull String status) {
        // 减少活跃执行数
        AtomicLong activeCount = activeExecutions.get(taskId);
        if (activeCount != null) {
            activeCount.decrementAndGet();
        }

        // 停止计时并记录
        Timer.Sample sample = activeTimers.remove(executionId);
        if (sample != null) {
            Timer timer = Timer.builder(metricName(METRIC_EXECUTION_DURATION))
                .tag("taskId", taskId)
                .tag("taskGroup", taskGroup)
                .tag("status", status)
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram(config.isRecordDurationHistogram())
                .register(meterRegistry);
            sample.stop(timer);
        }

        // 增加执行计数
        Counter.builder(metricName(METRIC_EXECUTION_COUNT))
            .tag("taskId", taskId)
            .tag("taskGroup", taskGroup)
            .tag("status", status)
            .register(meterRegistry)
            .increment();
    }

    private String generateExecutionId(@NonNull String taskId) {
        return taskId + ":" + System.currentTimeMillis() + ":" + Thread.currentThread().getId();
    }

    private String metricName(@NonNull String name) {
        return config.getPrefix() + "." + name;
    }
}
