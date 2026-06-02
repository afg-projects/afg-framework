package io.github.afgprojects.framework.ai.spring.chat;

import io.github.afgprojects.framework.ai.core.api.chat.AfgChatClient;
import io.github.afgprojects.framework.ai.core.api.chat.AiChatMetadata;
import io.github.afgprojects.framework.ai.core.api.chat.AiChatResponse;
import io.github.afgprojects.framework.ai.core.api.chat.AiMessage;
import io.github.afgprojects.framework.ai.spring.memory.AiMessageConverter;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * AfgChatClient 的 Spring AI 实现 - 委托 Spring AI ChatClient
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class SpringAiChatClient implements AfgChatClient {

    private final ChatClient chatClient;
    private final String systemPrompt;
    private final String modelName;
    private final String conversationId;

    public SpringAiChatClient(@NonNull ChatClient chatClient) {
        this(chatClient, null, null, null);
    }

    private SpringAiChatClient(
            @NonNull ChatClient chatClient,
            @Nullable String systemPrompt,
            @Nullable String modelName,
            @Nullable String conversationId) {
        this.chatClient = chatClient;
        this.systemPrompt = systemPrompt;
        this.modelName = modelName;
        this.conversationId = conversationId;
    }

    // ---- 同步对话 ----

    @Override
    public @NonNull AiChatResponse chat(@NonNull String userMessage) {
        var spec = chatClient.prompt().user(userMessage);
        applyDefaults(spec);
        return convertResponse(spec.call().chatResponse());
    }

    @Override
    public @NonNull AiChatResponse chat(@NonNull String conversationId, @NonNull String userMessage) {
        var spec = chatClient.prompt().user(userMessage);
        applyDefaults(spec);
        applyConversationId(spec, conversationId);
        return convertResponse(spec.call().chatResponse());
    }

    @Override
    public @NonNull AiChatResponse chat(@NonNull AiMessage message) {
        var springMessage = AiMessageConverter.toSpringAi(message);
        var spec = chatClient.prompt().messages(springMessage);
        applyDefaults(spec);
        return convertResponse(spec.call().chatResponse());
    }

    @Override
    public @NonNull AiChatResponse chat(@NonNull List<AiMessage> messages) {
        var springMessages = AiMessageConverter.toSpringAiMessages(messages);
        var spec = chatClient.prompt().messages(springMessages);
        applyDefaults(spec);
        return convertResponse(spec.call().chatResponse());
    }

    // ---- 流式对话 ----

    @Override
    public @NonNull Flux<String> chatStream(@NonNull String userMessage) {
        var spec = chatClient.prompt().user(userMessage);
        applyDefaults(spec);
        return spec.stream().content();
    }

    @Override
    public @NonNull Flux<String> chatStream(@NonNull String conversationId, @NonNull String userMessage) {
        var spec = chatClient.prompt().user(userMessage);
        applyDefaults(spec);
        applyConversationId(spec, conversationId);
        return spec.stream().content();
    }

    // ---- 结构化输出 ----

    @Override
    public <T> @NonNull T chat(@NonNull String userMessage, @NonNull Class<T> responseType) {
        var spec = chatClient.prompt().user(userMessage);
        applyDefaults(spec);
        return spec.call().entity(responseType);
    }

    // ---- 构建器式调用 ----

    @Override
    public @NonNull ChatRequestSpec prompt(@NonNull String userMessage) {
        return new DefaultChatRequestSpec(userMessage);
    }

    // ---- 配置切换（返回新实例） ----

    @Override
    public @NonNull AfgChatClient withSystemPrompt(@Nullable String systemPrompt) {
        return new SpringAiChatClient(this.chatClient, systemPrompt, this.modelName, this.conversationId);
    }

    @Override
    public @NonNull AfgChatClient withModel(@Nullable String modelName) {
        return new SpringAiChatClient(this.chatClient, this.systemPrompt, modelName, this.conversationId);
    }

    @Override
    public @NonNull AfgChatClient withConversationId(@Nullable String conversationId) {
        return new SpringAiChatClient(this.chatClient, this.systemPrompt, this.modelName, conversationId);
    }

    // ---- 内部方法 ----

    private void applyDefaults(ChatClient.ChatClientRequestSpec spec) {
        if (systemPrompt != null) {
            spec.system(systemPrompt);
        }
        if (modelName != null) {
            spec.options(ChatOptions.builder().model(modelName));
        }
        if (conversationId != null) {
            applyConversationId(spec, conversationId);
        }
    }

    private void applyConversationId(ChatClient.ChatClientRequestSpec spec, String convId) {
        spec.advisors(a -> a.param("chat_memory_conversation_id", convId));
    }

    private AiChatResponse convertResponse(@Nullable ChatResponse response) {
        if (response == null) {
            return AiChatResponse.of(null);
        }

        var result = response.getResult();
        String content = result != null && result.getOutput() != null
                ? result.getOutput().getText()
                : null;

        AiChatMetadata metadata = null;
        var meta = response.getMetadata();
        if (meta != null) {
            var usage = meta.getUsage();
            String finishReason = null;
            if (result != null && result.getMetadata() != null) {
                finishReason = result.getMetadata().getFinishReason();
            }
            metadata = new AiChatMetadata(
                usage != null && usage.getPromptTokens() != null ? usage.getPromptTokens().longValue() : null,
                usage != null && usage.getCompletionTokens() != null ? usage.getCompletionTokens().longValue() : null,
                usage != null && usage.getTotalTokens() != null ? usage.getTotalTokens().longValue() : null,
                finishReason,
                meta.getModel()
            );
        }

        return AiChatResponse.of(content, metadata);
    }

    private static ChatOptions.Builder<?> buildChatOptions(@NonNull Map<String, Object> options) {
        var builder = ChatOptions.builder();
        if (options.containsKey("model")) {
            builder.model((String) options.get("model"));
        }
        if (options.containsKey("temperature")) {
            builder.temperature(((Number) options.get("temperature")).doubleValue());
        }
        if (options.containsKey("maxTokens")) {
            builder.maxTokens(((Number) options.get("maxTokens")).intValue());
        }
        if (options.containsKey("topP")) {
            builder.topP(((Number) options.get("topP")).doubleValue());
        }
        return builder;
    }

    // ---- ChatRequestSpec 实现 ----

    private class DefaultChatRequestSpec implements ChatRequestSpec {

        private String specSystemPrompt = systemPrompt;
        private String specConversationId = conversationId;
        private Map<String, Object> specOptions = Map.of();
        private final String userMessage;

        DefaultChatRequestSpec(@NonNull String userMessage) {
            this.userMessage = userMessage;
        }

        @Override
        public ChatRequestSpec systemPrompt(@Nullable String systemPrompt) {
            this.specSystemPrompt = systemPrompt;
            return this;
        }

        @Override
        public ChatRequestSpec conversationId(@Nullable String conversationId) {
            this.specConversationId = conversationId;
            return this;
        }

        @Override
        public ChatRequestSpec options(@NonNull Map<String, Object> options) {
            this.specOptions = options;
            return this;
        }

        @Override
        public @NonNull AiChatResponse call() {
            var spec = chatClient.prompt().user(userMessage);

            if (specSystemPrompt != null) {
                spec.system(specSystemPrompt);
            }
            if (specConversationId != null) {
                applyConversationId(spec, specConversationId);
            }
            if (!specOptions.isEmpty()) {
                spec.options(buildChatOptions(specOptions));
            }

            return convertResponse(spec.call().chatResponse());
        }

        @Override
        public @NonNull Flux<String> stream() {
            var spec = chatClient.prompt().user(userMessage);

            if (specSystemPrompt != null) {
                spec.system(specSystemPrompt);
            }
            if (specConversationId != null) {
                applyConversationId(spec, specConversationId);
            }
            if (!specOptions.isEmpty()) {
                spec.options(buildChatOptions(specOptions));
            }

            return spec.stream().content();
        }

        @Override
        public <T> @NonNull T entity(@NonNull Class<T> type) {
            var spec = chatClient.prompt().user(userMessage);

            if (specSystemPrompt != null) {
                spec.system(specSystemPrompt);
            }
            if (specConversationId != null) {
                applyConversationId(spec, specConversationId);
            }
            if (!specOptions.isEmpty()) {
                spec.options(buildChatOptions(specOptions));
            }

            return spec.call().entity(type);
        }
    }
}
