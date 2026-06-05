package io.github.afgprojects.framework.ai.langchain4j.chat;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import io.github.afgprojects.framework.ai.core.api.chat.AfgChatClient;
import io.github.afgprojects.framework.ai.core.api.chat.AiChatMetadata;
import io.github.afgprojects.framework.ai.core.api.chat.AiChatResponse;
import io.github.afgprojects.framework.ai.langchain4j.internal.Lc4jMessageConverter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * AfgChatClient 的 LangChain4j 实现 - 委托 LangChain4j ChatModel / StreamingChatModel
 *
 * <p>将 afg-framework 的 AI 聊天接口适配到 LangChain4j 的模型接口：
 * <ul>
 *   <li>同步调用：{@link ChatModel#chat(List)}</li>
 *   <li>流式调用：{@link StreamingChatModel#chat(List, StreamingChatResponseHandler)}</li>
 * </ul>
 *
 * <p>所有配置切换（systemPrompt / model / conversationId）返回新实例，保持不可变性。
 *
 * @author afg-framework
 * @since 1.0.0
 */
public class Lc4jChatClient implements AfgChatClient {

    private final ChatModel chatModel;
    private final StreamingChatModel streamingChatModel;
    private final String systemPrompt;
    private final String modelName;
    private final String conversationId;

    public Lc4jChatClient(ChatModel chatModel) {
        this.chatModel = Objects.requireNonNull(chatModel, "chatModel must not be null");
        this.streamingChatModel = null;
        this.systemPrompt = null;
        this.modelName = null;
        this.conversationId = null;
    }

    public Lc4jChatClient(ChatModel chatModel,
                          StreamingChatModel streamingChatModel) {
        this.chatModel = Objects.requireNonNull(chatModel, "chatModel must not be null");
        this.streamingChatModel = streamingChatModel;
        this.systemPrompt = null;
        this.modelName = null;
        this.conversationId = null;
    }

    private Lc4jChatClient(ChatModel chatModel,
                           String systemPrompt,
                           String modelName,
                           String conversationId) {
        this.chatModel = Objects.requireNonNull(chatModel, "chatModel must not be null");
        this.streamingChatModel = null;
        this.systemPrompt = systemPrompt;
        this.modelName = modelName;
        this.conversationId = conversationId;
    }

    private Lc4jChatClient(ChatModel chatModel,
                           StreamingChatModel streamingChatModel,
                           String systemPrompt,
                           String modelName,
                           String conversationId) {
        this.chatModel = Objects.requireNonNull(chatModel, "chatModel must not be null");
        this.streamingChatModel = streamingChatModel;
        this.systemPrompt = systemPrompt;
        this.modelName = modelName;
        this.conversationId = conversationId;
    }

    // ---- 同步对话 ----

    @Override
    public AiChatResponse chat(String userMessage) {
        List<ChatMessage> messages = buildMessages(userMessage);
        ChatResponse response = chatModel.chat(messages);
        return convertResponse(response);
    }

    @Override
    public AiChatResponse chat(String conversationId, String userMessage) {
        // conversationId 在 LangChain4j 中需要通过 ChatMemory 管理
        // 此处简化：直接构建消息（ChatMemory 由外部 Lc4jChatMemoryAdapter 管理）
        List<ChatMessage> messages = buildMessages(userMessage);
        ChatResponse response = chatModel.chat(messages);
        return convertResponse(response);
    }

    @Override
    public AiChatResponse chat(io.github.afgprojects.framework.ai.core.api.chat.AiMessage message) {
        List<ChatMessage> messages = new ArrayList<>();
        if (systemPrompt != null) {
            messages.add(SystemMessage.from(systemPrompt));
        }
        messages.add(Lc4jMessageConverter.toLc4j(message));
        ChatResponse response = chatModel.chat(messages);
        return convertResponse(response);
    }

    @Override
    public AiChatResponse chat(List<io.github.afgprojects.framework.ai.core.api.chat.AiMessage> messages) {
        List<ChatMessage> lc4jMessages = new ArrayList<>();
        if (systemPrompt != null) {
            lc4jMessages.add(SystemMessage.from(systemPrompt));
        }
        for (var msg : messages) {
            lc4jMessages.add(Lc4jMessageConverter.toLc4j(msg));
        }
        ChatResponse response = chatModel.chat(lc4jMessages);
        return convertResponse(response);
    }

    // ---- 流式对话 ----

    @Override
    public Flux<String> chatStream(String userMessage) {
        if (streamingChatModel == null) {
            throw new UnsupportedOperationException(
                "Streaming is not available: no StreamingChatModel configured");
        }
        List<ChatMessage> messages = buildMessages(userMessage);
        Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();
        streamingChatModel.chat(messages, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                sink.tryEmitNext(partialResponse);
            }

            @Override
            public void onCompleteResponse(ChatResponse response) {
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
    public Flux<String> chatStream(String conversationId, String userMessage) {
        return chatStream(userMessage);
    }

    // ---- 结构化输出 ----

    @Override
    public <T> T chat(String userMessage, Class<T> responseType) {
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
    public AfgChatClient.ChatRequestSpec prompt(String userMessage) {
        return new Lc4jChatRequestSpec(userMessage);
    }

    // ---- 配置切换（返回新实例） ----

    @Override
    public AfgChatClient withSystemPrompt(String systemPrompt) {
        return new Lc4jChatClient(this.chatModel, this.streamingChatModel, systemPrompt, this.modelName, this.conversationId);
    }

    @Override
    public AfgChatClient withModel(String modelName) {
        // LangChain4j 的模型切换通常需要创建新的模型实例
        // 此处仅记录 modelName，实际模型切换在构建器中处理
        return new Lc4jChatClient(this.chatModel, this.streamingChatModel, this.systemPrompt, modelName, this.conversationId);
    }

    @Override
    public AfgChatClient withConversationId(String conversationId) {
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

    private AiChatResponse convertResponse(ChatResponse response) {
        AiMessage aiMessage = response.aiMessage();
        String content = aiMessage != null ? aiMessage.text() : null;

        AiChatMetadata metadata = null;
        if (response.tokenUsage() != null || response.finishReason() != null) {
            metadata = new AiChatMetadata(
                response.tokenUsage() != null ? (long) response.tokenUsage().inputTokenCount() : null,
                response.tokenUsage() != null ? (long) response.tokenUsage().outputTokenCount() : null,
                response.tokenUsage() != null ? (long) response.tokenUsage().totalTokenCount() : null,
                response.finishReason() != null ? response.finishReason().name() : null,
                response.modelName()
            );
        }

        return AiChatResponse.of(content, metadata);
    }

    // ---- 内部 ChatRequestSpec 实现 ----

    private class Lc4jChatRequestSpec implements AfgChatClient.ChatRequestSpec {

        private final String userMessage;
        private String specSystemPrompt;
        private String specConversationId;
        private Map<String, Object> specOptions;

        Lc4jChatRequestSpec(String userMessage) {
            this.userMessage = userMessage;
        }

        @Override
        public AfgChatClient.ChatRequestSpec systemPrompt(String systemPrompt) {
            this.specSystemPrompt = systemPrompt;
            return this;
        }

        @Override
        public AfgChatClient.ChatRequestSpec conversationId(String conversationId) {
            this.specConversationId = conversationId;
            return this;
        }

        @Override
        public AfgChatClient.ChatRequestSpec options(Map<String, Object> options) {
            this.specOptions = options;
            return this;
        }

        @Override
        public AiChatResponse call() {
            List<ChatMessage> messages = new ArrayList<>();
            if (specSystemPrompt != null) {
                messages.add(SystemMessage.from(specSystemPrompt));
            }
            messages.add(UserMessage.from(userMessage));
            ChatResponse response = chatModel.chat(messages);
            return convertResponse(response);
        }

        @Override
        public Flux<String> stream() {
            if (streamingChatModel == null) {
                throw new UnsupportedOperationException(
                    "Streaming is not available: no StreamingChatModel configured");
            }
            List<ChatMessage> messages = new ArrayList<>();
            if (specSystemPrompt != null) {
                messages.add(SystemMessage.from(specSystemPrompt));
            }
            messages.add(UserMessage.from(userMessage));
            Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();
            streamingChatModel.chat(messages, new StreamingChatResponseHandler() {
                @Override
                public void onPartialResponse(String partialResponse) {
                    sink.tryEmitNext(partialResponse);
                }

                @Override
                public void onCompleteResponse(ChatResponse response) {
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
        public <T> T entity(Class<T> type) {
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
