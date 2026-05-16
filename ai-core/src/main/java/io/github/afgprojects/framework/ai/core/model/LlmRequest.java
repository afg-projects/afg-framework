package io.github.afgprojects.framework.ai.core.model;

import io.github.afgprojects.framework.ai.core.memory.Message;
import io.github.afgprojects.framework.ai.core.tool.ToolDefinition;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a request to an LLM.
 * <p>
 * LlmRequest contains all the information needed to make a request
 * to an LLM:
 * <ul>
 *   <li>systemPrompt - the system message to set the behavior</li>
 *   <li>messages - the conversation history</li>
 *   <li>tools - the tools available for the LLM to call</li>
 *   <li>options - additional provider-specific options</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * LlmRequest request = new LlmRequest(
 *     "You are a helpful assistant.",
 *     List.of(Message.user("Hello!")),
 *     List.of(),
 *     Map.of("temperature", 0.7)
 * );
 *
 * // Using builder pattern
 * LlmRequest request = LlmRequest.builder()
 *     .systemPrompt("You are a helpful assistant.")
 *     .addMessage(Message.user("Hello!"))
 *     .addTool(searchTool)
 *     .option("temperature", 0.7)
 *     .build();
 * }</pre>
 *
 * @param systemPrompt the system message to set the behavior
 * @param messages     the conversation history
 * @param tools        the tools available for the LLM to call
 * @param options      additional provider-specific options
 * @author AFG Projects
 * @since 1.0.0
 */
public record LlmRequest(
    @Nullable String systemPrompt,
    @NonNull List<Message> messages,
    @NonNull List<ToolDefinition> tools,
    @NonNull Map<String, Object> options
) {

    /**
     * Creates an LlmRequest with validated parameters.
     * <p>
     * Null safety is ensured:
     * <ul>
     *   <li>systemPrompt can be null</li>
     *   <li>messages defaults to empty list if null</li>
     *   <li>tools defaults to empty list if null</li>
     *   <li>options defaults to empty map if null</li>
     * </ul>
     *
     * @param systemPrompt the system message
     * @param messages     the conversation history
     * @param tools        the available tools
     * @param options      additional options
     */
    public LlmRequest {
        if (messages == null) {
            messages = List.of();
        } else {
            messages = Collections.unmodifiableList(new ArrayList<>(messages));
        }
        if (tools == null) {
            tools = List.of();
        } else {
            tools = Collections.unmodifiableList(new ArrayList<>(tools));
        }
        if (options == null) {
            options = Map.of();
        } else {
            options = Collections.unmodifiableMap(new HashMap<>(options));
        }
    }

    /**
     * Creates an LlmRequest with only messages.
     *
     * @param messages the conversation history
     * @return a new LlmRequest instance
     */
    @NonNull
    public static LlmRequest of(@NonNull List<Message> messages) {
        return new LlmRequest(null, messages, List.of(), Map.of());
    }

    /**
     * Creates an LlmRequest with a single user message.
     *
     * @param userMessage the user message content
     * @return a new LlmRequest instance
     */
    @NonNull
    public static LlmRequest ofUserMessage(@Nullable String userMessage) {
        return new LlmRequest(null, List.of(Message.user(userMessage)), List.of(), Map.of());
    }

    /**
     * Creates a new LlmRequest with a different system prompt.
     *
     * @param systemPrompt the new system prompt
     * @return a new LlmRequest instance
     */
    @NonNull
    public LlmRequest withSystemPrompt(@Nullable String systemPrompt) {
        return new LlmRequest(systemPrompt, this.messages, this.tools, this.options);
    }

    /**
     * Creates a new LlmRequest with an additional message.
     *
     * @param message the message to add
     * @return a new LlmRequest instance
     */
    @NonNull
    public LlmRequest withMessage(@NonNull Message message) {
        List<Message> newMessages = new ArrayList<>(this.messages);
        newMessages.add(message);
        return new LlmRequest(this.systemPrompt, newMessages, this.tools, this.options);
    }

    /**
     * Creates a new LlmRequest with additional messages.
     *
     * @param messages the messages to add
     * @return a new LlmRequest instance
     */
    @NonNull
    public LlmRequest withMessages(@NonNull List<Message> messages) {
        List<Message> newMessages = new ArrayList<>(this.messages);
        newMessages.addAll(messages);
        return new LlmRequest(this.systemPrompt, newMessages, this.tools, this.options);
    }

    /**
     * Creates a new LlmRequest with an additional tool.
     *
     * @param tool the tool to add
     * @return a new LlmRequest instance
     */
    @NonNull
    public LlmRequest withTool(@NonNull ToolDefinition tool) {
        List<ToolDefinition> newTools = new ArrayList<>(this.tools);
        newTools.add(tool);
        return new LlmRequest(this.systemPrompt, this.messages, newTools, this.options);
    }

    /**
     * Creates a new LlmRequest with additional tools.
     *
     * @param tools the tools to add
     * @return a new LlmRequest instance
     */
    @NonNull
    public LlmRequest withTools(@NonNull List<ToolDefinition> tools) {
        List<ToolDefinition> newTools = new ArrayList<>(this.tools);
        newTools.addAll(tools);
        return new LlmRequest(this.systemPrompt, this.messages, newTools, this.options);
    }

    /**
     * Creates a new LlmRequest with an additional option.
     *
     * @param key   the option key
     * @param value the option value
     * @return a new LlmRequest instance
     */
    @NonNull
    public LlmRequest withOption(@NonNull String key, @Nullable Object value) {
        Map<String, Object> newOptions = new HashMap<>(this.options);
        newOptions.put(key, value);
        return new LlmRequest(this.systemPrompt, this.messages, this.tools, newOptions);
    }

    /**
     * Checks if this request has tools.
     *
     * @return true if tools are available
     */
    public boolean hasTools() {
        return !tools.isEmpty();
    }

    /**
     * Checks if this request has a system prompt.
     *
     * @return true if system prompt is present
     */
    public boolean hasSystemPrompt() {
        return systemPrompt != null && !systemPrompt.isBlank();
    }

    /**
     * Gets an option value by key.
     *
     * @param key the option key
     * @return the option value, or null if not present
     */
    @Nullable
    public Object getOption(@NonNull String key) {
        return options.get(key);
    }

    /**
     * Gets an option value by key with a default.
     *
     * @param key          the option key
     * @param defaultValue the default value
     * @param <T>          the value type
     * @return the option value, or the default if not present
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getOption(@NonNull String key, @Nullable T defaultValue) {
        Object value = options.get(key);
        if (value == null) {
            return defaultValue;
        }
        return (T) value;
    }

    /**
     * Creates a builder for constructing LlmRequest instances.
     *
     * @return a new Builder instance
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing LlmRequest instances.
     */
    public static class Builder {
        private String systemPrompt;
        private final List<Message> messages = new ArrayList<>();
        private final List<ToolDefinition> tools = new ArrayList<>();
        private final Map<String, Object> options = new HashMap<>();

        /**
         * Sets the system prompt.
         *
         * @param systemPrompt the system prompt
         * @return this builder
         */
        @NonNull
        public Builder systemPrompt(@Nullable String systemPrompt) {
            this.systemPrompt = systemPrompt;
            return this;
        }

        /**
         * Adds a message.
         *
         * @param message the message to add
         * @return this builder
         */
        @NonNull
        public Builder addMessage(@NonNull Message message) {
            this.messages.add(message);
            return this;
        }

        /**
         * Adds multiple messages.
         *
         * @param messages the messages to add
         * @return this builder
         */
        @NonNull
        public Builder addMessages(@NonNull List<Message> messages) {
            this.messages.addAll(messages);
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
         * Adds multiple tools.
         *
         * @param tools the tools to add
         * @return this builder
         */
        @NonNull
        public Builder addTools(@NonNull List<ToolDefinition> tools) {
            this.tools.addAll(tools);
            return this;
        }

        /**
         * Adds an option.
         *
         * @param key   the option key
         * @param value the option value
         * @return this builder
         */
        @NonNull
        public Builder option(@NonNull String key, @Nullable Object value) {
            this.options.put(key, value);
            return this;
        }

        /**
         * Adds multiple options.
         *
         * @param options the options to add
         * @return this builder
         */
        @NonNull
        public Builder options(@NonNull Map<String, Object> options) {
            this.options.putAll(options);
            return this;
        }

        /**
         * Builds the LlmRequest.
         *
         * @return a new LlmRequest instance
         */
        @NonNull
        public LlmRequest build() {
            return new LlmRequest(systemPrompt, messages, tools, options);
        }
    }
}
