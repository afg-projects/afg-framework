package io.github.afgprojects.framework.ai.core.tool;

import org.jspecify.annotations.Nullable;

/**
 * 默认权限检查结果。
 *
 * @param isAllowed          是否允许
 * @param denyReason         拒绝原因
 * @param requiredPermission 所需权限
 * @since 1.0.0
 */
public record DefaultPermissionResult(
    boolean isAllowed,
    @Nullable String denyReason,
    @Nullable String requiredPermission
) implements ToolPermissionChecker.PermissionResult {

    /**
     * 允许结果单例。
     */
    public static final DefaultPermissionResult ALLOWED = new DefaultPermissionResult(true, null, null);

    /**
     * 拒绝结果单例（无原因）。
     */
    public static final DefaultPermissionResult DENIED = new DefaultPermissionResult(false, "Permission denied", null);

    @Override
    public boolean isAllowed() {
        return isAllowed;
    }

    @Override
    public boolean isDenied() {
        return !isAllowed;
    }

    @Override
    public @Nullable String getDenyReason() {
        return denyReason;
    }

    @Override
    public @Nullable String getRequiredPermission() {
        return requiredPermission;
    }
}
