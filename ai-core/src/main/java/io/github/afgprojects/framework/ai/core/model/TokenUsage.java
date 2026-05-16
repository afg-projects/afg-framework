package io.github.afgprojects.framework.ai.core.model;

/**
 * Represents token usage statistics for an LLM request/response.
 * <p>
 * TokenUsage tracks the number of tokens consumed in different parts
 * of an LLM interaction:
 * <ul>
 *   <li>promptTokens - tokens in the input prompt/messages</li>
 *   <li>completionTokens - tokens in the generated response</li>
 *   <li>totalTokens - sum of prompt and completion tokens</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * TokenUsage usage = new TokenUsage(100, 50, 150);
 * System.out.println("Prompt: " + usage.promptTokens());
 * System.out.println("Completion: " + usage.completionTokens());
 * System.out.println("Total: " + usage.totalTokens());
 * }</pre>
 *
 * @param promptTokens     the number of tokens in the prompt
 * @param completionTokens the number of tokens in the completion
 * @param totalTokens      the total number of tokens (prompt + completion)
 * @author AFG Projects
 * @since 1.0.0
 */
public record TokenUsage(
    long promptTokens,
    long completionTokens,
    long totalTokens
) {

    /**
     * Creates a TokenUsage with validated parameters.
     * <p>
     * All token counts must be non-negative. If totalTokens is not
     * provided or is inconsistent, it will be calculated from
     * promptTokens + completionTokens.
     *
     * @param promptTokens     the number of tokens in the prompt
     * @param completionTokens the number of tokens in the completion
     * @param totalTokens      the total number of tokens
     * @throws IllegalArgumentException if any token count is negative
     */
    public TokenUsage {
        if (promptTokens < 0) {
            throw new IllegalArgumentException("promptTokens cannot be negative");
        }
        if (completionTokens < 0) {
            throw new IllegalArgumentException("completionTokens cannot be negative");
        }
        if (totalTokens < 0) {
            throw new IllegalArgumentException("totalTokens cannot be negative");
        }
        // Auto-calculate total if not matching
        if (totalTokens != promptTokens + completionTokens) {
            totalTokens = promptTokens + completionTokens;
        }
    }

    /**
     * Creates a TokenUsage by calculating total from prompt and completion.
     *
     * @param promptTokens     the number of tokens in the prompt
     * @param completionTokens the number of tokens in the completion
     * @return a new TokenUsage instance
     */
    public static TokenUsage of(long promptTokens, long completionTokens) {
        return new TokenUsage(promptTokens, completionTokens, promptTokens + completionTokens);
    }

    /**
     * Creates an empty TokenUsage with all zeros.
     *
     * @return a TokenUsage with zero tokens
     */
    public static TokenUsage empty() {
        return new TokenUsage(0, 0, 0);
    }

    /**
     * Adds another TokenUsage to this one.
     *
     * @param other the other TokenUsage to add
     * @return a new TokenUsage with summed values
     */
    public TokenUsage add(TokenUsage other) {
        if (other == null) {
            return this;
        }
        return new TokenUsage(
            this.promptTokens + other.promptTokens,
            this.completionTokens + other.completionTokens,
            this.totalTokens + other.totalTokens
        );
    }

    /**
     * Checks if this TokenUsage is empty (all zeros).
     *
     * @return true if all token counts are zero
     */
    public boolean isEmpty() {
        return promptTokens == 0 && completionTokens == 0 && totalTokens == 0;
    }
}
