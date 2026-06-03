package io.github.afgprojects.framework.ai.core.tool;

import io.github.afgprojects.framework.ai.core.api.tool.ToolContext;
import io.github.afgprojects.framework.ai.core.api.tool.ToolContextProvider;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 基于 Spring Security 的工具上下文提供者。
 *
 * <p>从 Spring Security SecurityContextHolder 中提取认证信息，
 * 转换为 ToolContext 供工具执行时使用。
 *
 * <p>当 Spring Security 不在 classpath 时，安全降级返回空上下文。
 *
 * @since 1.0.0
 */
@Slf4j
public class SecurityToolContextProvider implements ToolContextProvider {

    @Override
    public ToolContext provide() {
        try {
            var securityContext = org.springframework.security.core.context.SecurityContextHolder.getContext();
            var authentication = securityContext.getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()) {
                return ToolContext.empty();
            }

            var builder = ToolContext.builder()
                .userDetails(authentication.getPrincipal());

            // 提取用户 ID
            if (authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
                builder.userId(userDetails.getUsername());
            } else {
                builder.userId(authentication.getName());
            }

            // 提取权限列表转换为角色集合
            Set<String> roles = authentication.getAuthorities().stream()
                .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
            builder.roles(roles);

            return builder.build();

        } catch (NoClassDefFoundError e) {
            log.debug("Spring Security not available, returning empty tool context");
            return ToolContext.empty();
        } catch (Exception e) {
            log.debug("Failed to extract security context: {}", e.getMessage());
            return ToolContext.empty();
        }
    }
}
