package io.github.afgprojects.framework.ai.core.model;

import io.github.afgprojects.framework.ai.core.tool.ToolDefinition;
import org.jspecify.annotations.NonNull;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Interface for interacting with an LLM.
 * <p>
 * LlmClient provides methods for:
 * <ul>
 *   <li>Synchronous chat - single request/response</li>
 *   <li>Streaming chat - real-time response chunks</li>
 *   <li>Tool-enabled chat - automatic tool call handling</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * // Simple chat
 * LlmResponse response = client.chat(LlmRequest.ofUserMessage("Hello!"));
 *
 * // Streaming chat
 * Flux<LlmResponse> stream = client.chatStream(request);
 * stream.subscribe(chunk -> System.out.println(chunk.content()));
 *
 * // Chat with tools
 * List<ToolDefinition> tools = List.of(searchTool, calculatorTool);
 * LlmResponse response = client.chatWithTools(request, tools);
 * }</pre>
 *
 * @author AFG Projects
 * @since 1.0.0
 */
public interface LlmClient {

    /**
     * Sends a chat request and returns the response.
     * <p>
     * This is a synchronous operation that waits for the complete
     * response from the LLM.
     *
     * @param request the chat request
     * @return the chat response
     * @throws IllegalArgumentException if request is null
     * @throws io.github.afgprojects.framework.ai.core.exception.AiException if the request fails
     */
    @NonNull
    LlmResponse chat(@NonNull LlmRequest request);

    /**
     * Sends a chat request and returns a stream of response chunks.
     * <p>
     * This is useful for real-time display of the LLM's response
     * as it is being generated.
     *
     * @param request the chat request
     * @return a Flux of response chunks
     * @throws IllegalArgumentException if request is null
     */
    @NonNull
    Flux<LlmResponse> chatStream(@NonNull LlmRequest request);

    /**
     * Sends a chat request with tools and handles tool calls automatically.
     * <p>
     * This method will:
     * <ol>
     *   <li>Send the request with available tools</li>
     *   <li>If the LLM responds with tool calls, execute them</li>
     *   <li>Send the tool results back to the LLM</li>
     *   <li>Repeat until the LLM provides a final response</li>
     * </ol>
     *
     * <p>Note: Implementations should have a maximum iteration limit
     * to prevent infinite loops.
     *
     * @param request the chat request
     * @param tools   the tools available for the LLM to call
     * @return the final chat response after all tool calls are handled
     * @throws IllegalArgumentException if request or tools is null
     * @throws io.github.afgprojects.framework.ai.core.exception.AiException if the request fails
     */
    @NonNull
    LlmResponse chatWithTools(
        @NonNull LlmRequest request,
        @NonNull List<ToolDefinition> tools
    );

    /**
     * Gets the configuration for this client.
     *
     * @return the LLM configuration
     */
    @NonNull
    LlmConfig getConfig();

    /**
     * Checks if this client supports streaming.
     *
     * @return true if streaming is supported
     */
    default boolean supportsStreaming() {
        return true;
    }

    /**
     * Checks if this client supports tool calls.
     *
     * @return true if tool calls are supported
     */
    default boolean supportsToolCalls() {
        return true;
    }

    /**
     * Shuts down this client and releases resources.
     * <p>
     * After calling this method, the client should not be used.
     */
    default void shutdown() {
        // Default: no-op
    }
}
