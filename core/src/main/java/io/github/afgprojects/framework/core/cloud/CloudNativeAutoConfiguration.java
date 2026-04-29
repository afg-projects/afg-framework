package io.github.afgprojects.framework.core.cloud;

import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.afgprojects.framework.core.cloud.CloudNativeProperties;

/**
 * 云原生自动配置
 *
 * <p>提供 Kubernetes 环境下的云原生特性支持
 *
 * <h3>功能特性</h3>
 * <ul>
 *   <li>Kubernetes 探针支持</li>
 *   <li>优雅停机</li>
 *   <li>配置外部化</li>
 *   <li>服务发现集成</li>
 * </ul>
 *
 * <h3>配置示例</h3>
 * <pre>
 * afg:
 *   cloud:
 *     kubernetes:
 *       enabled: true
 *       namespace: ${KUBERNETES_NAMESPACE:default}
 *       service-account: ${KUBERNETES_SERVICE_ACCOUNT:}
 *     graceful-shutdown:
 *       enabled: true
 *       timeout: 30s
 *     config-externalization:
 *       enabled: true
 *       config-map: ${CONFIG_MAP_NAME:app-config}
 * </pre>
 *
 * @since 1.0.0
 */
@Configuration
@EnableConfigurationProperties(CloudNativeProperties.class)
public class CloudNativeAutoConfiguration {

    /**
     * Kubernetes 探针自动配置
     */
    @Bean
    public KubernetesProbeAutoConfiguration kubernetesProbeAutoConfiguration(
            CloudNativeProperties properties) {
        if (properties.getKubernetes().isEnabled()) {
            return new KubernetesProbeAutoConfiguration();
        }
        return null;
    }

    /**
     * 优雅停机生命周期管理器
     */
    @Bean
    public GracefulShutdownManager gracefulShutdownManager(CloudNativeProperties properties) {
        return new GracefulShutdownManager(properties.getGracefulShutdown());
    }
}