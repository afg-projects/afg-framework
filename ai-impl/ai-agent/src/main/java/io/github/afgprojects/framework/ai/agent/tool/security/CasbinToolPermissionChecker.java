package io.github.afgprojects.framework.ai.agent.tool.security;

import io.github.afgprojects.framework.ai.core.tool.*;
import io.github.afgprojects.framework.security.core.permission.PermissionService;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

/**
 * 基于 Casbin 的工具权限检查器。
 *
 * <p>使用 {@link PermissionService} 检查用户是否有权限调用工具。
 *
 * <p>检查逻辑：
 * <ol>
 *   <li>如果上下文未认证，拒绝</li>
 *   <li>如果用户是管理员，允许</li>
 *   <li>如果工具是 {@link SecureTool}：
 *     <ul>
 *       <li>检查 requiredRoles() - 满足任一角色即可</li>
 *       <li>检查 requiredPermission() - 必须具有该权限</li>
 *     </ul>
 *   </li>
 *   <li>默认允许（非 SecureTool）</li>
 * </ol>
 *
 * @since 1.0.0
 */
public class CasbinToolPermissionChecker implements ToolPermissionChecker {

    private static final Logger log = LoggerFactory.getLogger(CasbinToolPermissionChecker.class);

    private final PermissionService permissionService;

    /**
     * 创建权限检查器。
     *
     * @param permissionService 权限服务
     */
    public CasbinToolPermissionChecker(@NonNull PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Override
    public @NonNull PermissionResult check(
            @NonNull String toolName,
            @NonNull ToolContext context) {
        // 1. 检查是否已认证
        if (!context.isAuthenticated()) {
            log.debug("Tool '{}' denied: user not authenticated", toolName);
            return PermissionResult.denied("User not authenticated");
        }

        String userId = context.getUserId();

        // 2. 管理员直接放行
        if (context.isAdmin()) {
            log.debug("Tool '{}' allowed: user {} is admin", toolName, userId);
            return PermissionResult.allowed();
        }

        // 3. 获取工具定义（需要从外部传入或缓存）
        // 这里只做基础检查，具体工具检查在 check(toolName, arguments, context) 中
        return PermissionResult.allowed();
    }

    @Override
    public @NonNull PermissionResult check(
            @NonNull String toolName,
            @NonNull Map<String, Object> arguments,
            @NonNull ToolContext context) {
        // 1. 检查是否已认证
        if (!context.isAuthenticated()) {
            log.debug("Tool '{}' denied: user not authenticated", toolName);
            return PermissionResult.denied("User not authenticated");
        }

        String userId = context.getUserId();

        // 2. 管理员直接放行
        if (context.isAdmin()) {
            log.debug("Tool '{}' allowed: user {} is admin", toolName, userId);
            return PermissionResult.allowed();
        }

        // 3. 从参数中获取工具实例（如果有）
        Object toolObj = arguments.get("__tool__");
        if (!(toolObj instanceof SecureTool<?, ?> tool)) {
            // 非 SecureTool，默认允许
            return PermissionResult.allowed();
        }

        // 4. 检查角色
        Set<String> requiredRoles = tool.requiredRoles();
        if (!requiredRoles.isEmpty()) {
            if (permissionService.hasAnyRole(userId, requiredRoles)) {
                log.debug("Tool '{}' allowed: user {} has required role", toolName, userId);
                return PermissionResult.allowed();
            }
            // 角色不满足，继续检查权限
        }

        // 5. 检查权限
        String requiredPermission = tool.requiredPermission();
        if (requiredPermission != null && !requiredPermission.isBlank()) {
            if (permissionService.hasPermission(userId, requiredPermission)) {
                log.debug("Tool '{}' allowed: user {} has permission '{}'", toolName, userId, requiredPermission);
                return PermissionResult.allowed();
            }

            log.debug("Tool '{}' denied: user {} lacks permission '{}'", toolName, userId, requiredPermission);
            return PermissionResult.denied(
                "Permission '" + requiredPermission + "' required",
                requiredPermission
            );
        }

        // 6. 如果有角色要求但不满足
        if (!requiredRoles.isEmpty()) {
            log.debug("Tool '{}' denied: user {} lacks required roles {}", toolName, userId, requiredRoles);
            return PermissionResult.denied(
                "One of roles " + requiredRoles + " required",
                null
            );
        }

        // 7. 默认允许
        return PermissionResult.allowed();
    }

    /**
     * 检查 SecureTool 权限。
     *
     * @param tool    安全工具
     * @param context 工具上下文
     * @return 权限检查结果
     */
    public @NonNull PermissionResult checkSecureTool(
            @NonNull SecureTool<?, ?> tool,
            @NonNull ToolContext context) {
        // 1. 检查是否已认证
        if (!context.isAuthenticated()) {
            return PermissionResult.denied("User not authenticated");
        }

        String userId = context.getUserId();

        // 2. 管理员直接放行
        if (context.isAdmin()) {
            return PermissionResult.allowed();
        }

        // 3. 检查是否允许执行
        if (!tool.canExecute(context)) {
            return PermissionResult.denied("Tool execution not allowed in current context");
        }

        // 4. 检查角色
        Set<String> requiredRoles = tool.requiredRoles();
        if (!requiredRoles.isEmpty()) {
            if (permissionService.hasAnyRole(userId, requiredRoles)) {
                return PermissionResult.allowed();
            }
        }

        // 5. 检查权限
        String requiredPermission = tool.requiredPermission();
        if (requiredPermission != null && !requiredPermission.isBlank()) {
            if (permissionService.hasPermission(userId, requiredPermission)) {
                return PermissionResult.allowed();
            }

            return PermissionResult.denied(
                "Permission '" + requiredPermission + "' required",
                requiredPermission
            );
        }

        // 6. 如果有角色要求但不满足
        if (!requiredRoles.isEmpty()) {
            return PermissionResult.denied(
                "One of roles " + requiredRoles + " required",
                null
            );
        }

        // 7. 默认允许
        return PermissionResult.allowed();
    }
}
