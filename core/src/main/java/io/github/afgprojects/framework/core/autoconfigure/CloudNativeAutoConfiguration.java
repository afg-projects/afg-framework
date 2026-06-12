package io.github.afgprojects.framework.core.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import io.github.afgprojects.framework.core.config.AfgCoreProperties;
import io.github.afgprojects.framework.core.cloud.GracefulShutdownManager;

/**
 * 云原生自动配置
 *
 * <p>提供 Kubernetes 环境下的云原生特性支持
 *
 * <h3>功能特性</h3>
 * <ul>
 *   <li>优雅停机</li>
 *   <li>配置外部化</li>
 *   <li>服务发现集成</li>
 * </ul>
 *
 * <p>Kubernetes 探针支持由 {@link KubernetesProbeAutoConfiguration} 独立提供，
 * 通过 {@code afg.core.cloud-native.kubernetes.enabled} 控制开关。
 *
 * <h3>配置示例</h3>
 * <pre>
 * afg:
 *   core:
 *     cloud-native:
 *       graceful-shutdown:
 *         enabled: true
 *         timeout: 30s
 *       config-externalization:
 *         enabled: true
 *         config-map: ${CONFIG_MAP_NAME:app-config}
 * </pre>
 *
 * @since 1.0.0
 */
@AutoConfiguration(after = AfgAutoConfiguration.class)
@ConditionalOnProperty(prefix = "afg.core.cloud-native", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(AfgCoreProperties.class)
public class CloudNativeAutoConfiguration {

    /**
     * 优雅停机生命周期管理器
     *
     * @param properties 核心配置属性
     * @return 优雅停机管理器实例
     */
    @Bean
    @ConditionalOnMissingBean
    public GracefulShutdownManager gracefulShutdownManager(AfgCoreProperties properties) {
        return new GracefulShutdownManager(properties.getCloudNative().getGracefulShutdown());
    }
}