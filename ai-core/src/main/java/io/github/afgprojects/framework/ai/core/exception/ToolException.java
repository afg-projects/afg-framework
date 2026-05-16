package io.github.afgprojects.framework.ai.core.exception;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Tool 相关异常
 *
 * <p>处理工具调用过程中的异常情况。
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class ToolException extends AiException {

    private final @Nullable String toolName;

    /**
     * 创建 Tool 异常
     *
     * @param message   异常消息
     * @param errorCode 错误代码
     * @param toolName  工具名称
     */
    public ToolException(@NonNull String message, @NonNull String errorCode, @Nullable String toolName) {
        super(message, errorCode);
        this.toolName = toolName;
    }

    /**
     * 创建 Tool 异常（带原因）
     *
     * @param message   异常消息
     * @param errorCode 错误代码
     * @param toolName  工具名称
     * @param cause     原因
     */
    public ToolException(@NonNull String message, @NonNull String errorCode, @Nullable String toolName, @Nullable Throwable cause) {
        super(message, errorCode, cause);
        this.toolName = toolName;
    }

    /**
     * 获取工具名称
     *
     * @return 工具名称，可能为 null
     */
    public @Nullable String getToolName() {
        return toolName;
    }

    /**
     * 创建工具未找到异常
     */
    public static @NonNull ToolException notFound(@NonNull String toolName) {
        return new ToolException(
                "Tool not found: " + toolName,
                ErrorCodes.TOOL_NOT_FOUND,
                toolName
        );
    }

    /**
     * 创建工具执行失败异常
     */
    public static @NonNull ToolException executionFailed(@NonNull String toolName, @Nullable Throwable cause) {
        return new ToolException(
                "Tool execution failed: " + toolName,
                ErrorCodes.TOOL_EXECUTION_FAILED,
                toolName,
                cause
        );
    }

    /**
     * 创建工具输入无效异常
     */
    public static @NonNull ToolException invalidInput(@NonNull String toolName, @NonNull String details) {
        return new ToolException(
                "Invalid input for tool " + toolName + ": " + details,
                ErrorCodes.TOOL_INVALID_INPUT,
                toolName
        );
    }
}