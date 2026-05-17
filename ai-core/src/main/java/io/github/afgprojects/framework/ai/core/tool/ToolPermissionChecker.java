package io.github.afgprojects.framework.ai.core.tool;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * 工具权限检查器接口。
 *
 * <p>负责检查用户是否有权限调用特定工具。
 *
 * <p>实现示例：
 * <ul>
 *   <li>基于角色的权限控制 - 检查用户角色</li>
 *   <li>基于权限的访问控制 - 检查具体权限</li>
 *   <li>自定义策略 - 基于业务规则的动态判断</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>{@code
 * // 检查权限
 * PermissionResult result = permissionChecker.check("query_users", context);
 * if (!result.isAllowed()) {
 *     throw new AccessDeniedException(result.getDenyReason());
 * }
 *
 * // 带参数检查
 * PermissionResult result = permissionChecker.check("delete_user", arguments, context);
 * }</pre>
 *
 * @since 1.0.0
 */
public interface ToolPermissionChecker {

    /**
     * 检查用户是否有权限调用工具。
     *
     * @param toolName 工具名称
     * @param context  工具上下文
     * @return 权限检查结果
     */
    @NonNull
    PermissionResult check(@NonNull String toolName, @NonNull ToolContext context);

    /**
     * 检查用户是否有权限调用安全工具。
     *
     * <p>基于 SecureTool 的权限定义进行检查：
     * <ul>
     *   <li>检查 requiredPermission() 返回的权限</li>
     *   <li>检查 requiredRoles() 返回的角色</li>
     * </ul>
     *
     * @param tool    安全工具
     * @param context 工具上下文
     * @return 权限检查结果
     */
    @NonNull
    default PermissionResult checkSecureTool(@NonNull SecureTool<?, ?> tool, @NonNull ToolContext context) {
        // 检查权限
        String permission = tool.requiredPermission();
        if (permission != null && !permission.isEmpty()) {
            PermissionResult result = check(permission, context);
            if (result.isDenied()) {
                return result;
            }
        }

        // 检查角色
        var requiredRoles = tool.requiredRoles();
        if (!requiredRoles.isEmpty()) {
            var userRoles = context.getRoles();
            boolean hasRole = requiredRoles.stream()
                .anyMatch(userRoles::contains);
            if (!hasRole) {
                return PermissionResult.denied(
                    "Missing required role. Required: " + requiredRoles,
                    String.join(",", requiredRoles)
                );
            }
        }

        return PermissionResult.allowed();
    }

    /**
     * 检查用户是否有权限调用工具（带参数）。
     *
     * <p>某些工具可能需要根据参数进行更细粒度的权限检查。
     * 例如：删除用户工具需要检查用户是否有权限删除特定用户。
     *
     * @param toolName  工具名称
     * @param arguments 工具参数
     * @param context   工具上下文
     * @return 权限检查结果
     */
    default @NonNull PermissionResult check(
            @NonNull String toolName,
            @NonNull Map<String, Object> arguments,
            @NonNull ToolContext context) {
        return check(toolName, context);
    }

    /**
     * 权限检查结果。
     */
    interface PermissionResult {

        /**
         * 判断是否允许。
         *
         * @return 如果允许返回 true
         */
        boolean isAllowed();

        /**
         * 判断是否拒绝。
         *
         * @return 如果拒绝返回 true
         */
        default boolean isDenied() {
            return !isAllowed();
        }

        /**
         * 获取拒绝原因。
         *
         * @return 拒绝原因，允许时返回 null
         */
        @Nullable
        String getDenyReason();

        /**
         * 获取权限要求描述。
         *
         * <p>描述调用工具所需的权限或角色。
         *
         * @return 权限要求描述
         */
        @Nullable
        String getRequiredPermission();

        /**
         * 创建允许结果。
         *
         * @return 允许结果
         */
        static @NonNull PermissionResult allowed() {
            return DefaultPermissionResult.ALLOWED;
        }

        /**
         * 创建拒绝结果。
         *
         * @param reason 拒绝原因
         * @return 拒绝结果
         */
        static @NonNull PermissionResult denied(@NonNull String reason) {
            return new DefaultPermissionResult(false, reason, null);
        }

        /**
         * 创建拒绝结果（带权限要求）。
         *
         * @param reason            拒绝原因
         * @param requiredPermission 所需权限
         * @return 拒绝结果
         */
        static @NonNull PermissionResult denied(
                @NonNull String reason,
                @Nullable String requiredPermission) {
            return new DefaultPermissionResult(false, reason, requiredPermission);
        }
    }
}
