package io.github.afgprojects.framework.ai.autoconfigure;

import io.github.afgprojects.framework.ai.agent.executor.SecureToolExecutor;
import io.github.afgprojects.framework.ai.agent.tool.audit.JdbcToolAuditLogger;
import io.github.afgprojects.framework.ai.agent.tool.security.CasbinToolPermissionChecker;
import io.github.afgprojects.framework.ai.core.security.ContentSafetyChecker;
import io.github.afgprojects.framework.ai.core.tool.*;
import io.github.afgprojects.framework.security.core.authorization.AfgSecurityContext;
import io.github.afgprojects.framework.security.core.permission.PermissionService;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * 安全工具自动配置。
 *
 * <p>配置安全工具执行所需的组件：
 * <ul>
 *   <li>ToolContextProvider - 工具上下文提供者</li>
 *   <li>ToolPermissionChecker - 权限检查器</li>
 *   <li>ToolAuditLogger - 审计日志器</li>
 *   <li>SecureToolExecutor - 安全工具执行器</li>
 * </ul>
 *
 * <p>配置示例：
 * <pre>{@code
 * afg:
 *   ai:
 *     tool:
 *       security:
 *         enabled: true
 *         permission-checker:
 *           enabled: true
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
     * 配置工具上下文提供者。
     *
     * <p>从 Spring Security 上下文获取当前用户信息。
     */
    @Bean
    @ConditionalOnClass(AfgSecurityContext.class)
    @ConditionalOnMissingBean(ToolContextProvider.class)
    public ToolContextProvider toolContextProvider(@Autowired AfgSecurityContext securityContext) {
        log.info("Creating tool context provider from AfgSecurityContext");

        return () -> {
            var auth = securityContext.getAuthentication();
            if (auth == null) {
                return ToolContext.empty();
            }

            // 获取用户详情
            Object principal = auth.getPrincipal();
            if (principal instanceof io.github.afgprojects.framework.security.core.authentication.AfgUserDetails user) {
                return ToolContext.builder()
                    .userId(user.getUserId())
                    .userDetails(user)
                    .tenantId(user.getTenantId())
                    .organizationId(user.getOrganizationId())
                    .build();
            }

            // 其他情况，从认证信息中提取
            return ToolContext.builder()
                .userId(auth.getName())
                .build();
        };
    }

    /**
     * 配置权限检查器。
     *
     * <p>基于 Casbin 的权限检查实现。
     */
    @Bean
    @ConditionalOnClass(PermissionService.class)
    @ConditionalOnProperty(prefix = "afg.ai.tool.security.permission-checker", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(ToolPermissionChecker.class)
    public ToolPermissionChecker toolPermissionChecker(@Autowired PermissionService permissionService) {
        log.info("Creating Casbin tool permission checker");

        return new CasbinToolPermissionChecker(permissionService);
    }

    /**
     * 配置审计日志器。
     *
     * <p>基于 JDBC 的审计日志存储。
     */
    @Bean
    @ConditionalOnClass(JdbcClient.class)
    @ConditionalOnProperty(prefix = "afg.ai.tool.security.audit", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(ToolAuditLogger.class)
    public ToolAuditLogger toolAuditLogger(@Autowired JdbcClient jdbcClient) {
        log.info("Creating JDBC tool audit logger");

        return new JdbcToolAuditLogger(jdbcClient);
    }

    /**
     * 配置安全工具执行器。
     *
     * <p>集成权限校验、审计日志、内容安全检查。
     */
    @Bean
    @ConditionalOnMissingBean(SecureToolExecutor.class)
    public SecureToolExecutor secureToolExecutor(
            @Autowired ToolRegistry toolRegistry,
            @Autowired io.github.afgprojects.framework.ai.core.chat.AfgChatClient chatClient,
            @Autowired ToolContextProvider contextProvider,
            @Autowired(required = false) @Nullable ToolPermissionChecker permissionChecker,
            @Autowired(required = false) @Nullable ToolAuditLogger auditLogger,
            @Autowired(required = false) @Nullable ContentSafetyChecker contentSafetyChecker,
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
            properties.getMaxIterations(),
            properties.getTimeoutMs()
        );
    }

    /**
     * 默认工具上下文提供者（无 Security 上下文）。
     */
    @Bean
    @ConditionalOnMissingBean({ToolContextProvider.class, AfgSecurityContext.class})
    public ToolContextProvider defaultToolContextProvider() {
        log.info("Creating default (empty) tool context provider");

        return ToolContextProvider.empty();
    }
}