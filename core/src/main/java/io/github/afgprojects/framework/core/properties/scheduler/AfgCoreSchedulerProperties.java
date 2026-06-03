package io.github.afgprojects.framework.core.properties.scheduler;

import java.time.Duration;

import lombok.Data;

/**
 * 调度器配置。
 */
@Data
public class AfgCoreSchedulerProperties {

    /**
     * 是否启用调度器。
     */
    private boolean enabled = true;

    /**
     * 默认任务超时时间。
     */
    private Duration defaultTimeout = Duration.ofMinutes(30);

    /**
     * 默认重试次数。
     */
    private int defaultRetryAttempts = 3;

    /**
     * 默认重试延迟。
     */
    private Duration defaultRetryDelay = Duration.ofSeconds(1);

    /**
     * 重试延迟倍数。
     */
    private double retryMultiplier = 2.0;

    /**
     * 任务线程池大小。
     */
    private int threadPoolSize = Runtime.getRuntime().availableProcessors();

    /**
     * 任务执行日志存储配置。
     */
    private AfgCoreSchedulerLogStorageProperties logStorage = new AfgCoreSchedulerLogStorageProperties();

    /**
     * 监控指标配置。
     */
    private AfgCoreSchedulerMetricsProperties metrics = new AfgCoreSchedulerMetricsProperties();

    /**
     * 动态任务配置。
     */
    private AfgCoreSchedulerDynamicTaskProperties dynamicTask = new AfgCoreSchedulerDynamicTaskProperties();

    /**
     * 注解处理配置。
     */
    private AfgCoreSchedulerAnnotationProperties annotations = new AfgCoreSchedulerAnnotationProperties();

    /**
     * 管理 API 配置。
     */
    private AfgCoreSchedulerApiProperties api = new AfgCoreSchedulerApiProperties();
}
