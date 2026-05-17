package io.github.afgprojects.framework.ai.core.tool;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 工具审计日志接口。
 *
 * <p>记录 AI 工具调用的审计日志，包括：
 * <ul>
 *   <li>调用者信息 - 用户 ID、租户 ID、会话 ID</li>
 *   <li>工具信息 - 工具名称、参数</li>
 *   <li>执行结果 - 输出、错误信息</li>
 *   <li>执行时间 - 开始时间、结束时间、耗时</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>{@code
 * // 记录开始
 * String recordId = auditLogger.logStart(callId, "query_users", arguments, context);
 *
 * try {
 *     // 执行工具
 *     Object output = tool.execute(input);
 *     auditLogger.logSuccess(recordId, output, duration);
 * } catch (Exception e) {
 *     auditLogger.logFailure(recordId, e.getMessage(), duration);
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public interface ToolAuditLogger {

    /**
     * 记录工具调用开始。
     *
     * @param callId    调用 ID
     * @param toolName  工具名称
     * @param arguments 工具参数
     * @param context   工具上下文
     * @return 调用记录 ID
     */
    @NonNull
    String logStart(
        @NonNull String callId,
        @NonNull String toolName,
        @NonNull Map<String, Object> arguments,
        @NonNull ToolContext context
    );

    /**
     * 记录工具调用成功。
     *
     * @param recordId 调用记录 ID
     * @param output   工具输出
     * @param duration 执行时长
     */
    void logSuccess(
        @NonNull String recordId,
        @Nullable Object output,
        @NonNull Duration duration
    );

    /**
     * 记录工具调用失败。
     *
     * @param recordId 调用记录 ID
     * @param error    错误信息
     * @param duration 执行时长
     */
    void logFailure(
        @NonNull String recordId,
        @NonNull String error,
        @NonNull Duration duration
    );

    /**
     * 记录权限拒绝。
     *
     * @param callId   调用 ID
     * @param toolName 工具名称
     * @param reason   拒绝原因
     * @param context  工具上下文
     */
    void logPermissionDenied(
        @NonNull String callId,
        @NonNull String toolName,
        @NonNull String reason,
        @NonNull ToolContext context
    );

    /**
     * 记录超时。
     *
     * @param callId   调用 ID
     * @param toolName 工具名称
     * @param context  工具上下文
     * @param timeout  超时时间
     */
    default void logTimeout(
            @NonNull String callId,
            @NonNull String toolName,
            @NonNull ToolContext context,
            @NonNull Duration timeout) {
        logFailure(callId, "Tool execution timed out after " + timeout.toMillis() + "ms", timeout);
    }

    /**
     * 查询审计日志。
     *
     * @param query 查询条件
     * @return 审计日志列表
     */
    @NonNull
    List<ToolAuditEntry> query(@NonNull ToolAuditQuery query);

    /**
     * 工具审计日志条目。
     */
    interface ToolAuditEntry {

        /**
         * 获取记录 ID。
         *
         * @return 记录 ID
         */
        @NonNull
        String getId();

        /**
         * 获取调用 ID。
         *
         * @return 调用 ID
         */
        @NonNull
        String getCallId();

        /**
         * 获取工具名称。
         *
         * @return 工具名称
         */
        @NonNull
        String getToolName();

        /**
         * 获取用户 ID。
         *
         * @return 用户 ID
         */
        @Nullable
        String getUserId();

        /**
         * 获取租户 ID。
         *
         * @return 租户 ID
         */
        @Nullable
        String getTenantId();

        /**
         * 获取会话 ID。
         *
         * @return 会话 ID
         */
        @Nullable
        String getSessionId();

        /**
         * 获取工具参数。
         *
         * @return 工具参数
         */
        @NonNull
        Map<String, Object> getArguments();

        /**
         * 获取工具输出。
         *
         * @return 工具输出
         */
        @Nullable
        Object getOutput();

        /**
         * 获取错误信息。
         *
         * @return 错误信息
         */
        @Nullable
        String getError();

        /**
         * 获取调用状态。
         *
         * @return 调用状态
         */
        @NonNull
        ToolCallStatus getStatus();

        /**
         * 获取开始时间。
         *
         * @return 开始时间
         */
        @NonNull
        Instant getStartTime();

        /**
         * 获取结束时间。
         *
         * @return 结束时间
         */
        @Nullable
        Instant getEndTime();

        /**
         * 获取执行时长（毫秒）。
         *
         * @return 执行时长
         */
        long getDurationMs();
    }

    /**
     * 工具调用状态。
     */
    enum ToolCallStatus {
        /**
         * 已开始。
         */
        STARTED,

        /**
         * 执行成功。
         */
        SUCCESS,

        /**
         * 执行失败。
         */
        FAILURE,

        /**
         * 权限拒绝。
         */
        PERMISSION_DENIED,

        /**
         * 执行超时。
         */
        TIMEOUT
    }

    /**
     * 审计日志查询条件。
     */
    interface ToolAuditQuery {

        /**
         * 获取用户 ID。
         *
         * @return 用户 ID
         */
        @Nullable
        String getUserId();

        /**
         * 获取租户 ID。
         *
         * @return 租户 ID
         */
        @Nullable
        String getTenantId();

        /**
         * 获取工具名称。
         *
         * @return 工具名称
         */
        @Nullable
        String getToolName();

        /**
         * 获取调用状态。
         *
         * @return 调用状态
         */
        @Nullable
        ToolCallStatus getStatus();

        /**
         * 获取开始时间范围（起始）。
         *
         * @return 开始时间
         */
        @Nullable
        Instant getStartTimeFrom();

        /**
         * 获取开始时间范围（结束）。
         *
         * @return 结束时间
         */
        @Nullable
        Instant getStartTimeTo();

        /**
         * 获取页码（从 0 开始）。
         *
         * @return 页码
         */
        int getPage();

        /**
         * 获取每页大小。
         *
         * @return 每页大小
         */
        int getSize();
    }
}
