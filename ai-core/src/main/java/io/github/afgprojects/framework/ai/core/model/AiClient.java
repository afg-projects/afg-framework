package io.github.afgprojects.framework.ai.core.model;

import io.github.afgprojects.framework.ai.core.memory.ConversationMemory;
import io.github.afgprojects.framework.ai.core.tool.ToolDefinition;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * High-level AI client interface providing a fluent streaming API.
 * <p>
 * AiClient provides a modern, builder-style API for interacting with AI models,
 * wrapping Spring AI's ChatClient with framework-specific features:
 * <ul>
 *   <li>Fluent prompt building with method chaining</li>
 *   <li>System prompt configuration</li>
 *   <li>Tool/function calling support</li>
 *   <li>Conversation memory integration</li>
 *   <li>Streaming and synchronous responses</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * // Simple prompt
 * AiResponse response = aiClient.prompt("Hello!")
 *     .call();
 *
 * // With system prompt
 * AiResponse response = aiClient
 *     .withSystemPrompt("You are a helpful assistant")
 *     .prompt("What is Java?")
 *     .call();
 *
 * // Streaming response
 * Flux<AiResponse> stream = aiClient
 *     .prompt("Tell me a story")
 *     .stream();
 *
 * // With tools
 * AiResponse response = aiClient
 *     .withTools(List.of(searchTool, calculatorTool))
 *     .prompt("What is 2 + 2?")
 *     .call();
 *
 * // With conversation memory
 * AiResponse response = aiClient
 *     .withMemory(conversationMemory)
 *     .prompt("Remember my name is Alice")
 *     .call();
 * }</pre>
 *
 * @author AFG Projects
 * @since 1.0.0
 */
public interface AiClient {

    /**
     * Sets the user prompt for this request.
     * <p>
     * This method starts a new request chain. The prompt will be combined
     * with any configured system prompt, tools, and memory.
     *
     * @param prompt the user prompt text
     * @return a new PromptSpec for further configuration
     * @throws IllegalArgumentException if prompt is null or blank
     */
    @NonNull
    PromptSpec prompt(@NonNull String prompt);

    /**
     * Sets the system prompt for all subsequent requests.
     * <p>
     * The system prompt sets the behavior and context for the AI assistant.
     * This configuration persists across multiple prompt() calls.
     *
     * @param systemPrompt the system prompt text
     * @return this AiClient instance for method chaining
     */
    @NonNull
    AiClient withSystemPrompt(@Nullable String systemPrompt);

    /**
     * Sets the tools available for the AI to call.
     * <p>
     * Tools are functions that the AI can call to perform actions or
     * retrieve information. This configuration persists across multiple
     * prompt() calls.
     *
     * @param tools the list of tool definitions
     * @return this AiClient instance for method chaining
     * @throws IllegalArgumentException if tools is null
     */
    @NonNull
    AiClient withTools(@NonNull List<ToolDefinition> tools);

    /**
     * Sets the conversation memory for maintaining context.
     * <p>
     * Conversation memory stores the history of messages, allowing
     * the AI to maintain context across multiple interactions.
     *
     * @param memory the conversation memory implementation
     * @return this AiClient instance for method chaining
     * @throws IllegalArgumentException if memory is null
     */
    @NonNull
    AiClient withMemory(@NonNull ConversationMemory memory);

    /**
     * Sets the session ID for conversation memory.
     * <p>
     * The session ID identifies which conversation to use in the memory.
     * If not set, a default session ID will be used.
     *
     * @param sessionId the unique session identifier
     * @return this AiClient instance for method chaining
     */
    @NonNull
    AiClient withSessionId(@NonNull String sessionId);

    /**
     * Gets the current system prompt.
     *
     * @return the system prompt, or null if not set
     */
    @Nullable
    String getSystemPrompt();

    /**
     * Gets the current tools.
     *
     * @return the list of tool definitions, or empty list if none
     */
    @NonNull
    List<ToolDefinition> getTools();

    /**
     * Specification for a single prompt request.
     * <p>
     * PromptSpec allows configuring a specific request before executing
     * it with call() or stream().
     *
     * @author AFG Projects
     * @since 1.0.0
     */
    interface PromptSpec {

        /**
         * Executes the prompt and returns the response synchronously.
         *
         * @return the AI response
         * @throws io.github.afgprojects.framework.ai.core.exception.AiException if the request fails
         */
        @NonNull
        AiResponse call();

        /**
         * Executes the prompt and returns a stream of response chunks.
         * <p>
         * This is useful for real-time display of the AI's response
         * as it is being generated.
         *
         * @return a Flux of response chunks
         */
        @NonNull
        Flux<AiResponse> stream();

        /**
         * Adds additional context to this prompt.
         * <p>
         * Context is appended to the user prompt to provide additional
         * information without changing the main prompt.
         *
         * @param context the additional context
         * @return this PromptSpec for method chaining
         */
        @NonNull
        PromptSpec withContext(@NonNull String context);

        /**
         * Sets an option for this specific request.
         * <p>
         * Options are provider-specific parameters like temperature,
         * maxTokens, etc.
         *
         * @param key   the option key
         * @param value the option value
         * @return this PromptSpec for method chaining
         */
        @NonNull
        PromptSpec option(@NonNull String key, @Nullable Object value);
    }
}
