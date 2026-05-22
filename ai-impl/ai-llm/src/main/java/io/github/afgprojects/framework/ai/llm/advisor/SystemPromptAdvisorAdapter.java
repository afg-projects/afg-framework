package io.github.afgprojects.framework.ai.llm.advisor;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Advisor adapter for managing system prompts.
 * <p>
 * This adapter wraps the framework's system prompt functionality and provides
 * a mechanism to inject system prompts into the conversation context.
 *
 * <p>Example usage:
 * <pre>{@code
 * // Create with static system prompt
 * SystemPromptAdvisorAdapter advisor = new SystemPromptAdvisorAdapter(
 *     "You are a helpful assistant."
 * );
 *
 * // Create with dynamic system prompt provider
 * SystemPromptAdvisorAdapter advisor = new SystemPromptAdvisorAdapter(
 *     () -> "Current time: " + Instant.now()
 * );
 * }</pre>
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class SystemPromptAdvisorAdapter implements LlmAdvisor {

    private final SystemPromptProvider systemPromptProvider;

    /**
     * Creates an advisor with a static system prompt.
     *
     * @param systemPrompt the system prompt text (can be null)
     */
    public SystemPromptAdvisorAdapter(@Nullable String systemPrompt) {
        this.systemPromptProvider = () -> systemPrompt;
    }

    /**
     * Creates an advisor with a dynamic system prompt provider.
     *
     * @param systemPromptProvider the provider for system prompts
     */
    public SystemPromptAdvisorAdapter(@NonNull SystemPromptProvider systemPromptProvider) {
        this.systemPromptProvider = systemPromptProvider;
    }

    @Override
    public int getOrder() {
        // Execute first to ensure system prompt is added before other processing
        return 0;
    }

    @Override
    public String getName() {
        return "SystemPromptAdvisorAdapter";
    }

    @Override
    @NonNull
    public List<Message> apply(@NonNull List<Message> messages, @NonNull AdvisorContext context) {
        String systemPrompt = systemPromptProvider.getSystemPrompt();
        if (systemPrompt == null || systemPrompt.isBlank()) {
            return messages;
        }

        // Check if there's already a system message
        List<Message> result = new ArrayList<>(messages);
        boolean hasSystemMessage = result.stream()
                .anyMatch(msg -> msg instanceof SystemMessage);

        if (hasSystemMessage) {
            // Replace existing system message
            result.removeIf(msg -> msg instanceof SystemMessage);
        }

        // Add new system message at the beginning
        result.add(0, new SystemMessage(systemPrompt));

        return result;
    }

    /**
     * Functional interface for providing system prompts.
     * <p>
     * Allows dynamic system prompt generation based on context.
     */
    @FunctionalInterface
    public interface SystemPromptProvider {
        /**
         * Gets the system prompt.
         *
         * @return the system prompt text, or null if no prompt should be added
         */
        @Nullable String getSystemPrompt();
    }
}
