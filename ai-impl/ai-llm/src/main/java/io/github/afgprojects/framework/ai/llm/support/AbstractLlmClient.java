package io.github.afgprojects.framework.ai.llm.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.afgprojects.framework.ai.core.media.AudioContent;
import io.github.afgprojects.framework.ai.core.media.ImageContent;
import io.github.afgprojects.framework.ai.core.media.MediaContent;
import io.github.afgprojects.framework.ai.core.model.LlmConfig;
import io.github.afgprojects.framework.ai.core.model.LlmRequest;
import io.github.afgprojects.framework.ai.core.model.LlmResponse;
import io.github.afgprojects.framework.ai.core.model.TokenUsage;
import io.github.afgprojects.framework.ai.core.tool.Tool;
import io.github.afgprojects.framework.ai.core.tool.ToolCall;
import io.github.afgprojects.framework.ai.core.tool.ToolDefinition;
import io.github.afgprojects.framework.ai.core.tool.ToolRegistry;
import io.github.afgprojects.framework.ai.core.tool.ToolResult;
import io.github.afgprojects.framework.ai.llm.advisor.DefaultAdvisorContext;
import io.github.afgprojects.framework.ai.llm.advisor.LlmAdvisor;
import io.github.afgprojects.framework.ai.llm.ollama.AfgToolCallback;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.MimeTypeUtils;

import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LLM 客户端抽象基类
 *
 * <p>提供消息转换、响应解析、工具调用处理等公共逻辑，
 * 各提供商实现类只需关注 ChatModel 的创建和配置。
 *
 * @author afg-projects
 * @since 1.0.0
 */
public abstract class AbstractLlmClient {

    protected static final int MAX_TOOL_ITERATIONS = 10;

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 获取配置
     */
    @NonNull
    public abstract LlmConfig getConfig();

    /**
     * 获取工具注册表
     */
    @Nullable
    public abstract ToolRegistry getToolRegistry();

    /**
     * 获取 Advisor 列表
     */
    @NonNull
    public abstract List<LlmAdvisor> getAdvisors();

    // ==================== 消息转换 ====================

    /**
     * 构建 Spring AI 消息列表
     */
    @NonNull
    protected List<org.springframework.ai.chat.messages.Message> buildMessages(@NonNull LlmRequest request) {
        List<org.springframework.ai.chat.messages.Message> messages = new ArrayList<>();

        // 添加系统消息
        if (request.systemPrompt() != null) {
            messages.add(new SystemMessage(request.systemPrompt()));
        }

        // 添加对话历史
        if (request.messages() != null) {
            for (var msg : request.messages()) {
                switch (msg.role()) {
                    case SYSTEM -> {
                        // 如果已有显式 systemPrompt，跳过 messages 中的 SYSTEM 消息避免重复
                        if (request.systemPrompt() == null) {
                            messages.add(new SystemMessage(msg.content()));
                        }
                    }
                    case USER -> {
                        // 处理多模态消息
                        if (!msg.media().isEmpty()) {
                            List<Media> mediaList = convertToSpringAiMedia(msg.media());
                            messages.add(UserMessage.builder()
                                    .text(msg.content())
                                    .media(mediaList)
                                    .build());
                        } else {
                            messages.add(new UserMessage(msg.content()));
                        }
                    }
                    case ASSISTANT -> {
                        if (!msg.toolCalls().isEmpty()) {
                            List<AssistantMessage.ToolCall> toolCalls = convertToSpringAiToolCalls(msg.toolCalls());
                            messages.add(AssistantMessage.builder()
                                    .content(msg.content())
                                    .toolCalls(toolCalls)
                                    .build());
                        } else {
                            messages.add(new AssistantMessage(msg.content()));
                        }
                    }
                    case TOOL -> {
                        if (!msg.toolResults().isEmpty()) {
                            List<ToolResponseMessage.ToolResponse> responses =
                                    convertToSpringAiToolResponses(msg.toolResults());
                            messages.add(ToolResponseMessage.builder()
                                    .responses(responses)
                                    .build());
                        }
                    }
                }
            }
        }

        // 应用 Advisors
        List<LlmAdvisor> advisors = getAdvisors();
        if (advisors != null && !advisors.isEmpty()) {
            messages = applyAdvisors(messages, request, advisors);
        }

        log.debug("Built {} Spring AI messages for LLM call (systemPrompt={}, requestMessages={})",
                messages.size(),
                request.systemPrompt() != null ? "yes" : "no",
                request.messages() != null ? request.messages().size() : 0);

        return messages;
    }

    /**
     * 将框架 MediaContent 转换为 Spring AI Media
     */
    @NonNull
    protected List<Media> convertToSpringAiMedia(@NonNull List<MediaContent> mediaContentList) {
        List<Media> result = new ArrayList<>();
        for (MediaContent mc : mediaContentList) {
            Media media = convertSingleMediaContent(mc);
            if (media != null) {
                result.add(media);
            }
        }
        return result;
    }

    /**
     * 转换单个 MediaContent 为 Spring AI Media
     */
    @Nullable
    protected Media convertSingleMediaContent(@NonNull MediaContent mc) {
        try {
            // 处理 URL 类型
            if (mc.isUrl()) {
                String url = (String) mc.data();
                // URL 类型使用 MimeTypeUtils 解析 MIME 类型
                return switch (mc) {
                    case ImageContent img -> new Media(MimeTypeUtils.IMAGE_JPEG, java.net.URI.create(url));
                    case AudioContent aud -> new Media(MimeTypeUtils.parseMimeType(aud.mimeType()), java.net.URI.create(url));
                };
            }

            // 处理 Base64 类型
            if (mc.isBase64()) {
                String base64 = (String) mc.data();
                byte[] bytes = Base64.getDecoder().decode(base64);
                ByteArrayResource resource = new ByteArrayResource(bytes);
                return switch (mc) {
                    case ImageContent img -> new Media(MimeTypeUtils.parseMimeType(img.mimeType()), resource);
                    case AudioContent aud -> new Media(MimeTypeUtils.parseMimeType(aud.mimeType()), resource);
                };
            }

            // 处理字节类型
            if (mc.isBytes()) {
                byte[] bytes = (byte[]) mc.data();
                ByteArrayResource resource = new ByteArrayResource(bytes);
                return switch (mc) {
                    case ImageContent img -> new Media(MimeTypeUtils.parseMimeType(img.mimeType()), resource);
                    case AudioContent aud -> new Media(MimeTypeUtils.parseMimeType(aud.mimeType()), resource);
                };
            }

            log.warn("Unknown media content type: {}", mc.getClass().getSimpleName());
            return null;
        } catch (Exception e) {
            log.warn("Failed to convert media content: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 应用 Advisors 到消息列表
     */
    @NonNull
    protected List<org.springframework.ai.chat.messages.Message> applyAdvisors(
            @NonNull List<org.springframework.ai.chat.messages.Message> messages,
            @NonNull LlmRequest request,
            @NonNull List<LlmAdvisor> advisors) {

        DefaultAdvisorContext context = new DefaultAdvisorContext(request);

        List<LlmAdvisor> sortedAdvisors = advisors.stream()
                .sorted((a, b) -> Integer.compare(a.getOrder(), b.getOrder()))
                .toList();

        List<org.springframework.ai.chat.messages.Message> result = messages;
        for (LlmAdvisor advisor : sortedAdvisors) {
            log.debug("Applying advisor: {} (order={})", advisor.getName(), advisor.getOrder());
            result = advisor.apply(result, context);
        }

        return result;
    }

    // ==================== 工具转换 ====================

    /**
     * 将工具定义列表转换为 Spring AI ToolCallback Map
     */
    @NonNull
    protected Map<String, ToolCallback> convertToToolCallbacks(@NonNull List<ToolDefinition> tools) {
        ToolRegistry registry = getToolRegistry();
        if (registry == null) {
            log.warn("ToolRegistry is null, tool calls will not be executed");
            return Map.of();
        }

        Map<String, ToolCallback> callbacks = new HashMap<>();
        for (ToolDefinition toolDef : tools) {
            AfgToolCallback callback = new AfgToolCallback(toolDef, registry, objectMapper);
            callbacks.put(toolDef.name(), callback);
        }
        return callbacks;
    }

    /**
     * 转换工具调用为 Spring AI 格式
     */
    @NonNull
    protected List<AssistantMessage.ToolCall> convertToSpringAiToolCalls(@NonNull List<ToolCall> toolCalls) {
        List<AssistantMessage.ToolCall> result = new ArrayList<>();
        for (ToolCall toolCall : toolCalls) {
            try {
                result.add(new AssistantMessage.ToolCall(
                        toolCall.id(),
                        "function",
                        toolCall.name(),
                        objectMapper.writeValueAsString(toolCall.arguments())
                ));
            } catch (Exception e) {
                log.warn("Failed to serialize tool call arguments: {}", e.getMessage());
                result.add(new AssistantMessage.ToolCall(
                        toolCall.id(),
                        "function",
                        toolCall.name(),
                        "{}"
                ));
            }
        }
        return result;
    }

    /**
     * 转换工具结果为 Spring AI 格式
     */
    @NonNull
    protected List<ToolResponseMessage.ToolResponse> convertToSpringAiToolResponses(
            @NonNull List<ToolResult> toolResults) {
        List<ToolResponseMessage.ToolResponse> result = new ArrayList<>();
        for (ToolResult toolResult : toolResults) {
            result.add(new ToolResponseMessage.ToolResponse(
                    toolResult.toolCallId(),
                    toolResult.toolName(),
                    toolResult.output() != null ? toolResult.output() : ""
            ));
        }
        return result;
    }

    // ==================== 工具调用循环 ====================

    /**
     * 执行工具调用循环
     *
     * <p>循环逻辑：
     * <ol>
     *   <li>发送请求（带工具定义）</li>
     *   <li>检测响应中的工具调用</li>
     *   <li>执行工具，收集结果</li>
     *   <li>将工具结果追加到消息中，再次请求</li>
     *   <li>重复直到没有工具调用或达到最大迭代次数</li>
     * </ol>
     *
     * @param initialMessages 初始消息列表（Spring AI 格式）
     * @param toolCallbacks   工具回调 Map
     * @param callFunction    调用 ChatModel 的函数（接收 Prompt，返回 ChatResponse）
     * @return 最终的 LlmResponse（包含所有工具执行结果）
     */
    @NonNull
    protected LlmResponse executeToolCallLoop(
            @NonNull List<org.springframework.ai.chat.messages.Message> initialMessages,
            @NonNull Map<String, ToolCallback> toolCallbacks,
            java.util.function.Function<Prompt, ChatResponse> callFunction) {

        List<org.springframework.ai.chat.messages.Message> messages = new ArrayList<>(initialMessages);
        List<ToolResult> allToolResults = new ArrayList<>();
        TokenUsage totalTokenUsage = null;
        int promptTokens = 0;
        int completionTokens = 0;

        for (int iteration = 0; iteration < MAX_TOOL_ITERATIONS; iteration++) {
            log.debug("Tool call loop iteration {}", iteration + 1);

            Prompt prompt = new Prompt(messages);
            ChatResponse response = callFunction.apply(prompt);

            // 提取工具调用
            List<ToolCall> toolCalls = extractToolCalls(response);
            if (toolCalls.isEmpty()) {
                // 没有工具调用，返回最终响应
                LlmResponse.FinishReason finishReason = extractFinishReason(response);
                String content = response.getResult() != null && response.getResult().getOutput() != null
                        ? response.getResult().getOutput().getText()
                        : "";

                // 合计 token 使用量
                TokenUsage finalTokenUsage = totalTokenUsage != null ? totalTokenUsage : extractTokenUsage(response);

                return new LlmResponse(content, List.of(), allToolResults, finalTokenUsage, finishReason);
            }

            log.debug("LLM requested {} tool calls", toolCalls.size());

            // 累加 token 使用量
            TokenUsage iterationUsage = extractTokenUsage(response);
            if (iterationUsage != null) {
                promptTokens += iterationUsage.promptTokens();
                completionTokens += iterationUsage.completionTokens();
                totalTokenUsage = new TokenUsage(promptTokens, completionTokens, promptTokens + completionTokens);
            }

            // 执行工具调用
            String assistantContent = response.getResult() != null && response.getResult().getOutput() != null
                    ? response.getResult().getOutput().getText()
                    : "";

            // 添加 AssistantMessage（带工具调用）到消息列表
            List<AssistantMessage.ToolCall> springAiToolCalls = convertToSpringAiToolCalls(toolCalls);
            messages.add(AssistantMessage.builder()
                    .content(assistantContent)
                    .toolCalls(springAiToolCalls)
                    .build());

            // 执行每个工具调用并收集结果
            List<ToolResponseMessage.ToolResponse> toolResponses = new ArrayList<>();
            for (ToolCall toolCall : toolCalls) {
                ToolResult toolResult = executeToolCall(toolCall, toolCallbacks);
                allToolResults.add(toolResult);

                toolResponses.add(new ToolResponseMessage.ToolResponse(
                        toolCall.id(),
                        toolCall.name(),
                        toolResult.output() != null ? toolResult.output() : ""
                ));
            }

            // 添加 ToolResponseMessage 到消息列表
            messages.add(ToolResponseMessage.builder()
                    .responses(toolResponses)
                    .build());

            log.debug("Executed {} tool calls, continuing loop", toolCalls.size());
        }

        // 达到最大迭代次数，返回带警告的响应
        log.warn("Tool call loop reached maximum iterations ({})", MAX_TOOL_ITERATIONS);
        return new LlmResponse("", List.of(), allToolResults, totalTokenUsage, LlmResponse.FinishReason.LENGTH);
    }

    /**
     * 执行单个工具调用
     */
    @NonNull
    protected ToolResult executeToolCall(
            @NonNull ToolCall toolCall,
            @NonNull Map<String, ToolCallback> toolCallbacks) {

        ToolCallback callback = toolCallbacks.get(toolCall.name());
        if (callback == null) {
            log.warn("Tool '{}' not found in callbacks", toolCall.name());
            return ToolResult.failure(toolCall.id(), toolCall.name(),
                    "Tool '" + toolCall.name() + "' is not available");
        }

        try {
            String argumentsJson = objectMapper.writeValueAsString(toolCall.arguments());
            String result = callback.call(argumentsJson);
            return ToolResult.success(toolCall.id(), toolCall.name(), result);
        } catch (Exception e) {
            log.error("Tool '{}' execution failed", toolCall.name(), e);
            return ToolResult.failure(toolCall.id(), toolCall.name(),
                    e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    // ==================== 响应解析 ====================

    /**
     * 转换 Spring AI 响应为框架响应
     */
    @NonNull
    protected LlmResponse convertResponse(@NonNull ChatResponse response) {
        var result = response.getResult();
        var content = result != null && result.getOutput() != null
                ? result.getOutput().getText()
                : "";

        // Token 使用量
        TokenUsage tokenUsage = extractTokenUsage(response);

        // Tool Calls
        List<ToolCall> toolCalls = extractToolCalls(response);

        // 完成原因
        LlmResponse.FinishReason finishReason = extractFinishReason(response);

        // 如果有工具调用，设置完成原因为 TOOL_CALL
        if (!toolCalls.isEmpty() && finishReason != LlmResponse.FinishReason.TOOL_CALL) {
            finishReason = LlmResponse.FinishReason.TOOL_CALL;
        }

        return new LlmResponse(content, toolCalls, List.of(), tokenUsage, finishReason);
    }

    /**
     * 提取 Token 使用量
     */
    protected @Nullable TokenUsage extractTokenUsage(@NonNull ChatResponse response) {
        var metadata = response.getMetadata();
        if (metadata != null && metadata.getUsage() != null) {
            var usage = metadata.getUsage();
            return new TokenUsage(
                    usage.getPromptTokens(),
                    usage.getCompletionTokens(),
                    usage.getTotalTokens()
            );
        }
        return null;
    }

    /**
     * 提取工具调用
     */
    @NonNull
    protected List<ToolCall> extractToolCalls(@Nullable ChatResponse response) {
        if (response == null) {
            return List.of();
        }

        var result = response.getResult();
        if (result == null || result.getOutput() == null) {
            return List.of();
        }

        var assistantMessage = result.getOutput();
        var toolCallList = assistantMessage.getToolCalls();
        if (toolCallList == null || toolCallList.isEmpty()) {
            return List.of();
        }

        List<ToolCall> toolCalls = new ArrayList<>();
        for (var tc : toolCallList) {
            Map<String, Object> args = parseArguments(tc.arguments());
            toolCalls.add(new ToolCall(tc.id(), tc.name(), args));
        }
        return toolCalls;
    }

    /**
     * 提取完成原因
     */
    protected LlmResponse.@Nullable FinishReason extractFinishReason(@Nullable ChatResponse response) {
        if (response == null) {
            return null;
        }
        var result = response.getResult();
        if (result == null || result.getMetadata() == null) {
            return null;
        }
        return convertFinishReason(result.getMetadata().getFinishReason());
    }

    /**
     * 转换完成原因（子类可覆盖以处理提供商特定的值）
     */
    protected LlmResponse.@Nullable FinishReason convertFinishReason(@Nullable String reason) {
        if (reason == null) {
            return null;
        }
        return switch (reason.toLowerCase()) {
            case "stop", "end_turn" -> LlmResponse.FinishReason.STOP;
            case "length", "max_tokens" -> LlmResponse.FinishReason.LENGTH;
            case "tool_calls", "tool_call", "tool_use" -> LlmResponse.FinishReason.TOOL_CALL;
            case "content_filter" -> LlmResponse.FinishReason.CONTENT_FILTER;
            default -> LlmResponse.FinishReason.UNKNOWN;
        };
    }

    // ==================== 工具方法 ====================

    /**
     * 解析 arguments JSON 字符串
     */
    @NonNull
    protected Map<String, Object> parseArguments(@Nullable String arguments) {
        if (arguments == null || arguments.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(arguments, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Failed to parse arguments: {}", e.getMessage());
            return Map.of();
        }
    }

    /**
     * 从请求中获取选项值
     */
    @Nullable
    @SuppressWarnings("unchecked")
    protected <T> T getOption(@NonNull LlmRequest request, @NonNull String key, @Nullable T defaultValue) {
        Object value = request.options().get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return (T) value;
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }
}
