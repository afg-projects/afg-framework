package io.github.afgprojects.framework.ai.agent.executor;

import io.github.afgprojects.framework.ai.core.exception.ToolException;
import io.github.afgprojects.framework.ai.core.model.LlmClient;
import io.github.afgprojects.framework.ai.core.model.LlmRequest;
import io.github.afgprojects.framework.ai.core.model.LlmResponse;
import io.github.afgprojects.framework.ai.core.security.ContentSafetyChecker;
import io.github.afgprojects.framework.ai.core.tool.*;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 安全工具执行器。
 *
 * <p>在工具执行过程中集成安全能力：
 * <ul>
 *   <li>权限校验 - 执行前检查用户权限</li>
 *   <li>内容安全 - 检查输入/输出内容</li>
 *   <li>审计日志 - 记录工具调用过程</li>
 *   <li>PII 保护 - 脱敏敏感信息</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class SecureToolExecutor {

    private static final Logger log = LoggerFactory.getLogger(SecureToolExecutor.class);

    private final ToolRegistry toolRegistry;
    private final LlmClient llmClient;
    private final ToolContextProvider contextProvider;
    private final ToolPermissionChecker permissionChecker;
    private final ToolAuditLogger auditLogger;
    private final ContentSafetyChecker contentSafetyChecker;

    private final int maxIterations;
    private final long timeoutMs;
    private final boolean enableContentSafety;

    /**
     * 创建安全工具执行器。
     *
     * @param toolRegistry         工具注册表
     * @param llmClient            LLM 客户端
     * @param contextProvider      上下文提供者
     * @param permissionChecker    权限检查器（可选）
     * @param auditLogger          审计日志器（可选）
     * @param contentSafetyChecker 内容安全检查器（可选）
     * @param maxIterations        最大迭代次数
     * @param timeoutMs            执行超时（毫秒）
     */
    public SecureToolExecutor(
            @NonNull ToolRegistry toolRegistry,
            @NonNull LlmClient llmClient,
            @NonNull ToolContextProvider contextProvider,
            @Nullable ToolPermissionChecker permissionChecker,
            @Nullable ToolAuditLogger auditLogger,
            @Nullable ContentSafetyChecker contentSafetyChecker,
            int maxIterations,
            long timeoutMs) {
        this.toolRegistry = toolRegistry;
        this.llmClient = llmClient;
        this.contextProvider = contextProvider;
        this.permissionChecker = permissionChecker;
        this.auditLogger = auditLogger;
        this.contentSafetyChecker = contentSafetyChecker;
        this.maxIterations = maxIterations;
        this.timeoutMs = timeoutMs;
        this.enableContentSafety = contentSafetyChecker != null;
    }

    /**
     * 执行带工具调用的请求。
     *
     * @param request 初始请求
     * @return 最终响应
     */
    public @NonNull LlmResponse executeWithTools(@NonNull LlmRequest request) {
        return executeWithTools(request, contextProvider.provide());
    }

    /**
     * 执行带工具调用的请求（指定上下文）。
     *
     * @param request 初始请求
     * @param context 工具上下文
     * @return 最终响应
     */
    public @NonNull LlmResponse executeWithTools(
            @NonNull LlmRequest request,
            @NonNull ToolContext context) {
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

            // 执行工具调用（带安全检查）
            List<ToolResult> toolResults = executeToolsSecure(response.toolCalls(), context);

            // 构建包含工具结果的请求
            currentRequest = buildRequestWithToolResults(currentRequest, response, toolResults);
        }

        log.warn("Max iterations ({}) reached, returning last response", maxIterations);
        return llmClient.chat(currentRequest);
    }

    /**
     * 异步执行带工具调用的请求。
     */
    public @NonNull CompletableFuture<LlmResponse> executeWithToolsAsync(@NonNull LlmRequest request) {
        return CompletableFuture.supplyAsync(() -> executeWithTools(request));
    }

    /**
     * 执行工具调用列表（带安全检查）。
     */
    private @NonNull List<ToolResult> executeToolsSecure(
            @NonNull List<ToolCall> toolCalls,
            @NonNull ToolContext context) {
        List<ToolResult> results = new ArrayList<>();

        for (ToolCall call : toolCalls) {
            ToolResult result = executeSingleToolSecure(call, context);
            results.add(result);
        }

        return results;
    }

    /**
     * 执行单个工具调用（带安全检查）。
     */
    private @NonNull ToolResult executeSingleToolSecure(
            @NonNull ToolCall call,
            @NonNull ToolContext context) {
        log.debug("Executing tool: {} with id: {}", call.name(), call.id());
        Instant start = Instant.now();
        String recordId = null;

        // 1. 查找工具
        var toolOpt = toolRegistry.getTool(call.name());
        if (toolOpt.isEmpty()) {
            log.warn("Tool not found: {}", call.name());
            return ToolResult.failure(call.id(), call.name(), "Tool not found: " + call.name());
        }

        Tool<?, ?> tool = toolOpt.get();

        // 2. 权限检查
        if (tool instanceof SecureTool<?, ?> secureTool) {
            if (permissionChecker != null) {
                var result = permissionChecker.checkSecureTool(secureTool, context);
                if (result.isDenied()) {
                    log.warn("Tool {} permission denied: {}", call.name(), result.getDenyReason());
                    if (auditLogger != null && secureTool.isAuditable()) {
                        auditLogger.logPermissionDenied(call.id(), call.name(), result.getDenyReason(), context);
                    }
                    return ToolResult.failure(call.id(), call.name(),
                        "Permission denied: " + result.getDenyReason());
                }
            }
        }

        // 3. 内容安全检查（输入）
        if (enableContentSafety && contentSafetyChecker != null) {
            String inputContent = serializeArguments(call.arguments());
            var checkResult = contentSafetyChecker.checkInput(inputContent, buildSafetyContext(context));
            if (checkResult.isBlocked()) {
                log.warn("Tool {} input blocked by content safety: {}", call.name(), checkResult.getReason());
                return ToolResult.failure(call.id(), call.name(),
                    "Content blocked: " + checkResult.getReason());
            }
        }

        // 4. 记录审计开始
        if (auditLogger != null && isAuditable(tool)) {
            Map<String, Object> auditArgs = new HashMap<>(call.arguments());
            auditArgs.put("__tool__", tool);
            recordId = auditLogger.logStart(call.id(), call.name(), auditArgs, context);
        }

        try {
            // 5. 执行工具
            Object output = executeWithTimeout(tool, call.arguments(), context);

            // 6. 内容安全检查（输出）
            if (enableContentSafety && contentSafetyChecker != null && output != null) {
                var checkResult = contentSafetyChecker.checkOutput(output.toString(), buildSafetyContext(context));
                if (checkResult.isBlocked()) {
                    log.warn("Tool {} output blocked by content safety: {}", call.name(), checkResult.getReason());
                    if (recordId != null) {
                        auditLogger.logFailure(recordId, "Output blocked: " + checkResult.getReason(),
                            Duration.between(start, Instant.now()));
                    }
                    return ToolResult.failure(call.id(), call.name(),
                        "Output blocked: " + checkResult.getReason());
                }
            }

            // 7. 记录审计成功
            Duration duration = Duration.between(start, Instant.now());
            if (recordId != null) {
                auditLogger.logSuccess(recordId, output, duration);
            }

            log.debug("Tool {} executed successfully in {}ms", call.name(), duration.toMillis());
            return ToolResult.success(call.id(), call.name(), output != null ? output.toString() : "");

        } catch (java.util.concurrent.TimeoutException e) {
            Duration duration = Duration.between(start, Instant.now());
            log.error("Tool {} execution timeout after {}ms", call.name(), timeoutMs);
            if (recordId != null) {
                auditLogger.logTimeout(call.id(), call.name(), context, Duration.ofMillis(timeoutMs));
            }
            return ToolResult.failure(call.id(), call.name(), "Tool execution timeout after " + timeoutMs + "ms");

        } catch (Exception e) {
            Duration duration = Duration.between(start, Instant.now());
            log.error("Tool {} execution failed: {}", call.name(), e.getMessage());
            if (recordId != null) {
                auditLogger.logFailure(recordId, e.getMessage(), duration);
            }
            return ToolResult.failure(call.id(), call.name(), e.getMessage());
        }
    }

    /**
     * 带超时执行工具。
     */
    private @Nullable Object executeWithTimeout(
            @NonNull Tool<?, ?> tool,
            @NonNull Map<String, Object> arguments,
            @NonNull ToolContext context) throws Exception {

        return CompletableFuture.supplyAsync(() -> {
            if (tool instanceof SecureTool<?, ?> secureTool) {
                // 安全工具：带上下文执行
                @SuppressWarnings("unchecked")
                SecureTool<Map<String, Object>, Object> typedTool =
                    (SecureTool<Map<String, Object>, Object>) secureTool;

                // 校验输入
                typedTool.validate(arguments, context);

                // 执行
                Object output = typedTool.execute(arguments, context);

                // 过滤输出
                return typedTool.filterOutput(output, context);
            } else {
                // 普通工具：无上下文执行
                @SuppressWarnings("unchecked")
                Tool<Map<String, Object>, Object> typedTool =
                    (Tool<Map<String, Object>, Object>) tool;
                return typedTool.execute(arguments);
            }
        }).get(timeoutMs, TimeUnit.MILLISECONDS);
    }

    /**
     * 构建包含工具结果的请求。
     */
    private @NonNull LlmRequest buildRequestWithToolResults(
            @NonNull LlmRequest previousRequest,
            @NonNull LlmResponse response,
            @NonNull List<ToolResult> toolResults) {

        List<io.github.afgprojects.framework.ai.core.memory.Message> messages =
            new ArrayList<>(previousRequest.messages());

        messages.add(io.github.afgprojects.framework.ai.core.memory.Message.assistantWithTools(
            response.content(),
            new ArrayList<>(response.toolCalls())
        ));

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

    /**
     * 判断工具是否需要审计。
     */
    private boolean isAuditable(@NonNull Tool<?, ?> tool) {
        if (tool instanceof SecureTool<?, ?> secureTool) {
            return secureTool.isAuditable();
        }
        return true; // 默认审计
    }

    /**
     * 序列化参数。
     */
    private @NonNull String serializeArguments(@NonNull Map<String, Object> arguments) {
        Map<String, Object> filtered = new HashMap<>(arguments);
        filtered.remove("__tool__");
        return filtered.toString();
    }

    /**
     * 构建内容安全上下文。
     */
    private ContentSafetyChecker.SafetyCheckContext buildSafetyContext(@NonNull ToolContext context) {
        return new ContentSafetyChecker.SafetyCheckContext() {
            @Override
            public @Nullable String getUserId() {
                return context.getUserId();
            }

            @Override
            public @Nullable String getTenantId() {
                return context.getTenantId();
            }

            @Override
            public @Nullable String getModelName() {
                return null;
            }

            @Override
            public @Nullable String getOperationType() {
                return "tool_execution";
            }

            @Override
            public boolean isStrictMode() {
                return false;
            }

            @Override
            public @NonNull List<String> getCheckCategories() {
                return List.of();
            }
        };
    }
}
