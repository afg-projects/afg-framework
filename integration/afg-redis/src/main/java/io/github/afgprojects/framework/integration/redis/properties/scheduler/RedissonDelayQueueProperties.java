package io.github.afgprojects.framework.integration.redis.properties.scheduler;

import lombok.Data;

/**
 * Redisson 延迟队列配置。
 */
@Data
public class RedissonDelayQueueProperties {

    private boolean enabled = true;
    private String name = "afg-delay-queue";
    private int consumerThreads = 2;
    private int batchSize = 10;
}
