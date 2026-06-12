package io.github.afgprojects.framework.ai.core.tool;

import io.github.afgprojects.framework.ai.core.api.tool.ToolAuditLogger;
import io.github.afgprojects.framework.ai.core.api.tool.ToolContext;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 空操作工具审计日志记录器。
 *
 * <p>不执行任何审计日志记录的空实现，用于不需要审计日志的场景。
 *
 * <p>所有方法均为空操作：
 * <ul>
 *   <li>{@link #logStart} - 返回占位符 ID</li>
 *   <li>{@link #logSuccess} - 空操作</li>
 *   <li>{@link #logFailure} - 空操作</li>
 *   <li>{@link #logPermissionDenied} - 空操作</li>
 *   <li>{@link #query} - 返回空列表</li>
 * </ul>
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class NoOpToolAuditLogger implements ToolAuditLogger {

    @Override
    public @NonNull String logStart(
            @NonNull String callId,
            @NonNull String toolName,
            @NonNull Map<String, Object> arguments,
            @NonNull ToolContext context) {
        return "noop-audit-" + System.nanoTime();
    }

    @Override
    public void logSuccess(
            @NonNull String recordId,
            @Nullable Object output,
            @NonNull Duration duration) {
        // no-op
    }

    @Override
    public void logFailure(
            @NonNull String recordId,
            @NonNull String error,
            @NonNull Duration duration) {
        // no-op
    }

    @Override
    public void logPermissionDenied(
            @NonNull String callId,
            @NonNull String toolName,
            @NonNull String reason,
            @NonNull ToolContext context) {
        // no-op
    }

    @Override
    public @NonNull List<ToolAuditEntry> query(@NonNull ToolAuditQuery query) {
        return Collections.emptyList();
    }
}
