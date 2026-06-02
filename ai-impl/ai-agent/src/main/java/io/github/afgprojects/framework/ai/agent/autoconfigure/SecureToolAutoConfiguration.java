package io.github.afgprojects.framework.ai.agent.autoconfigure;

import io.github.afgprojects.framework.ai.core.autoconfigure.AiConfigurationProperties;
import io.github.afgprojects.framework.ai.agent.executor.SecureToolExecutor;
import io.github.afgprojects.framework.ai.agent.tool.audit.JdbcToolAuditLogger;
import io.github.afgprojects.framework.ai.core.api.chat.AfgChatClient;
import io.github.afgprojects.framework.ai.core.api.security.ContentSafetyChecker;
import io.github.afgprojects.framework.ai.core.api.tool.*;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * 安全工具自动配置。
 *
 * <p>配置安全工具执行所需的组件（不依赖 security-core）：
 * <ul>
 *   <li>ToolContextProvider - 默认空上下文提供者（无 Security 上下文时）</li>
 *   <li>ToolAuditLogger - 审计日志器</li>
 *   <li>SecureToolExecutor - 安全工具执行器</li>
 * </ul>
 *
 * <p>依赖 security-core 的组件（ToolContextProvider、ToolPermissionChecker）
 * 由 {@code ai-agent-security} 模块提供。
 *
 * <p>配置示例：
 * <pre>{@code
 * afg:
 *   ai:
 *     tool:
 *       security:
 *         enabled: true
 *         audit:
 *           enabled: true
 *         executor:
 *           max-iterations: 10
 *           timeout-ms: 30000
 * }</pre>
 *
 * @since 1.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties({AiConfigurationProperties.class, SecureToolProperties.class})
@ConditionalOnClass({ToolRegistry.class, ToolContext.class})
@ConditionalOnProperty(prefix = "afg.ai.tool.security", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SecureToolAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(SecureToolAutoConfiguration.class);

    /**
     * 配置审计日志器。
     *
     * <p>基于 JDBC 的审计日志存储。
     */
    @Bean
    @ConditionalOnBean(JdbcClient.class)
    @ConditionalOnProperty(prefix = "afg.ai.tool.security.audit", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(ToolAuditLogger.class)
    public ToolAuditLogger toolAuditLogger(@Autowired JdbcClient jdbcClient) {
        log.info("Creating JDBC tool audit logger");

        return new JdbcToolAuditLogger(jdbcClient);
    }

    /**
     * 配置安全工具执行器。
     *
     * <p>集成权限校验、审计日志、内容安全检查、执行记录。
     */
    @Bean
    @ConditionalOnBean({ToolRegistry.class, AfgChatClient.class, ToolContextProvider.class})
    @ConditionalOnMissingBean(SecureToolExecutor.class)
    public SecureToolExecutor secureToolExecutor(
            @Autowired ToolRegistry toolRegistry,
            @Autowired io.github.afgprojects.framework.ai.core.api.chat.AfgChatClient chatClient,
            @Autowired ToolContextProvider contextProvider,
            @Autowired(required = false) @Nullable ToolPermissionChecker permissionChecker,
            @Autowired(required = false) @Nullable ToolAuditLogger auditLogger,
            @Autowired(required = false) @Nullable ContentSafetyChecker contentSafetyChecker,
            @Autowired(required = false) @Nullable ToolExecutionRecorder executionRecorder,
            @Autowired SecureToolProperties properties) {

        log.info("Creating secure tool executor with maxIterations={}, timeoutMs={}",
            properties.getMaxIterations(), properties.getTimeoutMs());

        return new SecureToolExecutor(
            toolRegistry,
            chatClient,
            contextProvider,
            permissionChecker,
            auditLogger,
            contentSafetyChecker,
            executionRecorder,
            properties.getMaxIterations(),
            properties.getTimeoutMs()
        );
    }

    /**
     * 默认工具上下文提供者（无 Security 上下文）。
     *
     * <p>当没有 AfgSecurityContext Bean 且没有其他 ToolContextProvider 时生效。
     * 如果引入了 ai-agent-security 模块且存在 AfgSecurityContext，
     * 则由 AgentSecurityAutoConfiguration 提供基于安全上下文的 ToolContextProvider。
     */
    @Bean
    @ConditionalOnMissingBean(ToolContextProvider.class)
    public ToolContextProvider defaultToolContextProvider() {
        log.info("Creating default (empty) tool context provider");

        return ToolContextProvider.empty();
    }
}
