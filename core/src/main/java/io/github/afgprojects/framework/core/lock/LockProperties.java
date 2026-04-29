package io.github.afgprojects.framework.core.lock;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 分布式锁配置属性
 * <p>
 * 配置前缀：afg.lock
 * </p>
 *
 * <pre>
 * afg:
 *   lock:
 *     enabled: true
 *     key-prefix: "afg:lock"
 *     default-wait-time: 5000
 *     default-lease-time: -1
 *     annotations:
 *       enabled: true
 * </pre>
 */
@Data
@ConfigurationProperties(prefix = "afg.lock")
public class LockProperties {

    /**
     * 是否启用分布式锁
     */
    private boolean enabled = true;

    /**
     * 锁键前缀
     * <p>
     * 用于区分不同应用的锁，避免冲突
     * </p>
     */
    private String keyPrefix = "afg:lock";

    /**
     * 默认等待时间（毫秒）
     * <p>
     * 获取锁的最大等待时间
     * </p>
     */
    private long defaultWaitTime = 5000;

    /**
     * 默认持有时间（毫秒）
     * <p>
     * -1 表示使用 watchdog 自动续期
     * </p>
     */
    private long defaultLeaseTime = -1;

    /**
     * 注解相关配置
     */
    private AnnotationConfig annotations = new AnnotationConfig();

    /**
     * 注解配置
     */
    @Data
    public static class AnnotationConfig {

        /**
         * 是否启用注解支持
         */
        private boolean enabled = true;
    }
}
