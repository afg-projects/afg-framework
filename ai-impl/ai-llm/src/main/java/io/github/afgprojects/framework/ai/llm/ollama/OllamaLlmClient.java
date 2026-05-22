package io.github.afgprojects.framework.ai.llm.ollama;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.afgprojects.framework.ai.core.memory.Message;
import io.github.afgprojects.framework.ai.core.model.LlmClient;
import io.github.afgprojects.framework.ai.core.model.LlmConfig;
import io.github.afgprojects.framework.ai.core.model.LlmRequest;
import io.github.afgprojects.framework.ai.core.model.LlmResponse;
import io.github.afgprojects.framework.ai.core.model.TokenUsage;
import io.github.afgprojects.framework.ai.core.tool.ToolCall;
import io.github.afgprojects.framework.ai.core.tool.ToolDefinition;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaModel;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Ollama LLM 客户端实现
 *
 * <p>基于 Spring AI Ollama 实现的本地 LLM 客户端，支持本地部署的模型。
 * 适用于开发测试和私有部署场景。
 *
 * <p>支持的模型包括：
 * <ul>
 *   <li>qwen2.5:1.5b - 轻量级中文模型</li>
 *   <li>llama3 - Meta Llama 3</li>
 *   <li>mistral - Mistral AI</li>
 *   <li>其他 Ollama 支持的模型</li>
 * </ul>
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class OllamaLlmClient implements LlmClient {

    private static final String PROVIDER_NAME = "ollama";
    private static final String DEFAULT_BASE_URL = "http://localhost:11434";

    private final OllamaChatModel chatModel;
    private final LlmConfig config;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 创建 Ollama LLM 客户端
     *
     * @param config 配置（baseUrl 默认为 http://localhost:11434）
     */
    public OllamaLlmClient(@NonNull LlmConfig config) {
        this.config = config;

        // 创建 Ollama API
        String baseUrl = config.baseUrl() != null ? config.baseUrl() : DEFAULT_BASE_URL;
        OllamaApi ollamaApi = OllamaApi.builder()
                .baseUrl(baseUrl)
                .build();

        // 创建 OllamaChatOptions
        OllamaChatOptions ollamaOptions = OllamaChatOptions.builder()
                .model(config.model())
                .build();

        // 创建 ChatModel
        this.chatModel = OllamaChatModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(ollamaOptions)
                .build();
    }

    /**
     * 使用模型名称创建客户端（默认本地 Ollama）
     *
     * @param model 模型名称（如 qwen2.5:1.5b）
     */
    public OllamaLlmClient(@NonNull String model) {
        this(LlmConfig.of(model).withBaseUrl(DEFAULT_BASE_URL));
    }

    /**
     * 使用 baseUrl 和模型创建客户端
     *
     * @param baseUrl Ollama 服务地址
     * @param model   模型名称
     */
    public OllamaLlmClient(@NonNull String baseUrl, @NonNull String model) {
        this(LlmConfig.of(model).withBaseUrl(baseUrl));
    }

    @Override
    public @NonNull LlmResponse chat(@NonNull LlmRequest request) {
        Prompt prompt = buildPrompt(request);
        ChatResponse response = chatModel.call(prompt);
        return convertResponse(response);
    }

    @Override
    public @NonNull Flux<LlmResponse> chatStream(@NonNull LlmRequest request) {
        Prompt prompt = buildPrompt(request);
        return chatModel.stream(prompt)
                .map(this::convertResponse);
    }

    @Override
    public @NonNull LlmResponse chatWithTools(@NonNull LlmRequest request, @NonNull List<ToolDefinition> tools) {
        // Ollama 暂不支持原生工具调用，直接调用 chat
        return chat(request);
    }

    @Override
    public @NonNull LlmConfig getConfig() {
        return config;
    }

    /**
     * 构建 Spring AI Prompt
     */
    private @NonNull Prompt buildPrompt(@NonNull LlmRequest request) {
        List<org.springframework.ai.chat.messages.Message> messages = new ArrayList<>();

        // 添加系统消息
        if (request.systemPrompt() != null) {
            messages.add(new SystemMessage(request.systemPrompt()));
        }

        // 添加对话历史
        if (request.messages() != null) {
            for (var msg : request.messages()) {
                switch (msg.role()) {
                    case SYSTEM -> messages.add(new SystemMessage(msg.content()));
                    case USER -> messages.add(new UserMessage(msg.content()));
                    case ASSISTANT -> messages.add(new AssistantMessage(msg.content()));
                    case TOOL -> {
                        // Tool 消息处理需要 Spring AI 特定 API
                    }
                }
            }
        }

        return new Prompt(messages);
    }

    /**
     * 转换 Spring AI 响应
     */
    private @NonNull LlmResponse convertResponse(@NonNull ChatResponse response) {
        var result = response.getResult();
        var content = result != null && result.getOutput() != null
                ? result.getOutput().getText()
                : "";

        // Token 使用量
        var metadata = response.getMetadata();
        TokenUsage tokenUsage = null;
        if (metadata != null && metadata.getUsage() != null) {
            var usage = metadata.getUsage();
            tokenUsage = new TokenUsage(
                    usage.getPromptTokens(),
                    usage.getCompletionTokens(),
                    usage.getTotalTokens()
            );
        }

        // Tool Calls（Ollama 暂不支持）
        List<ToolCall> toolCalls = List.of();

        // 确定完成原因
        LlmResponse.FinishReason finishReason = null;
        if (result != null && result.getMetadata() != null) {
            finishReason = convertFinishReason(result.getMetadata().getFinishReason());
        }

        return new LlmResponse(content, toolCalls, List.of(), tokenUsage, finishReason);
    }

    /**
     * 转换完成原因
     */
    private LlmResponse.@Nullable FinishReason convertFinishReason(@Nullable String reason) {
        if (reason == null) {
            return null;
        }
        return switch (reason.toLowerCase()) {
            case "stop" -> LlmResponse.FinishReason.STOP;
            case "length" -> LlmResponse.FinishReason.LENGTH;
            default -> LlmResponse.FinishReason.UNKNOWN;
        };
    }
}