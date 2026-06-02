package io.github.afgprojects.framework.ai.agent.security;

import io.github.afgprojects.framework.ai.core.api.tool.ToolContext;
import io.github.afgprojects.framework.ai.core.api.tool.ToolContextProvider;
import io.github.afgprojects.framework.ai.core.api.tool.ToolPermissionChecker;
import io.github.afgprojects.framework.security.core.authentication.AfgUserDetails;
import io.github.afgprojects.framework.security.core.authorization.AfgSecurityContext;
import io.github.afgprojects.framework.security.core.permission.PermissionService;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Agent 安全自动配置。
 *
 * <p>配置依赖 security-core 的安全组件：
 * <ul>
 *   <li>ToolContextProvider - 从 AfgSecurityContext 获取工具上下文</li>
 *   <li>ToolPermissionChecker - 基于 Casbin 的权限检查器</li>
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
 * }</pre>
 *
 * @since 1.0.0
 */
@AutoConfiguration
@ConditionalOnClass({AfgSecurityContext.class, PermissionService.class})
@ConditionalOnProperty(prefix = "afg.ai.tool.security", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AgentSecurityAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(AgentSecurityAutoConfiguration.class);

    /**
     * 配置工具上下文提供者。
     *
     * <p>从 Spring Security 上下文获取当前用户信息。
     */
    @Bean
    @ConditionalOnBean(AfgSecurityContext.class)
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
            if (principal instanceof AfgUserDetails user) {
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
    @ConditionalOnBean(PermissionService.class)
    @ConditionalOnProperty(prefix = "afg.ai.tool.security.permission-checker", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(ToolPermissionChecker.class)
    public ToolPermissionChecker toolPermissionChecker(@Autowired PermissionService permissionService) {
        log.info("Creating Casbin tool permission checker");

        return new CasbinToolPermissionChecker(permissionService);
    }
}
