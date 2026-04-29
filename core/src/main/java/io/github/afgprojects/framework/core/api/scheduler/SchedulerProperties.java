package io.github.afgprojects.framework.core.api.scheduler;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * 任务调度器配置属性
 *
 * <p>配置示例：
 * <pre>{@code
 * afg:
 *   scheduler:
 *     enabled: true
 *     default-timeout: 30m
 *     default-retry-attempts: 3
 *     default-retry-delay: 1s
 *     log-storage:
 *       type: memory
 *       max-size: 10000
 *       retention: 7d
 *     metrics:
 *       enabled: true
 *       tags:
 *         application: ${spring.application.name}
 * }</pre>
 */
@Data
@ConfigurationProperties(prefix = "afg.scheduler")
public class SchedulerProperties {

    /**
     * 是否启用调度器
     */
    private boolean enabled = true;

    /**
     * 默认任务超时时间
     */
    private Duration defaultTimeout = Duration.ofMinutes(30);

    /**
     * 默认重试次数
     */
    private int defaultRetryAttempts = 3;

    /**
     * 默认重试延迟
     */
    private Duration defaultRetryDelay = Duration.ofSeconds(1);

    /**
     * 重试延迟倍数（指数退避）
     */
    private double retryMultiplier = 2.0;

    /**
     * 任务线程池大小
     */
    private int threadPoolSize = Runtime.getRuntime().availableProcessors();

    /**
     * 任务执行日志存储配置
     */
    private LogStorageConfig logStorage = new LogStorageConfig();

    /**
     * 监控指标配置
     */
    private MetricsConfig metrics = new MetricsConfig();

    /**
     * 动态任务配置
     */
    private DynamicTaskConfig dynamicTask = new DynamicTaskConfig();

    /**
     * 注解处理配置
     */
    private AnnotationConfig annotations = new AnnotationConfig();

    /**
     * 管理 API 配置
     */
    private ApiConfig api = new ApiConfig();

    /**
     * 执行日志存储配置
     */
    @Data
    public static class LogStorageConfig {

        /**
         * 存储类型：memory, redis, jdbc
         */
        private String type = "memory";

        /**
         * 最大存储数量（仅内存模式有效）
         */
        private int maxSize = 10000;

        /**
         * 日志保留时间
         */
        private Duration retention = Duration.ofDays(7);

        /**
         * 是否记录成功执行的日志
         */
        private boolean logSuccess = true;

        /**
         * 是否记录错误堆栈
         */
        private boolean logErrorStack = true;
    }

    /**
     * 监控指标配置
     */
    @Data
    public static class MetricsConfig {

        /**
         * 是否启用指标收集
         */
        private boolean enabled = true;

        /**
         * 指标名称前缀
         */
        private String prefix = "afg.scheduler";

        /**
         * 额外标签
         */
        private Map<String, String> tags = new HashMap<>();

        /**
         * 是否记录执行时间分布
         */
        private boolean recordDurationHistogram = true;
    }

    /**
     * 动态任务配置
     */
    @Data
    public static class DynamicTaskConfig {

        /**
         * 是否启用动态任务配置
         */
        private boolean enabled = false;

        /**
         * 配置源类型：config-center, jdbc
         */
        private String sourceType = "config-center";

        /**
         * 配置刷新间隔
         */
        private Duration refreshInterval = Duration.ofMinutes(1);

        /**
         * 配置前缀
         */
        private String configPrefix = "afg.tasks";
    }

    /**
     * 注解处理配置
     */
    @Data
    public static class AnnotationConfig {

        /**
         * 是否启用注解处理
         */
        private boolean enabled = true;
    }

    /**
     * 管理 API 配置
     */
    @Data
    public static class ApiConfig {

        /**
         * 是否启用管理 API
         */
        private boolean enabled = false;

        /**
         * API 基础路径
         */
        private String basePath = "/afg/scheduler";
    }
}
