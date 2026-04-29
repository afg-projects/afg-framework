package io.github.afgprojects.framework.core.cloud;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 云原生配置属性
 *
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "afg.cloud-native")
public class CloudNativeProperties {

    /**
     * Kubernetes 配置
     */
    private KubernetesConfig kubernetes = new KubernetesConfig();

    /**
     * 优雅停机配置
     */
    private GracefulShutdownConfig gracefulShutdown = new GracefulShutdownConfig();

    /**
     * 配置外部化
     */
    private ConfigExternalizationConfig configExternalization = new ConfigExternalizationConfig();

    /**
     * Kubernetes 配置
     */
    @Data
    public static class KubernetesConfig {

        /**
         * 是否启用 Kubernetes 支持
         */
        private boolean enabled = true;

        /**
         * 命名空间
         */
        private @Nullable String namespace;

        /**
         * 服务账户
         */
        private @Nullable String serviceAccount;

        /**
         * Pod 名称
         */
        private @Nullable String podName;

        /**
         * Pod IP
         */
        private @Nullable String podIp;

        /**
         * 节点名称
         */
        private @Nullable String nodeName;
    }

    /**
     * 优雅停机配置
     */
    @Data
    public static class GracefulShutdownConfig {

        /**
         * 是否启用优雅停机
         */
        private boolean enabled = true;

        /**
         * 停机超时时间
         */
        private Duration timeout = Duration.ofSeconds(30);

        /**
         * 是否等待请求完成
         */
        private boolean waitForRequests = true;

        /**
         * 请求完成等待超时
         */
        private Duration requestWaitTimeout = Duration.ofSeconds(10);
    }

    /**
     * 配置外部化配置
     */
    @Data
    public static class ConfigExternalizationConfig {

        /**
         * 是否启用配置外部化
         */
        private boolean enabled = true;

        /**
         * ConfigMap 名称
         */
        private @Nullable String configMap;

        /**
         * Secret 名称
         */
        private @Nullable String secret;

        /**
         * 是否自动刷新
         */
        private boolean autoRefresh = true;

        /**
         * 刷新间隔
         */
        private Duration refreshInterval = Duration.ofMinutes(1);
    }
}