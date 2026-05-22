package io.github.afgprojects.framework.ai.llm.advisor;

import io.github.afgprojects.framework.ai.core.model.LlmRequest;
import io.github.afgprojects.framework.ai.core.model.LlmResponse;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

/**
 * Interface for LLM advisors that can modify requests and responses.
 * <p>
 * Advisors provide a mechanism to intercept and modify LLM requests before
 * they are sent to the model, and responses after they are received.
 * This enables features like:
 * <ul>
 *   <li>System prompt injection</li>
 *   <li>Conversation memory management</li>
 *   <li>RAG context augmentation</li>
 *   <li>Content filtering and transformation</li>
 * </ul>
 *
 * <p>Advisors are executed in order based on their {@link #getOrder()} value.
 * Lower values are executed first.
 *
 * <p>Example implementation:
 * <pre>{@code
 * public class LoggingAdvisor implements LlmAdvisor {
 *     @Override
 *     public int getOrder() { return 1000; }
 *
 *     @Override
 *     public String getName() { return "LoggingAdvisor"; }
 *
 *     @Override
 *     public List<Message> apply(List<Message> messages, AdvisorContext context) {
 *         log.info("Processing {} messages", messages.size());
 *         return messages;
 *     }
 * }
 * }</pre>
 *
 * @author afg-projects
 * @since 1.0.0
 */
public interface LlmAdvisor {

    /**
     * Gets the execution order for this advisor.
     * <p>
     * Advisors with lower order values are executed first.
     * Common order values:
     * <ul>
     *   <li>0-99: System prompt advisors</li>
     *   <li>100-199: Memory advisors</li>
     *   <li>200-299: RAG advisors</li>
     *   <li>1000+: Logging and monitoring advisors</li>
     * </ul>
     *
     * @return the order value
     */
    int getOrder();

    /**
     * Gets the name of this advisor.
     * <p>
     * Used for logging and debugging purposes.
     *
     * @return the advisor name
     */
    String getName();

    /**
     * Applies this advisor to the message list.
     * <p>
     * This method is called before the request is sent to the LLM.
     * Implementations can modify, add, or remove messages.
     *
     * @param messages the current message list
     * @param context  the advisor context containing request information
     * @return the modified message list
     */
    @NonNull
    List<Message> apply(@NonNull List<Message> messages, @NonNull AdvisorContext context);

    /**
     * Called after a response is received from the LLM.
     * <p>
     * This method can be used for post-processing, logging, or
     * updating internal state (e.g., conversation memory).
     *
     * @param response the LLM response
     * @param context  the advisor context
     */
    default void onResponse(@NonNull LlmResponse response, @NonNull AdvisorContext context) {
        // Default: no action
    }

    /**
     * Context information passed to advisors during execution.
     */
    interface AdvisorContext {
        /**
         * Gets the original LLM request.
         *
         * @return the request
         */
        @NonNull
        LlmRequest getRequest();

        /**
         * Gets the session ID for this conversation.
         *
         * @return the session ID, or null if not available
         */
        @Nullable
        String getSessionId();

        /**
         * Gets a context attribute by key.
         *
         * @param key the attribute key
         * @param <T> the attribute type
         * @return the attribute value, or null if not present
         */
        @Nullable
        <T> T getAttribute(@NonNull String key);

        /**
         * Sets a context attribute.
         *
         * @param key   the attribute key
         * @param value the attribute value
         */
        void setAttribute(@NonNull String key, @Nullable Object value);
    }
}
