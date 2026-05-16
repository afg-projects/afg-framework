package io.github.afgprojects.framework.ai.agent.executor;

import io.github.afgprojects.framework.ai.core.exception.ToolException;
import io.github.afgprojects.framework.ai.core.model.LlmClient;
import io.github.afgprojects.framework.ai.core.model.LlmRequest;
import io.github.afgprojects.framework.ai.core.model.LlmResponse;
import io.github.afgprojects.framework.ai.core.tool.Tool;
import io.github.afgprojects.framework.ai.core.tool.ToolCall;
import io.github.afgprojects.framework.ai.core.tool.ToolRegistry;
import io.github.afgprojects.framework.ai.core.tool.ToolResult;
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
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class ToolExecutor {

    private static final Logger log = LoggerFactory.getLogger(ToolExecutor.class);

    private final ToolRegistry toolRegistry;
    private final LlmClient llmClient;
    private final int maxIterations;
    private final long timeoutMs;

    /**
     * 创建工具执行器
     *
     * @param toolRegistry  工具注册表
     * @param llmClient     LLM 客户端
     * @param maxIterations 最大迭代次数
     * @param timeoutMs     执行超时（毫秒）
     */
    public ToolExecutor(
            @NonNull ToolRegistry toolRegistry,
            @NonNull LlmClient llmClient,
            int maxIterations,
            long timeoutMs
    ) {
        this.toolRegistry = toolRegistry;
        this.llmClient = llmClient;
        this.maxIterations = maxIterations;
        this.timeoutMs = timeoutMs;
    }

    /**
     * 执行带工具调用的请求
     *
     * <p>循环执行：
     * <ol>
     *   <li>调用 LLM</li>
     *   <li>如果有工具调用，执行工具</li>
     *   <li>将工具结果反馈给 LLM</li>
     *   <li>重复直到 LLM 返回最终答案或达到最大迭代次数</li>
     * </ol>
     *
     * @param request 初始请求
     * @return 最终响应
     */
    public @NonNull LlmResponse executeWithTools(@NonNull LlmRequest request) {
        LlmRequest currentRequest = request;
        int iteration = 0;

        while (iteration < maxIterations) {
            iteration++;
            log.debug("Tool execution iteration {}", iteration);

            // 调用 LLM
            LlmResponse response = llmClient.chat(currentRequest);

            // 如果没有工具调用，返回结果
            if (!response.hasToolCalls()) {
                log.debug("LLM returned final response after {} iterations", iteration);
                return response;
            }

            // 执行工具调用
            List<ToolResult> toolResults = executeTools(response.toolCalls());

            // 构建包含工具结果的请求
            currentRequest = buildRequestWithToolResults(currentRequest, response, toolResults);
        }

        log.warn("Max iterations ({}) reached, returning last response", maxIterations);
        return llmClient.chat(currentRequest);
    }

    /**
     * 异步执行带工具调用的请求
     */
    public @NonNull CompletableFuture<LlmResponse> executeWithToolsAsync(@NonNull LlmRequest request) {
        return CompletableFuture.supplyAsync(() -> executeWithTools(request));
    }

    /**
     * 执行工具调用列表
     */
    private @NonNull List<ToolResult> executeTools(@NonNull List<ToolCall> toolCalls) {
        List<ToolResult> results = new ArrayList<>();

        for (ToolCall call : toolCalls) {
            ToolResult result = executeSingleTool(call);
            results.add(result);
        }

        return results;
    }

    /**
     * 执行单个工具调用
     */
    private @NonNull ToolResult executeSingleTool(@NonNull ToolCall call) {
        log.debug("Executing tool: {} with id: {}", call.name(), call.id());

        // 查找工具
        var toolOpt = toolRegistry.getTool(call.name());
        if (toolOpt.isEmpty()) {
            log.warn("Tool not found: {}", call.name());
            return new ToolResult(call.id(), call.name(), null, "Tool not found: " + call.name());
        }

        Tool<?, ?> tool = toolOpt.get();

        try {
            // 执行工具（带超时）
            Object output = executeWithTimeout(tool, call.arguments());

            log.debug("Tool {} executed successfully", call.name());
            return new ToolResult(call.id(), call.name(), output != null ? output.toString() : "", null);

        } catch (Exception e) {
            log.error("Tool {} execution failed: {}", call.name(), e.getMessage());
            return new ToolResult(call.id(), call.name(), null, e.getMessage());
        }
    }

    /**
     * 带超时执行工具
     */
    private @Nullable Object executeWithTimeout(@NonNull Tool<?, ?> tool, @NonNull Map<String, Object> arguments) {
        try {
            return CompletableFuture.supplyAsync(() -> {
                // 由于 Tool 是泛型，我们需要使用原始类型执行
                // 实际实现中应该有类型安全的转换
                @SuppressWarnings("unchecked")
                Tool<Map<String, Object>, Object> typedTool = (Tool<Map<String, Object>, Object>) tool;
                return typedTool.execute(arguments);
            }).get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            throw new ToolException("Tool execution timeout: " + tool.name(), ToolException.ErrorCodes.TOOL_EXECUTION_FAILED, tool.name(), e);
        } catch (Exception e) {
            throw new ToolException("Tool execution failed: " + tool.name(), ToolException.ErrorCodes.TOOL_EXECUTION_FAILED, tool.name(), e);
        }
    }

    /**
     * 构建包含工具结果的请求
     */
    private @NonNull LlmRequest buildRequestWithToolResults(
            @NonNull LlmRequest previousRequest,
            @NonNull LlmResponse response,
            @NonNull List<ToolResult> toolResults
    ) {
        // 构建消息列表
        List<io.github.afgprojects.framework.ai.core.memory.Message> messages = new ArrayList<>(previousRequest.messages());

        // 添加助手响应（包含工具调用）
        messages.add(io.github.afgprojects.framework.ai.core.memory.Message.assistantWithTools(
                response.content(),
                new ArrayList<>(response.toolCalls())
        ));

        // 添加工具结果
        messages.add(io.github.afgprojects.framework.ai.core.memory.Message.tool(
                null,
                new ArrayList<>(toolResults)
        ));

        return new LlmRequest(
                previousRequest.systemPrompt(),
                messages,
                previousRequest.tools(),
                previousRequest.options()
        );
    }
}