package io.github.afgprojects.framework.core.cloud;

import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.afgprojects.framework.core.cloud.KubernetesProbeProperties;

/**
 * Kubernetes 探针自动配置
 *
 * <p>为 Kubernetes 环境提供健康检查探针支持
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
 *   kubernetes:
 *     probes:
 *       enabled: true
 *       liveness:
 *         path: /health/liveness
 *         initial-delay: 30s
 *         period: 10s
 *       readiness:
 *         path: /health/readiness
 *         initial-delay: 5s
 *         period: 5s
 *       startup:
 *         path: /health/startup
 *         initial-delay: 0s
 *         period: 2s
 *         failure-threshold: 30
 * </pre>
 *
 * @since 1.0.0
 */
@Configuration
@EnableConfigurationProperties(KubernetesProbeProperties.class)
public class KubernetesProbeAutoConfiguration {

    /**
     * 存活探针端点
     */
    @Bean
    public LivenessProbeEndpoint livenessProbeEndpoint(KubernetesProbeProperties properties) {
        return new LivenessProbeEndpoint(properties.getLiveness());
    }

    /**
     * 就绪探针端点
     */
    @Bean
    public ReadinessProbeEndpoint readinessProbeEndpoint(KubernetesProbeProperties properties) {
        return new ReadinessProbeEndpoint(properties.getReadiness());
    }

    /**
     * 启动探针端点
     */
    @Bean
    public StartupProbeEndpoint startupProbeEndpoint(KubernetesProbeProperties properties) {
        return new StartupProbeEndpoint(properties.getStartup());
    }
}