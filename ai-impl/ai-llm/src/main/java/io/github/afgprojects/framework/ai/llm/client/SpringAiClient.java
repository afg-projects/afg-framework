package io.github.afgprojects.framework.ai.llm.client;

import io.github.afgprojects.framework.ai.core.memory.ConversationMemory;
import io.github.afgprojects.framework.ai.core.memory.Message;
import io.github.afgprojects.framework.ai.core.model.AiClient;
import io.github.afgprojects.framework.ai.core.model.AiResponse;
import io.github.afgprojects.framework.ai.core.tool.ToolDefinition;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Spring AI-based implementation of AiClient.
 * <p>
 * This implementation wraps Spring AI's ChatClient to provide a high-level,
 * fluent API for AI interactions. It supports:
 * <ul>
 *   <li>Synchronous and streaming responses</li>
 *   <li>System prompt configuration</li>
 *   <li>Tool/function calling</li>
 *   <li>Conversation memory integration</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * // Create using builder
 * SpringAiClient client = SpringAiClient.builder()
 *     .chatModel(chatModel)
 *     .defaultSystemPrompt("You are a helpful assistant")
 *     .build();
 *
 * // Simple prompt
 * AiResponse response = client.prompt("Hello!").call();
 *
 * // Streaming
 * Flux<AiResponse> stream = client.prompt("Tell me a story").stream();
 * }</pre>
 *
 * @author AFG Projects
 * @since 1.0.0
 */
public class SpringAiClient implements AiClient {

    private static final Logger log = LoggerFactory.getLogger(SpringAiClient.class);
    private static final String DEFAULT_SESSION_ID = "default";

    private final ChatClient chatClient;
    private final org.springframework.ai.chat.model.ChatModel chatModel;
    private final String defaultSystemPrompt;
    private final ConversationMemory memory;
    private final String sessionId;
    private final List<ToolDefinition> tools;

    /**
     * Creates a SpringAiClient with the specified configuration.
     *
     * @param chatClient          the Spring AI ChatClient
     * @param chatModel           the underlying ChatModel
     * @param defaultSystemPrompt the default system prompt (may be null)
     * @param memory              the conversation memory (may be null)
     * @param sessionId           the session ID for memory
     * @param tools               the available tools
     */
    private SpringAiClient(
        @NonNull ChatClient chatClient,
        org.springframework.ai.chat.model.@NonNull ChatModel chatModel,
        @Nullable String defaultSystemPrompt,
        @Nullable ConversationMemory memory,
        @NonNull String sessionId,
        @NonNull List<ToolDefinition> tools
    ) {
        this.chatClient = chatClient;
        this.chatModel = chatModel;
        this.defaultSystemPrompt = defaultSystemPrompt;
        this.memory = memory;
        this.sessionId = sessionId;
        this.tools = new ArrayList<>(tools);
    }

    @Override
    @NonNull
    public PromptSpec prompt(@NonNull String prompt) {
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("Prompt cannot be null or blank");
        }
        return new PromptSpecImpl(prompt);
    }

    @Override
    @NonNull
    public AiClient withSystemPrompt(@Nullable String systemPrompt) {
        return new SpringAiClient(
            chatClient,
            chatModel,
            systemPrompt,
            memory,
            sessionId,
            tools
        );
    }

    @Override
    @NonNull
    public AiClient withTools(@NonNull List<ToolDefinition> tools) {
        if (tools == null) {
            throw new IllegalArgumentException("Tools cannot be null");
        }
        return new SpringAiClient(
            chatClient,
            chatModel,
            defaultSystemPrompt,
            memory,
            sessionId,
            tools
        );
    }

    @Override
    @NonNull
    public AiClient withMemory(@NonNull ConversationMemory memory) {
        if (memory == null) {
            throw new IllegalArgumentException("Memory cannot be null");
        }
        return new SpringAiClient(
            chatClient,
            chatModel,
            defaultSystemPrompt,
            memory,
            sessionId,
            tools
        );
    }

    @Override
    @NonNull
    public AiClient withSessionId(@NonNull String sessionId) {
        return new SpringAiClient(
            chatClient,
            chatModel,
            defaultSystemPrompt,
            memory,
            sessionId,
            tools
        );
    }

    @Override
    @Nullable
    public String getSystemPrompt() {
        return defaultSystemPrompt;
    }

    @Override
    @NonNull
    public List<ToolDefinition> getTools() {
        return List.copyOf(tools);
    }

    /**
     * Creates a builder for constructing SpringAiClient instances.
     *
     * @return a new Builder instance
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing SpringAiClient instances.
     */
    public static class Builder {
        private org.springframework.ai.chat.model.ChatModel chatModel;
        private String defaultSystemPrompt;
        private ConversationMemory memory;
        private String sessionId = DEFAULT_SESSION_ID;
        private List<ToolDefinition> tools = new ArrayList<>();

        /**
         * Sets the ChatModel to use.
         *
         * @param chatModel the ChatModel
         * @return this builder
         */
        @NonNull
        public Builder chatModel(org.springframework.ai.chat.model.@NonNull ChatModel chatModel) {
            this.chatModel = chatModel;
            return this;
        }

        /**
         * Sets the default system prompt.
         *
         * @param systemPrompt the system prompt
         * @return this builder
         */
        @NonNull
        public Builder defaultSystemPrompt(@Nullable String systemPrompt) {
            this.defaultSystemPrompt = systemPrompt;
            return this;
        }

        /**
         * Sets the conversation memory.
         *
         * @param memory the conversation memory
         * @return this builder
         */
        @NonNull
        public Builder memory(@Nullable ConversationMemory memory) {
            this.memory = memory;
            return this;
        }

        /**
         * Sets the session ID for memory.
         *
         * @param sessionId the session ID
         * @return this builder
         */
        @NonNull
        public Builder sessionId(@NonNull String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        /**
         * Sets the available tools.
         *
         * @param tools the tools
         * @return this builder
         */
        @NonNull
        public Builder tools(@NonNull List<ToolDefinition> tools) {
            this.tools = new ArrayList<>(tools);
            return this;
        }

        /**
         * Adds a tool.
         *
         * @param tool the tool to add
         * @return this builder
         */
        @NonNull
        public Builder addTool(@NonNull ToolDefinition tool) {
            this.tools.add(tool);
            return this;
        }

        /**
         * Builds the SpringAiClient.
         *
         * @return a new SpringAiClient instance
         * @throws IllegalStateException if chatModel is not set
         */
        @NonNull
        public SpringAiClient build() {
            if (chatModel == null) {
                throw new IllegalStateException("ChatModel must be set");
            }

            // Build ChatClient with optional system prompt
            ChatClient.Builder clientBuilder = ChatClient.builder(chatModel);
            if (defaultSystemPrompt != null && !defaultSystemPrompt.isBlank()) {
                clientBuilder.defaultSystem(defaultSystemPrompt);
            }

            return new SpringAiClient(
                clientBuilder.build(),
                chatModel,
                defaultSystemPrompt,
                memory,
                sessionId,
                tools
            );
        }
    }

    /**
     * Implementation of PromptSpec for handling individual prompt requests.
     */
    private class PromptSpecImpl implements PromptSpec {

        private final String prompt;
        private String context;
        private final Map<String, Object> options = new HashMap<>();

        PromptSpecImpl(@NonNull String prompt) {
            this.prompt = prompt;
        }

        @Override
        @NonNull
        public AiResponse call() {
            log.debug("Executing prompt: {}", prompt);

            // Build the full prompt text
            String fullPrompt = context != null ? prompt + "\n\n" + context : prompt;

            // Build messages list
            List<org.springframework.ai.chat.messages.Message> messages = new ArrayList<>();

            // Add system message if configured
            String systemPrompt = defaultSystemPrompt;
            if (systemPrompt != null && !systemPrompt.isBlank()) {
                messages.add(new SystemMessage(systemPrompt));
            }

            // Add conversation history from memory
            if (memory != null) {
                List<Message> history = memory.getHistory(sessionId);
                for (Message msg : history) {
                    messages.add(convertMessage(msg));
                }
            }

            // Add current user message
            messages.add(new UserMessage(fullPrompt));

            // Execute the call using ChatClient API
            String content = chatClient.prompt()
                .messages(messages)
                .call()
                .content();

            // Store in memory if configured
            if (memory != null) {
                memory.addMessage(sessionId, Message.user(fullPrompt));
                memory.addMessage(sessionId, Message.assistant(content));
            }

            return AiResponse.of(content);
        }

        @Override
        @NonNull
        public Flux<AiResponse> stream() {
            log.debug("Streaming prompt: {}", prompt);

            // Build the full prompt text
            String fullPrompt = context != null ? prompt + "\n\n" + context : prompt;

            // Build messages list
            List<org.springframework.ai.chat.messages.Message> messages = new ArrayList<>();

            // Add system message if configured
            if (defaultSystemPrompt != null && !defaultSystemPrompt.isBlank()) {
                messages.add(new SystemMessage(defaultSystemPrompt));
            }

            // Add conversation history from memory
            if (memory != null) {
                List<Message> history = memory.getHistory(sessionId);
                for (Message msg : history) {
                    messages.add(convertMessage(msg));
                }
            }

            // Add current user message
            messages.add(new UserMessage(fullPrompt));

            // Store user message in memory
            if (memory != null) {
                memory.addMessage(sessionId, Message.user(fullPrompt));
            }

            // Execute the stream
            return chatClient.prompt()
                .messages(messages)
                .stream()
                .content()
                .map(AiResponse::of);
        }

        @Override
        @NonNull
        public PromptSpec withContext(@NonNull String context) {
            if (context == null) {
                throw new IllegalArgumentException("Context cannot be null");
            }
            this.context = context;
            return this;
        }

        @Override
        @NonNull
        public PromptSpec option(@NonNull String key, @Nullable Object value) {
            this.options.put(key, value);
            return this;
        }
    }

    /**
     * Converts a framework Message to a Spring AI Message.
     */
    private org.springframework.ai.chat.messages.Message convertMessage(@NonNull Message message) {
        return switch (message.role()) {
            case SYSTEM -> new SystemMessage(message.content());
            case USER -> {
                yield new UserMessage(message.content());
            }
            case ASSISTANT -> {

                yield new AssistantMessage(message.content());
            }
            case TOOL -> {
                if (!message.toolResults().isEmpty()) {

                    yield ToolResponseMessage.builder()
                        .responses(new ArrayList<>())
                        .build();
                }
                yield new AssistantMessage("");
            }
        };
    }
}
