package io.github.afgprojects.framework.ai.agent.executor;

import io.github.afgprojects.framework.ai.core.chat.AfgChatClient;
import io.github.afgprojects.framework.ai.core.chat.AiChatResponse;
import io.github.afgprojects.framework.ai.core.chat.AiMessage;
import io.github.afgprojects.framework.ai.core.exception.AiException;
import io.github.afgprojects.framework.ai.core.exception.ToolException;
import io.github.afgprojects.framework.ai.core.tool.Tool;
import io.github.afgprojects.framework.ai.core.tool.ToolRegistry;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 工具执行器
 *
 * <p>负责执行工具调用并将结果返回给 LLM。
 * 使用 AfgChatClient 进行对话，Spring AI Advisor 链处理原生工具调用。
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class ToolExecutor {

    private static final Logger log = LoggerFactory.getLogger(ToolExecutor.class);

    private final ToolRegistry toolRegistry;
    private final AfgChatClient chatClient;
    private final int maxIterations;
    private final long timeoutMs;

    /**
     * 创建工具执行器
     *
     * @param toolRegistry  工具注册表
     * @param chatClient    对话客户端
     * @param maxIterations 最大迭代次数
     * @param timeoutMs     执行超时（毫秒）
     */
    public ToolExecutor(
            @NonNull ToolRegistry toolRegistry,
            @NonNull AfgChatClient chatClient,
            int maxIterations,
            long timeoutMs
    ) {
        this.toolRegistry = toolRegistry;
        this.chatClient = chatClient;
        this.maxIterations = maxIterations;
        this.timeoutMs = timeoutMs;
    }

    /**
     * 执行带工具调用的对话
     *
     * <p>循环执行：
     * <ol>
     *   <li>调用 LLM</li>
     *   <li>如果 LLM 返回最终答案（无工具调用），返回结果</li>
     *   <li>否则继续迭代直到达到最大迭代次数</li>
     * </ol>
     *
     * <p>Spring AI Advisor 链自动处理工具调用，
     * 本方法负责编排多轮对话流程。
     *
     * @param systemPrompt 系统提示词
     * @param messages     对话消息列表
     * @return 最终响应
     */
    public @NonNull AiChatResponse executeWithTools(
            @Nullable String systemPrompt,
            @NonNull List<AiMessage> messages
    ) {
        AfgChatClient client = systemPrompt != null
                ? chatClient.withSystemPrompt(systemPrompt)
                : chatClient;

        List<AiMessage> currentMessages = new ArrayList<>(messages);
        AiChatResponse lastResponse = null;

        for (int iteration = 1; iteration <= maxIterations; iteration++) {
            log.debug("Tool execution iteration {}", iteration);

            // 调用 LLM（Spring AI Advisor 链自动处理工具调用）
            AiChatResponse response = client.chat(currentMessages);
            String content = response.content() != null ? response.content() : "";

            if (content.isBlank()) {
                log.debug("Empty response at iteration {}, continuing", iteration);
                continue;
            }

            // Advisor 链已处理工具调用，响应为最终结果
            log.debug("LLM returned response after {} iterations", iteration);
            lastResponse = response;
            break;
        }

        if (lastResponse != null) {
            return lastResponse;
        }

        log.warn("Max iterations ({}) reached, making final call", maxIterations);
        return client.chat(currentMessages);
    }

    /**
     * 执行带工具调用的对话（简化版）
     *
     * @param task 用户任务
     * @return 最终响应
     */
    public @NonNull AiChatResponse executeWithTools(@NonNull String task) {
        List<AiMessage> messages = new ArrayList<>();
        messages.add(AiMessage.user(task));
        return executeWithTools(null, messages);
    }

    /**
     * 异步执行带工具调用的对话
     */
    public @NonNull CompletableFuture<AiChatResponse> executeWithToolsAsync(
            @Nullable String systemPrompt,
            @NonNull List<AiMessage> messages
    ) {
        return CompletableFuture.supplyAsync(() -> executeWithTools(systemPrompt, messages));
    }

    /**
     * 异步执行带工具调用的对话（简化版）
     */
    public @NonNull CompletableFuture<AiChatResponse> executeWithToolsAsync(@NonNull String task) {
        return CompletableFuture.supplyAsync(() -> executeWithTools(task));
    }

    /**
     * 执行单个工具（带超时）
     *
     * @param toolName  工具名称
     * @param arguments 工具参数
     * @return 工具执行结果
     */
    public @Nullable Object executeTool(@NonNull String toolName, @NonNull Map<String, Object> arguments) {
        var toolOpt = toolRegistry.getTool(toolName);
        if (toolOpt.isEmpty()) {
            throw new ToolException("Tool not found: " + toolName,
                    AiException.ErrorCodes.TOOL_NOT_FOUND, toolName);
        }

        Tool<?, ?> tool = toolOpt.get();

        try {
            return CompletableFuture.supplyAsync(() -> {
                @SuppressWarnings("unchecked")
                Tool<Map<String, Object>, Object> typedTool = (Tool<Map<String, Object>, Object>) tool;
                return typedTool.execute(arguments);
            }).get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            throw new ToolException("Tool execution timeout: " + toolName,
                    AiException.ErrorCodes.TOOL_EXECUTION_FAILED, toolName, e);
        } catch (Exception e) {
            throw new ToolException("Tool execution failed: " + toolName,
                    AiException.ErrorCodes.TOOL_EXECUTION_FAILED, toolName, e);
        }
    }
}