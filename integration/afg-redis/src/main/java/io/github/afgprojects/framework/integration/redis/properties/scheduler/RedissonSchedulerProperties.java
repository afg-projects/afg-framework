package io.github.afgprojects.framework.integration.redis.properties.scheduler;

import java.time.Duration;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * Redisson 任务调度配置属性
 */
@Data
@ConfigurationProperties(prefix = "afg.scheduler.redisson")
public class RedissonSchedulerProperties {

    private boolean enabled = true;
    private String executorName = "afg-scheduler";
    private int workerCount = Runtime.getRuntime().availableProcessors();
    private @Nullable Duration taskTimeout;
    private RedissonDelayQueueProperties delayQueue = new RedissonDelayQueueProperties();
    private RedissonRetryProperties retry = new RedissonRetryProperties();
}
