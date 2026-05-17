package io.github.afgprojects.framework.ai.llm.anthropic;

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
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Anthropic Claude LLM 客户端实现
 *
 * <p>基于 Spring AI Anthropic 实现的 LLM 客户端，支持同步和流式调用。
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class AnthropicLlmClient implements LlmClient {

    private static final String PROVIDER_NAME = "anthropic";

    private final AnthropicChatModel chatModel;
    private final LlmConfig config;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 创建 Anthropic LLM 客户端
     *
     * @param config 配置
     */
    public AnthropicLlmClient(@NonNull LlmConfig config) {
        this.config = config;

        // 创建 Anthropic API
        AnthropicApi anthropicApi = AnthropicApi.builder()
                .apiKey(config.apiKey())
                .build();

        // 创建 ChatOptions
        AnthropicChatOptions.Builder optionsBuilder = AnthropicChatOptions.builder()
                .model(config.model());

        // 创建 ChatModel
        this.chatModel = AnthropicChatModel.builder()
                .anthropicApi(anthropicApi)
                .defaultOptions(optionsBuilder.build())
                .build();
    }

    /**
     * 使用 API Key 和模型创建客户端
     *
     * @param apiKey API Key
     * @param model  模型名称
     */
    public AnthropicLlmClient(@NonNull String apiKey, @NonNull String model) {
        this(LlmConfig.of(apiKey, model));
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
        // 简化实现：直接调用 chat
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
                        // 暂时跳过，后续版本完善
                    }
                }
            }
        }

        // 构建选项
        AnthropicChatOptions.Builder optionsBuilder = AnthropicChatOptions.builder();

        // 从 options Map 中获取参数
        Double temperature = request.getOption("temperature", (Double) null);
        if (temperature != null) {
            optionsBuilder.temperature(temperature);
        }

        Integer maxTokens = request.getOption("maxTokens", (Integer) null);
        if (maxTokens != null) {
            optionsBuilder.maxTokens(maxTokens);
        }

        return new Prompt(messages, optionsBuilder.build());
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

        // Tool Calls
        List<ToolCall> toolCalls = new ArrayList<>();
        if (result != null && result.getOutput() != null) {
            var assistantMessage = result.getOutput();
            var toolCallList = assistantMessage.getToolCalls();
            if (toolCallList != null && !toolCallList.isEmpty()) {
                for (var tc : toolCallList) {
                    Map<String, Object> args = parseArguments(tc.arguments());
                    toolCalls.add(new ToolCall(tc.id(), tc.name(), args));
                }
            }
        }

        // 确定完成原因
        LlmResponse.FinishReason finishReason = null;
        if (result != null && result.getMetadata() != null) {
            finishReason = convertFinishReason(result.getMetadata().getFinishReason());
        }

        return new LlmResponse(content, toolCalls, tokenUsage, finishReason);
    }

    /**
     * 解析 arguments JSON 字符串
     */
    private @NonNull Map<String, Object> parseArguments(@Nullable String arguments) {
        if (arguments == null || arguments.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(arguments, new TypeReference<>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    /**
     * 转换完成原因
     */
    private LlmResponse.@Nullable FinishReason convertFinishReason(@Nullable String reason) {
        if (reason == null) {
            return null;
        }
        return switch (reason.toLowerCase()) {
            case "end_turn", "stop" -> LlmResponse.FinishReason.STOP;
            case "max_tokens", "length" -> LlmResponse.FinishReason.LENGTH;
            case "tool_use", "tool_calls" -> LlmResponse.FinishReason.TOOL_CALL;
            case "content_filter" -> LlmResponse.FinishReason.CONTENT_FILTER;
            default -> LlmResponse.FinishReason.UNKNOWN;
        };
    }
}
