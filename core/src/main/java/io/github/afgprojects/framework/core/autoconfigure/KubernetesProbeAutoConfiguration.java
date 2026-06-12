package io.github.afgprojects.framework.core.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import io.github.afgprojects.framework.core.config.AfgCoreProperties;
import io.github.afgprojects.framework.core.cloud.LivenessProbeEndpoint;
import io.github.afgprojects.framework.core.cloud.ReadinessProbeEndpoint;
import io.github.afgprojects.framework.core.cloud.StartupProbeEndpoint;

/**
 * Kubernetes 探针自动配置
 *
 * <p>为 Kubernetes 环境提供健康检查探针支持
 *
 * <h3>启用条件</h3>
 * <ul>
 *   <li>{@code afg.core.cloud-native.kubernetes.enabled=true}（默认为 true）</li>
 * </ul>
 *
 * <h3>探针类型</h3>
 * <ul>
 *   <li>Liveness Probe - 存活探针，检测应用是否运行</li>
 *   <li>Readiness Probe - 就绪探针，检测应用是否准备好接收流量</li>
 *   <li>Startup Probe - 启动探针，检测应用是否启动完成</li>
 * </ul>
 *
 * <h3>配置示例</h3>
 * <pre>
 * afg:
 *   core:
 *     cloud-native:
 *       kubernetes:
 *         enabled: true
 *       probe:
 *         liveness:
 *           path: /health/liveness
 *           initial-delay: 30s
 *           period: 10s
 *         readiness:
 *           path: /health/readiness
 *           initial-delay: 5s
 *           period: 5s
 *         startup:
 *           path: /health/startup
 *           initial-delay: 0s
 *           period: 2s
 *           failure-threshold: 30
 * </pre>
 *
 * @since 1.0.0
 */
@AutoConfiguration(after = CloudNativeAutoConfiguration.class)
@ConditionalOnProperty(prefix = "afg.core.cloud-native.kubernetes", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(AfgCoreProperties.class)
public class KubernetesProbeAutoConfiguration {

    /**
     * 存活探针端点
     *
     * @param properties 核心配置属性
     * @return 存活探针端点实例
     */
    @Bean
    @ConditionalOnMissingBean
    public LivenessProbeEndpoint livenessProbeEndpoint(AfgCoreProperties properties) {
        return new LivenessProbeEndpoint(properties.getCloudNative().getProbe().getLiveness());
    }

    /**
     * 就绪探针端点
     *
     * @param properties 核心配置属性
     * @return 就绪探针端点实例
     */
    @Bean
    @ConditionalOnMissingBean
    public ReadinessProbeEndpoint readinessProbeEndpoint(AfgCoreProperties properties) {
        return new ReadinessProbeEndpoint(properties.getCloudNative().getProbe().getReadiness());
    }

    /**
     * 启动探针端点
     *
     * @param properties 核心配置属性
     * @return 启动探针端点实例
     */
    @Bean
    @ConditionalOnMissingBean
    public StartupProbeEndpoint startupProbeEndpoint(AfgCoreProperties properties) {
        return new StartupProbeEndpoint(properties.getCloudNative().getProbe().getStartup());
    }
}