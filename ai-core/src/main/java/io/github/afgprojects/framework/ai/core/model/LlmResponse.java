package io.github.afgprojects.framework.ai.core.model;

import io.github.afgprojects.framework.ai.core.tool.ToolCall;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a response from an LLM.
 * <p>
 * LlmResponse contains the result of an LLM request:
 * <ul>
 *   <li>content - the generated text content</li>
 *   <li>toolCalls - the tool calls requested by the LLM</li>
 *   <li>tokenUsage - the token usage statistics</li>
 *   <li>finishReason - the reason why the LLM stopped generating</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * LlmResponse response = new LlmResponse(
 *     "Hello! How can I help you?",
 *     List.of(),
 *     new TokenUsage(10, 8, 18),
 *     FinishReason.STOP
 * );
 *
 * if (response.hasToolCalls()) {
 *     for (ToolCall call : response.toolCalls()) {
 *         System.out.println("Tool: " + call.name());
 *     }
 * }
 * }</pre>
 *
 * @param content      the generated text content
 * @param toolCalls    the tool calls requested by the LLM
 * @param tokenUsage   the token usage statistics
 * @param finishReason the reason why the LLM stopped generating
 * @author AFG Projects
 * @since 1.0.0
 */
public record LlmResponse(
    @Nullable String content,
    @NonNull List<ToolCall> toolCalls,
    @Nullable TokenUsage tokenUsage,
    @Nullable FinishReason finishReason
) {

    /**
     * Creates an LlmResponse with validated parameters.
     * <p>
     * Null safety is ensured:
     * <ul>
     *   <li>content can be null (for tool-only responses)</li>
     *   <li>toolCalls defaults to empty list if null</li>
     *   <li>tokenUsage can be null</li>
     *   <li>finishReason can be null</li>
     * </ul>
     *
     * @param content      the generated text content
     * @param toolCalls    the tool calls
     * @param tokenUsage   the token usage
     * @param finishReason the finish reason
     */
    public LlmResponse {
        if (toolCalls == null) {
            toolCalls = List.of();
        } else {
            toolCalls = Collections.unmodifiableList(new ArrayList<>(toolCalls));
        }
    }

    /**
     * Creates an LlmResponse with only content.
     *
     * @param content the generated text content
     * @return a new LlmResponse instance
     */
    @NonNull
    public static LlmResponse of(@Nullable String content) {
        return new LlmResponse(content, List.of(), null, null);
    }

    /**
     * Creates an LlmResponse with content and finish reason.
     *
     * @param content      the generated text content
     * @param finishReason the finish reason
     * @return a new LlmResponse instance
     */
    @NonNull
    public static LlmResponse of(@Nullable String content, @NonNull FinishReason finishReason) {
        return new LlmResponse(content, List.of(), null, finishReason);
    }

    /**
     * Creates an LlmResponse with content and token usage.
     *
     * @param content    the generated text content
     * @param tokenUsage the token usage
     * @return a new LlmResponse instance
     */
    @NonNull
    public static LlmResponse of(@Nullable String content, @NonNull TokenUsage tokenUsage) {
        return new LlmResponse(content, List.of(), tokenUsage, null);
    }

    /**
     * Creates an LlmResponse with tool calls only.
     *
     * @param toolCalls the tool calls
     * @return a new LlmResponse instance
     */
    @NonNull
    public static LlmResponse ofToolCalls(@NonNull List<ToolCall> toolCalls) {
        return new LlmResponse(null, toolCalls, null, FinishReason.TOOL_CALL);
    }

    /**
     * Checks if this response has tool calls.
     *
     * @return true if tool calls are present
     */
    public boolean hasToolCalls() {
        return !toolCalls.isEmpty();
    }

    /**
     * Checks if this response has content.
     *
     * @return true if content is present and not blank
     */
    public boolean hasContent() {
        return content != null && !content.isBlank();
    }

    /**
     * Checks if this response has token usage.
     *
     * @return true if token usage is present
     */
    public boolean hasTokenUsage() {
        return tokenUsage != null && !tokenUsage.isEmpty();
    }

    /**
     * Creates a new LlmResponse with different content.
     *
     * @param content the new content
     * @return a new LlmResponse instance
     */
    @NonNull
    public LlmResponse withContent(@Nullable String content) {
        return new LlmResponse(content, this.toolCalls, this.tokenUsage, this.finishReason);
    }

    /**
     * Creates a new LlmResponse with different tool calls.
     *
     * @param toolCalls the new tool calls
     * @return a new LlmResponse instance
     */
    @NonNull
    public LlmResponse withToolCalls(@NonNull List<ToolCall> toolCalls) {
        return new LlmResponse(this.content, toolCalls, this.tokenUsage, this.finishReason);
    }

    /**
     * Creates a new LlmResponse with different token usage.
     *
     * @param tokenUsage the new token usage
     * @return a new LlmResponse instance
     */
    @NonNull
    public LlmResponse withTokenUsage(@Nullable TokenUsage tokenUsage) {
        return new LlmResponse(this.content, this.toolCalls, tokenUsage, this.finishReason);
    }

    /**
     * Creates a new LlmResponse with different finish reason.
     *
     * @param finishReason the new finish reason
     * @return a new LlmResponse instance
     */
    @NonNull
    public LlmResponse withFinishReason(@Nullable FinishReason finishReason) {
        return new LlmResponse(this.content, this.toolCalls, this.tokenUsage, finishReason);
    }

    /**
     * Represents the reason why an LLM stopped generating.
     *
     * @author AFG Projects
     * @since 1.0.0
     */
    public enum FinishReason {
        /**
         * The LLM stopped naturally (end of response).
         */
        STOP,

        /**
         * The LLM stopped due to reaching the maximum length.
         */
        LENGTH,

        /**
         * The LLM stopped to call a tool.
         */
        TOOL_CALL,

        /**
         * The content was filtered due to safety policies.
         */
        CONTENT_FILTER,

        /**
         * The LLM stopped for an unknown reason.
         */
        UNKNOWN
    }
}
