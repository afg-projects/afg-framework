package io.github.afgprojects.framework.integration.redis.properties.lock;

import lombok.Data;

/**
 * 分布式锁注解配置。
 */
@Data
public class LockAnnotationProperties {

    private boolean enabled = true;
}
