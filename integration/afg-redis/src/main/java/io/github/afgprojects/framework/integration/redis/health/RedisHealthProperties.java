package io.github.afgprojects.framework.integration.redis.health;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * Redis 健康检查配置属性
 * 配置 Redis 连接健康检查参数
 *
 * @since 1.0.0
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "afg.health.redis")
@SuppressWarnings({"PMD.UncommentedEmptyConstructor", "PMD.FieldDeclarationsShouldBeAtStartOfClass"})
public class RedisHealthProperties {

    /**
     * 是否启用 Redis 健康检查
     */
    private boolean enabled = true;

    /**
     * 连接超时时间（毫秒）
     * 用于验证 Redis 是否可用
     */
    private long connectionTimeout = 3000;

    /**
     * 是否包含服务器详细信息
     * 包括版本、内存使用等
     */
    private boolean includeServerInfo = true;

    /**
     * 响应时间警告阈值（毫秒）
     * 超过此阈值将报告 WARNING 状态
     */
    private long responseTimeWarningThreshold = 1000;

    /**
     * 响应时间严重阈值（毫秒）
     * 超过此阈值将报告 CRITICAL 状态
     */
    private long responseTimeCriticalThreshold = 3000;
}
