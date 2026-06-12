package io.github.afgprojects.framework.ai.core.tool;

import io.github.afgprojects.framework.ai.core.api.tool.ToolContext;
import io.github.afgprojects.framework.ai.core.api.tool.ToolPermissionChecker;
import org.jspecify.annotations.NonNull;

import java.util.Map;

/**
 * 空操作工具权限检查器。
 *
 * <p>始终允许所有工具调用的空实现，用于不需要权限检查的场景。
 *
 * <p>所有权限检查均返回允许结果：
 * <ul>
 *   <li>{@link #check(String, ToolContext)} - 始终返回允许</li>
 *   <li>{@link #check(String, Map, ToolContext)} - 始终返回允许</li>
 *   <li>{@link #checkSecureTool} - 始终返回允许</li>
 * </ul>
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class NoOpToolPermissionChecker implements ToolPermissionChecker {

    @Override
    public @NonNull PermissionResult check(@NonNull String toolName, @NonNull ToolContext context) {
        return PermissionResult.allowed();
    }

    @Override
    public @NonNull PermissionResult check(
            @NonNull String toolName,
            @NonNull Map<String, Object> arguments,
            @NonNull ToolContext context) {
        return PermissionResult.allowed();
    }
}
