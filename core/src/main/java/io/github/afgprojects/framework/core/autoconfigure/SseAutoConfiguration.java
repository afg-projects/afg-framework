package io.github.afgprojects.framework.core.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import io.github.afgprojects.framework.core.api.sse.LocalSseConnectionManager;
import io.github.afgprojects.framework.core.api.sse.NoOpSseConnectionManager;
import io.github.afgprojects.framework.core.api.sse.SseConnectionManager;
import io.github.afgprojects.framework.core.config.AfgCoreProperties;

/**
 * SSE 自动配置
 * <p>
 * 自动配置 SSE（Server-Sent Events）连接管理功能，支持多种连接管理后端。
 * 默认使用本地内存连接管理（{@link LocalSseConnectionManager}），仅对单实例有效。
 * </p>
 * <p>
 * 配置示例：
 * <pre>
 * afg:
 *   core:
 *     sse:
 *       enabled: true
 *       timeout: 300000
 *       max-connections: 1000
 *       heartbeat-interval: 30000
 * </pre>
 * </p>
 * <p>
 * 仅在 SERVLET Web 环境下生效（需要 {@link SseEmitter} 类存在）。
 * </p>
 *
 * @since 1.0.0
 */
@AutoConfiguration(after = AfgAutoConfiguration.class)
@ConditionalOnClass(SseEmitter.class)
@ConditionalOnProperty(prefix = "afg.core.sse", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(AfgCoreProperties.class)
public class SseAutoConfiguration {

    /**
     * 本地内存 SSE 连接管理器
     * <p>
     * 当没有 Redis 等分布式 SSE 后端时，提供基于 ConcurrentHashMap 的本地连接管理。
     * 仅对单实例有效，多实例部署时需引入分布式实现。
     *
     * @param properties 核心配置属性
     * @return 本地 SSE 连接管理器实例
     */
    @Bean
    @ConditionalOnMissingBean(SseConnectionManager.class)
    public SseConnectionManager localSseConnectionManager(AfgCoreProperties properties) {
        return new LocalSseConnectionManager(properties);
    }

    /**
     * NoOp SSE 连接管理器降级实现
     * <p>
     * 当不需要任何 SSE 功能时使用。所有连接管理操作均为空操作。
     * <p>
     * 注意：此 Bean 仅在 {@link LocalSseConnectionManager} 也不满足条件时才会被创建，
     * 正常情况下 {@link LocalSseConnectionManager} 作为默认实现已足够。
     *
     * @return NoOp SSE 连接管理器实例
     */
    @Bean
    @ConditionalOnMissingBean(SseConnectionManager.class)
    public SseConnectionManager noOpSseConnectionManager() {
        return new NoOpSseConnectionManager();
    }
}
