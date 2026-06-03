package io.github.afgprojects.framework.integration.redis.properties.lock;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Redis 分布式锁配置属性
 */
@Data
@ConfigurationProperties(prefix = "afg.redis.lock")
public class LockProperties {

    private boolean enabled = true;
    private String keyPrefix = "afg:lock";
    private long defaultWaitTime = 5000;
    private long defaultLeaseTime = -1;
    private LockAnnotationProperties annotations = new LockAnnotationProperties();
}
