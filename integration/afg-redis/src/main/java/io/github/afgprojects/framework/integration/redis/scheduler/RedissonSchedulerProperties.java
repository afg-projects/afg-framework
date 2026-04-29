package io.github.afgprojects.framework.integration.redis.scheduler;

import java.time.Duration;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * Redisson 任务调度配置属性
 *
 * <p>配置示例：
 * <pre>{@code
 * afg:
 *   scheduler:
 *     redisson:
 *       enabled: true
 *       executor-name: "afg-scheduler"
 *       worker-count: 4
 *       task-timeout: 30m
 *       delay-queue:
 *         enabled: true
 *         name: "afg-delay-queue"
 * }</pre>
 */
@Data
@ConfigurationProperties(prefix = "afg.scheduler.redisson")
public class RedissonSchedulerProperties {

    /**
     * 是否启用调度器
     */
    private boolean enabled = true;

    /**
     * 执行器名称（用于 Redisson RScheduledExecutorService）
     */
    private String executorName = "afg-scheduler";

    /**
     * 工作线程数
     */
    private int workerCount = Runtime.getRuntime().availableProcessors();

    /**
     * 任务执行超时时间
     */
    private @Nullable Duration taskTimeout;

    /**
     * 延迟队列配置
     */
    private DelayQueueConfig delayQueue = new DelayQueueConfig();

    /**
     * 重试配置
     */
    private RetryConfig retry = new RetryConfig();

    /**
     * 延迟队列配置
     */
    @Data
    public static class DelayQueueConfig {

        /**
         * 是否启用延迟队列
         */
        private boolean enabled = true;

        /**
         * 延迟队列名称
         */
        private String name = "afg-delay-queue";

        /**
         * 消费者线程数
         */
        private int consumerThreads = 2;

        /**
         * 批量获取大小
         */
        private int batchSize = 10;
    }

    /**
     * 重试配置
     */
    @Data
    public static class RetryConfig {

        /**
         * 是否启用重试
         */
        private boolean enabled = true;

        /**
         * 最大重试次数
         */
        private int maxAttempts = 3;

        /**
         * 初始重试延迟
         */
        private Duration initialDelay = Duration.ofSeconds(1);

        /**
         * 重试延迟倍数
         */
        private double multiplier = 2.0;
    }
}
