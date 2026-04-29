package io.github.afgprojects.framework.core.web.health;

import java.time.Duration;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * 健康检查配置属性
 * 支持不同级别的健康检查配置
 *
 * @since 1.0.0
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "afg.health")
@SuppressWarnings({"PMD.UncommentedEmptyConstructor", "PMD.FieldDeclarationsShouldBeAtStartOfClass"})
public class HealthCheckProperties {

    /**
     * 是否启用存活探针
     */
    private boolean livenessEnabled = true;

    /**
     * 是否启用就绪探针
     */
    private boolean readinessEnabled = true;

    /**
     * 是否启用深度检查
     */
    private boolean deepEnabled = true;

    /**
     * 存活探针详细配置
     */
    private final LivenessConfig liveness = new LivenessConfig();

    /**
     * 就绪探针详细配置
     */
    private final ReadinessConfig readiness = new ReadinessConfig();

    /**
     * 深度检查详细配置
     */
    private final DeepConfig deep = new DeepConfig();

    /**
     * 存活探针配置
     */
    @Getter
    @Setter
    public static class LivenessConfig {

        /**
         * 是否启用线程死锁检测
         */
        private boolean deadlockDetectionEnabled = true;

        /**
         * 死锁检测超时时间
         */
        private Duration deadlockDetectionTimeout = Duration.ofSeconds(5);

        /**
         * 是否检查 JVM 内存
         */
        private boolean memoryCheckEnabled = true;

        /**
         * 内存使用率告警阈值（百分比）
         */
        private int memoryWarningThreshold = 80;

        /**
         * 内存使用率严重阈值（百分比）
         */
        private int memoryCriticalThreshold = 95;
    }

    /**
     * 就绪探针配置
     */
    @Getter
    @Setter
    public static class ReadinessConfig {

        /**
         * 是否检查数据库连接
         */
        private boolean databaseCheckEnabled = true;

        /**
         * 数据库检查超时时间
         */
        private Duration databaseCheckTimeout = Duration.ofSeconds(3);

        /**
         * 是否检查 Redis 连接
         */
        private boolean redisCheckEnabled = true;

        /**
         * Redis 检查超时时间
         */
        private Duration redisCheckTimeout = Duration.ofSeconds(3);

        /**
         * 是否检查模块状态
         */
        private boolean moduleCheckEnabled = true;

        /**
         * 需要检查的模块ID集合（空表示检查所有模块）
         */
        private Set<String> requiredModules = Set.of();
    }

    /**
     * 深度检查配置
     */
    @Getter
    @Setter
    public static class DeepConfig {

        /**
         * 深度检查超时时间
         */
        private Duration timeout = Duration.ofSeconds(10);

        /**
         * 是否检查外部服务
         */
        private boolean externalServiceCheckEnabled = true;

        /**
         * 是否检查配置中心
         */
        private boolean configCenterCheckEnabled = true;

        /**
         * 外部服务检查超时时间
         */
        private Duration externalServiceTimeout = Duration.ofSeconds(5);
    }
}
