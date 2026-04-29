package io.github.afgprojects.framework.core.cloud;

import java.time.Duration;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * Kubernetes 探针配置属性
 *
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "afg.kubernetes.probe")
public class KubernetesProbeProperties {

    /**
     * 是否启用探针
     */
    private boolean enabled = true;

    /**
     * 存活探针配置
     */
    private ProbeConfig liveness = new ProbeConfig();

    /**
     * 就绪探针配置
     */
    private ProbeConfig readiness = new ProbeConfig();

    /**
     * 启动探针配置
     */
    private ProbeConfig startup = new ProbeConfig();

    /**
     * 探针配置
     */
    @Data
    public static class ProbeConfig {

        /**
         * 探针路径
         */
        private String path = "/health/default";

        /**
         * 初始延迟时间
         */
        private Duration initialDelay = Duration.ofSeconds(10);

        /**
         * 检查间隔
         */
        private Duration period = Duration.ofSeconds(10);

        /**
         * 超时时间
         */
        private Duration timeout = Duration.ofSeconds(5);

        /**
         * 成功阈值
         */
        private int successThreshold = 1;

        /**
         * 失败阈值
         */
        private int failureThreshold = 3;
    }
}