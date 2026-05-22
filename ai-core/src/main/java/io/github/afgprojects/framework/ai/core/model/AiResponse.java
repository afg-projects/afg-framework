package io.github.afgprojects.framework.ai.core.model;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a response from an AI client.
 * <p>
 * AiResponse is a simplified response model for the high-level AiClient API,
 * providing easy access to:
 * <ul>
 *   <li>content - the generated text content</li>
 *   <li>tokenUsage - the token usage statistics (optional)</li>
 *   <li>finishReason - the reason why the AI stopped generating</li>
 * </ul>
 *
 * <p>For more detailed response information including tool calls and results,
 * use the lower-level LlmClient API.
 *
 * <p>Example usage:
 * <pre>{@code
 * AiResponse response = aiClient.prompt("Hello!").call();
 *
 * if (response.hasContent()) {
 *     System.out.println(response.content());
 * }
 *
 * if (response.hasTokenUsage()) {
 *     System.out.println("Tokens used: " + response.tokenUsage().total());
 * }
 * }</pre>
 *
 * @param content      the generated text content
 * @param tokenUsage   the token usage statistics (may be null for streaming chunks)
 * @param finishReason the reason why the AI stopped generating
 * @author AFG Projects
 * @since 1.0.0
 */
public record AiResponse(
    @Nullable String content,
    @Nullable TokenUsage tokenUsage,
    @Nullable FinishReason finishReason
) {

    /**
     * Represents the reason why an AI stopped generating.
     *
     * @author AFG Projects
     * @since 1.0.0
     */
    public enum FinishReason {
        /**
         * The AI stopped naturally (end of response).
         */
        STOP,

        /**
         * The AI stopped due to reaching the maximum length.
         */
        LENGTH,

        /**
         * The AI stopped to call a tool.
         */
        TOOL_CALL,

        /**
         * The content was filtered due to safety policies.
         */
        CONTENT_FILTER,

        /**
         * The AI stopped for an unknown reason.
         */
        UNKNOWN
    }

    /**
     * Creates an AiResponse with only content.
     *
     * @param content the generated text content
     * @return a new AiResponse instance
     */
    @NonNull
    public static AiResponse of(@Nullable String content) {
        return new AiResponse(content, null, null);
    }

    /**
     * Creates an AiResponse with content and finish reason.
     *
     * @param content      the generated text content
     * @param finishReason the finish reason
     * @return a new AiResponse instance
     */
    @NonNull
    public static AiResponse of(@Nullable String content, @NonNull FinishReason finishReason) {
        return new AiResponse(content, null, finishReason);
    }

    /**
     * Creates an AiResponse with content and token usage.
     *
     * @param content    the generated text content
     * @param tokenUsage the token usage
     * @return a new AiResponse instance
     */
    @NonNull
    public static AiResponse of(@Nullable String content, @NonNull TokenUsage tokenUsage) {
        return new AiResponse(content, tokenUsage, null);
    }

    /**
     * Creates an AiResponse with all fields.
     *
     * @param content      the generated text content
     * @param tokenUsage   the token usage
     * @param finishReason the finish reason
     * @return a new AiResponse instance
     */
    @NonNull
    public static AiResponse of(
        @Nullable String content,
        @Nullable TokenUsage tokenUsage,
        @Nullable FinishReason finishReason
    ) {
        return new AiResponse(content, tokenUsage, finishReason);
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
     * Creates a new AiResponse with different content.
     *
     * @param content the new content
     * @return a new AiResponse instance
     */
    @NonNull
    public AiResponse withContent(@Nullable String content) {
        return new AiResponse(content, this.tokenUsage, this.finishReason);
    }

    /**
     * Creates a new AiResponse with different token usage.
     *
     * @param tokenUsage the new token usage
     * @return a new AiResponse instance
     */
    @NonNull
    public AiResponse withTokenUsage(@Nullable TokenUsage tokenUsage) {
        return new AiResponse(this.content, tokenUsage, this.finishReason);
    }

    /**
     * Creates a new AiResponse with different finish reason.
     *
     * @param finishReason the new finish reason
     * @return a new AiResponse instance
     */
    @NonNull
    public AiResponse withFinishReason(@Nullable FinishReason finishReason) {
        return new AiResponse(this.content, this.tokenUsage, finishReason);
    }

    /**
     * Concatenates the content of this response with another.
     * <p>
     * This is useful for combining streaming chunks.
     *
     * @param other the other response to concatenate
     * @return a new AiResponse with combined content
     */
    @NonNull
    public AiResponse concat(@NonNull AiResponse other) {
        String newContent = (this.content == null ? "" : this.content)
                          + (other.content == null ? "" : other.content);
        return new AiResponse(newContent, other.tokenUsage, other.finishReason);
    }
}
