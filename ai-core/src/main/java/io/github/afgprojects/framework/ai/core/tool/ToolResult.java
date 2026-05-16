package io.github.afgprojects.framework.ai.core.tool;

import jakarta.annotation.Nullable;

/**
 * Represents the result of a tool execution.
 *
 * @param toolCallId the ID of the tool call this result corresponds to
 * @param toolName   the name of the tool that was executed
 * @param output     the output from the tool execution (may be null)
 * @param error      the error message if execution failed (may be null)
 * @author AFG Projects
 * @since 1.0.0
 */
public record ToolResult(
    String toolCallId,
    String toolName,
    @Nullable String output,
    @Nullable String error
) {

    /**
     * Creates a ToolResult with validated parameters.
     *
     * @param toolCallId the ID of the tool call
     * @param toolName   the name of the tool
     * @param output     the output (may be null)
     * @param error      the error (may be null)
     * @throws IllegalArgumentException if toolCallId or toolName is null or blank
     */
    public ToolResult {
        if (toolCallId == null || toolCallId.isBlank()) {
            throw new IllegalArgumentException("Tool call id cannot be null or blank");
        }
        if (toolName == null || toolName.isBlank()) {
            throw new IllegalArgumentException("Tool name cannot be null or blank");
        }
    }

    /**
     * Creates a successful ToolResult.
     *
     * @param toolCallId the ID of the tool call
     * @param toolName   the name of the tool
     * @param output     the output
     * @return a new ToolResult instance indicating success
     */
    public static ToolResult success(String toolCallId, String toolName, String output) {
        return new ToolResult(toolCallId, toolName, output, null);
    }

    /**
     * Creates a failed ToolResult.
     *
     * @param toolCallId the ID of the tool call
     * @param toolName   the name of the tool
     * @param error      the error message
     * @return a new ToolResult instance indicating failure
     */
    public static ToolResult failure(String toolCallId, String toolName, String error) {
        return new ToolResult(toolCallId, toolName, null, error);
    }

    /**
     * Checks if the tool execution was successful.
     *
     * @return true if no error occurred, false otherwise
     */
    public boolean isSuccess() {
        return error == null;
    }

    /**
     * Checks if the tool execution failed.
     *
     * @return true if an error occurred, false otherwise
     */
    public boolean isFailure() {
        return error != null;
    }
}
