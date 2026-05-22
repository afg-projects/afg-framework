package io.github.afgprojects.framework.ai.llm.openai;

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
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * OpenAI LLM 客户端实现
 *
 * <p>基于 Spring AI OpenAI 实现的 LLM 客户端，支持同步和流式调用。
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class OpenAiLlmClient implements LlmClient {

    private static final String PROVIDER_NAME = "openai";
    private static final int MAX_TOOL_ITERATIONS = 10;

    private final OpenAiChatModel chatModel;
    private final LlmConfig config;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 创建 OpenAI LLM 客户端
     *
     * @param config 配置
     */
    public OpenAiLlmClient(@NonNull LlmConfig config) {
        this.config = config;

        // 创建 OpenAI API
        OpenAiApi.Builder apiBuilder = OpenAiApi.builder()
                .apiKey(config.apiKey());

        if (config.baseUrl() != null) {
            apiBuilder.baseUrl(config.baseUrl());
        }

        OpenAiApi openAiApi = apiBuilder.build();

        // 创建 ChatOptions
        OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder()
                .model(config.model());

        // 从 options 中获取额外参数（如果 config 是通过 builder 创建的）
        // 这里使用默认值，实际参数从 request 中获取

        // 创建 ChatModel
        this.chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(optionsBuilder.build())
                .build();
    }

    /**
     * 使用 API Key 和模型创建客户端
     *
     * @param apiKey API Key
     * @param model  模型名称
     */
    public OpenAiLlmClient(@NonNull String apiKey, @NonNull String model) {
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
        // 简化实现：直接调用 chat，工具定义在 request 中
        // 完整实现需要处理工具调用循环
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
        OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder();

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
                    // 解析 arguments JSON 字符串为 Map
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

        return new LlmResponse(content, toolCalls, List.of(), tokenUsage, finishReason);
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
            // 如果解析失败，返回空 Map
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
            case "stop" -> LlmResponse.FinishReason.STOP;
            case "length" -> LlmResponse.FinishReason.LENGTH;
            case "tool_calls", "tool_call" -> LlmResponse.FinishReason.TOOL_CALL;
            case "content_filter" -> LlmResponse.FinishReason.CONTENT_FILTER;
            default -> LlmResponse.FinishReason.UNKNOWN;
        };
    }
}
