package io.github.afgprojects.framework.core.properties.lock;

import lombok.Data;

/**
 * 分布式锁配置。
 */
@Data
public class AfgCoreLockProperties {

    /**
     * 是否启用分布式锁。
     */
    private boolean enabled = true;

    /**
     * 锁键前缀。
     */
    private String keyPrefix = "afg:lock";

    /**
     * 默认等待时间（毫秒）。
     */
    private long defaultWaitTime = 5000;

    /**
     * 默认持有时间（毫秒）。
     * -1 表示使用 watchdog 自动续期。
     */
    private long defaultLeaseTime = -1;

    /**
     * 注解相关配置。
     */
    private AfgCoreLockAnnotationProperties annotations = new AfgCoreLockAnnotationProperties();
}
