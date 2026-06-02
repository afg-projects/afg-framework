package io.github.afgprojects.framework.ai.langchain4j.chat;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import io.github.afgprojects.framework.ai.core.api.chat.AfgChatClient;
import io.github.afgprojects.framework.ai.core.api.chat.AiChatMetadata;
import io.github.afgprojects.framework.ai.core.api.chat.AiChatResponse;
import io.github.afgprojects.framework.ai.langchain4j.internal.Lc4jMessageConverter;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * AfgChatClient 的 LangChain4j 实现 - 委托 LangChain4j ChatLanguageModel / StreamingChatLanguageModel
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class Lc4jChatClient implements AfgChatClient {

    private final ChatLanguageModel chatModel;
    private final StreamingChatLanguageModel streamingChatModel;
    private final String systemPrompt;
    private final String modelName;
    private final String conversationId;

    public Lc4jChatClient(@NonNull ChatLanguageModel chatModel) {
        this(chatModel, null, null, null);
    }

    public Lc4jChatClient(@NonNull ChatLanguageModel chatModel,
                          @Nullable StreamingChatLanguageModel streamingChatModel) {
        this(chatModel, streamingChatModel, null, null, null);
    }

    private Lc4jChatClient(@NonNull ChatLanguageModel chatModel,
                           @Nullable String systemPrompt,
                           @Nullable String modelName,
                           @Nullable String conversationId) {
        this(chatModel, null, systemPrompt, modelName, conversationId);
    }

    private Lc4jChatClient(@NonNull ChatLanguageModel chatModel,
                           @Nullable StreamingChatLanguageModel streamingChatModel,
                           @Nullable String systemPrompt,
                           @Nullable String modelName,
                           @Nullable String conversationId) {
        this.chatModel = chatModel;
        this.streamingChatModel = streamingChatModel;
        this.systemPrompt = systemPrompt;
        this.modelName = modelName;
        this.conversationId = conversationId;
    }

    // ---- 同步对话 ----

    @Override
    public @NonNull AiChatResponse chat(@NonNull String userMessage) {
        List<ChatMessage> messages = buildMessages(userMessage);
        Response<AiMessage> response = chatModel.generate(messages);
        return convertResponse(response);
    }

    @Override
    public @NonNull AiChatResponse chat(@NonNull String conversationId, @NonNull String userMessage) {
        // conversationId 在 LangChain4j 中需要通过 ChatMemory 管理
        // 此处简化：直接构建消息（ChatMemory 由外部 Lc4jChatMemoryAdapter 管理）
        List<ChatMessage> messages = buildMessages(userMessage);
        Response<AiMessage> response = chatModel.generate(messages);
        return convertResponse(response);
    }

    @Override
    public @NonNull AiChatResponse chat(@NonNull io.github.afgprojects.framework.ai.core.api.chat.AiMessage message) {
        List<ChatMessage> messages = new ArrayList<>();
        if (systemPrompt != null) {
            messages.add(SystemMessage.from(systemPrompt));
        }
        messages.add(Lc4jMessageConverter.toLc4j(message));
        Response<AiMessage> response = chatModel.generate(messages);
        return convertResponse(response);
    }

    @Override
    public @NonNull AiChatResponse chat(@NonNull List<io.github.afgprojects.framework.ai.core.api.chat.AiMessage> messages) {
        List<ChatMessage> lc4jMessages = new ArrayList<>();
        if (systemPrompt != null) {
            lc4jMessages.add(SystemMessage.from(systemPrompt));
        }
        for (var msg : messages) {
            lc4jMessages.add(Lc4jMessageConverter.toLc4j(msg));
        }
        Response<AiMessage> response = chatModel.generate(lc4jMessages);
        return convertResponse(response);
    }

    // ---- 流式对话 ----

    @Override
    public @NonNull Flux<String> chatStream(@NonNull String userMessage) {
        if (streamingChatModel == null) {
            throw new UnsupportedOperationException(
                "Streaming is not available: no StreamingChatLanguageModel configured");
        }
        List<ChatMessage> messages = buildMessages(userMessage);
        Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();
        streamingChatModel.generate(messages, new dev.langchain4j.model.output.StreamingResponseHandler<AiMessage>() {
            @Override
            public void onPartialResponse(String partialResponse) {
                sink.tryEmitNext(partialResponse);
            }

            @Override
            public void onComplete(Response<AiMessage> response) {
                sink.tryEmitComplete();
            }

            @Override
            public void onError(Throwable error) {
                sink.tryEmitError(error);
            }
        });
        return sink.asFlux();
    }

    @Override
    public @NonNull Flux<String> chatStream(@NonNull String conversationId, @NonNull String userMessage) {
        return chatStream(userMessage);
    }

    // ---- 结构化输出 ----

    @Override
    public <T> @NonNull T chat(@NonNull String userMessage, @NonNull Class<T> responseType) {
        // LangChain4j 的结构化输出需要配合 @Description 注解的 POJO
        // 这里使用简化实现：通过 chat 获取文本，再由 Jackson 反序列化
        AiChatResponse response = chat(userMessage);
        if (response.content() == null) {
            throw new IllegalStateException("AI response content is null, cannot deserialize to " + responseType.getName());
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            return mapper.readValue(response.content(), responseType);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize AI response to " + responseType.getName(), e);
        }
    }

    // ---- 构建器式调用 ----

    @Override
    public @NonNull ChatRequestSpec prompt(@NonNull String userMessage) {
        return new Lc4jChatRequestSpec(userMessage);
    }

    // ---- 配置切换（返回新实例） ----

    @Override
    public @NonNull AfgChatClient withSystemPrompt(@Nullable String systemPrompt) {
        return new Lc4jChatClient(this.chatModel, this.streamingChatModel, systemPrompt, this.modelName, this.conversationId);
    }

    @Override
    public @NonNull AfgChatClient withModel(@Nullable String modelName) {
        // LangChain4j 的模型切换通常需要创建新的模型实例
        // 此处仅记录 modelName，实际模型切换在构建器中处理
        return new Lc4jChatClient(this.chatModel, this.streamingChatModel, this.systemPrompt, modelName, this.conversationId);
    }

    @Override
    public @NonNull AfgChatClient withConversationId(@Nullable String conversationId) {
        return new Lc4jChatClient(this.chatModel, this.streamingChatModel, this.systemPrompt, this.modelName, conversationId);
    }

    // ---- 内部方法 ----

    private List<ChatMessage> buildMessages(String userMessage) {
        List<ChatMessage> messages = new ArrayList<>();
        if (systemPrompt != null) {
            messages.add(SystemMessage.from(systemPrompt));
        }
        messages.add(UserMessage.from(userMessage));
        return messages;
    }

    private AiChatResponse convertResponse(Response<AiMessage> response) {
        if (response == null) {
            return AiChatResponse.of(null);
        }

        AiMessage aiMessage = response.content();
        String content = aiMessage != null ? aiMessage.text() : null;

        AiChatMetadata metadata = null;
        var tokenUsage = response.tokenUsage();
        var finishReason = response.finishReason();
        if (tokenUsage != null || finishReason != null) {
            metadata = new AiChatMetadata(
                tokenUsage != null ? (long) tokenUsage.inputTokenCount() : null,
                tokenUsage != null ? (long) tokenUsage.outputTokenCount() : null,
                tokenUsage != null ? (long) tokenUsage.totalTokenCount() : null,
                finishReason != null ? finishReason.name() : null,
                null // LangChain4j Response 不直接返回模型名称
            );
        }

        return AiChatResponse.of(content, metadata);
    }

    // ---- ChatRequestSpec 实现 ----

    private class Lc4jChatRequestSpec implements ChatRequestSpec {

        private String specSystemPrompt = systemPrompt;
        private String specConversationId = conversationId;
        private Map<String, Object> specOptions = Map.of();
        private final String userMessage;

        Lc4jChatRequestSpec(@NonNull String userMessage) {
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
            List<ChatMessage> messages = new ArrayList<>();
            if (specSystemPrompt != null) {
                messages.add(SystemMessage.from(specSystemPrompt));
            }
            messages.add(UserMessage.from(userMessage));
            Response<AiMessage> response = chatModel.generate(messages);
            return convertResponse(response);
        }

        @Override
        public @NonNull Flux<String> stream() {
            if (streamingChatModel == null) {
                throw new UnsupportedOperationException(
                    "Streaming is not available: no StreamingChatLanguageModel configured");
            }
            List<ChatMessage> messages = new ArrayList<>();
            if (specSystemPrompt != null) {
                messages.add(SystemMessage.from(specSystemPrompt));
            }
            messages.add(UserMessage.from(userMessage));
            Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();
            streamingChatModel.generate(messages, new dev.langchain4j.model.output.StreamingResponseHandler<AiMessage>() {
                @Override
                public void onPartialResponse(String partialResponse) {
                    sink.tryEmitNext(partialResponse);
                }

                @Override
                public void onComplete(Response<AiMessage> response) {
                    sink.tryEmitComplete();
                }

                @Override
                public void onError(Throwable error) {
                    sink.tryEmitError(error);
                }
            });
            return sink.asFlux();
        }

        @Override
        public <T> @NonNull T entity(@NonNull Class<T> type) {
            AiChatResponse response = call();
            if (response.content() == null) {
                throw new IllegalStateException("AI response content is null, cannot deserialize to " + type.getName());
            }
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
                return mapper.readValue(response.content(), type);
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                throw new RuntimeException("Failed to deserialize AI response to " + type.getName(), e);
            }
        }
    }
}
