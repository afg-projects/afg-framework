package io.github.afgprojects.framework.ai.agent.executor;

import io.github.afgprojects.framework.ai.core.chat.AfgChatClient;
import io.github.afgprojects.framework.ai.core.chat.AiChatResponse;
import io.github.afgprojects.framework.ai.core.chat.AiMessage;
import io.github.afgprojects.framework.ai.core.exception.ToolException;
import io.github.afgprojects.framework.ai.core.security.ContentSafetyChecker;
import io.github.afgprojects.framework.ai.core.tool.SecureTool;
import io.github.afgprojects.framework.ai.core.tool.Tool;
import io.github.afgprojects.framework.ai.core.tool.ToolAuditLogger;
import io.github.afgprojects.framework.ai.core.tool.ToolContext;
import io.github.afgprojects.framework.ai.core.tool.ToolContextProvider;
import io.github.afgprojects.framework.ai.core.tool.ToolPermissionChecker;
import io.github.afgprojects.framework.ai.core.tool.ToolPermissionChecker.PermissionResult;
import io.github.afgprojects.framework.ai.core.tool.ToolRegistry;
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
 * <p>使用 AfgChatClient 进行对话，Spring AI Advisor 链处理原生工具调用。
 * 安全检查通过 Advisor 链中的安全 Advisor 拦截实现。
 *
 * @since 1.0.0
 */
public class SecureToolExecutor {

    private static final Logger log = LoggerFactory.getLogger(SecureToolExecutor.class);

    private final ToolRegistry toolRegistry;
    private final AfgChatClient chatClient;
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
     * @param chatClient           对话客户端
     * @param contextProvider      上下文提供者
     * @param permissionChecker    权限检查器（可选）
     * @param auditLogger          审计日志器（可选）
     * @param contentSafetyChecker 内容安全检查器（可选）
     * @param maxIterations        最大迭代次数
     * @param timeoutMs            执行超时（毫秒）
     */
    public SecureToolExecutor(
            @NonNull ToolRegistry toolRegistry,
            @NonNull AfgChatClient chatClient,
            @NonNull ToolContextProvider contextProvider,
            @Nullable ToolPermissionChecker permissionChecker,
            @Nullable ToolAuditLogger auditLogger,
            @Nullable ContentSafetyChecker contentSafetyChecker,
            int maxIterations,
            long timeoutMs) {
        this.toolRegistry = toolRegistry;
        this.chatClient = chatClient;
        this.contextProvider = contextProvider;
        this.permissionChecker = permissionChecker;
        this.auditLogger = auditLogger;
        this.contentSafetyChecker = contentSafetyChecker;
        this.maxIterations = maxIterations;
        this.timeoutMs = timeoutMs;
        this.enableContentSafety = contentSafetyChecker != null;
    }

    /**
     * 执行带安全检查的工具调用对话。
     *
     * @param systemPrompt 系统提示词
     * @param messages     对话消息列表
     * @return 最终响应
     */
    public @NonNull AiChatResponse executeWithTools(
            @Nullable String systemPrompt,
            @NonNull List<AiMessage> messages
    ) {
        return executeWithTools(systemPrompt, messages, contextProvider.provide());
    }

    /**
     * 执行带安全检查的工具调用对话（指定上下文）。
     *
     * @param systemPrompt 系统提示词
     * @param messages     对话消息列表
     * @param context      工具上下文
     * @return 最终响应
     */
    public @NonNull AiChatResponse executeWithTools(
            @Nullable String systemPrompt,
            @NonNull List<AiMessage> messages,
            @NonNull ToolContext context
    ) {
        AfgChatClient client = systemPrompt != null
                ? chatClient.withSystemPrompt(systemPrompt)
                : chatClient;

        List<AiMessage> currentMessages = new ArrayList<>(messages);
        AiChatResponse lastResponse = null;

        for (int iteration = 1; iteration <= maxIterations; iteration++) {
            log.debug("Tool execution iteration {}", iteration);

            // 检查输入内容安全性（首轮对话）
            if (enableContentSafety && iteration == 1) {
                String inputContent = serializeMessages(currentMessages);
                var checkResult = contentSafetyChecker.checkInput(inputContent, buildSafetyContext(context));
                if (checkResult.isBlocked()) {
                    log.warn("Input blocked by content safety: {}", checkResult.getReason());
                    return AiChatResponse.of("Content blocked: " + checkResult.getReason());
                }
            }

            // 调用 LLM（Spring AI Advisor 链自动处理工具调用，安全 Advisor 拦截权限检查）
            AiChatResponse response = client.chat(currentMessages);
            String content = response.content() != null ? response.content() : "";

            if (content.isBlank()) {
                log.debug("Empty response at iteration {}, continuing", iteration);
                continue;
            }

            // 检查输出内容安全性
            if (enableContentSafety) {
                var checkResult = contentSafetyChecker.checkOutput(content, buildSafetyContext(context));
                if (checkResult.isBlocked()) {
                    log.warn("Output blocked by content safety: {}", checkResult.getReason());
                    return AiChatResponse.of("Output blocked: " + checkResult.getReason());
                }
            }

            // Advisor 链已处理工具调用（包括安全检查）
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
     * 执行带安全检查的工具调用对话（简化版）
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
     * 执行带安全检查的工具调用对话（简化版，指定上下文）
     *
     * @param task    用户任务
     * @param context 工具上下文
     * @return 最终响应
     */
    public @NonNull AiChatResponse executeWithTools(@NonNull String task, @NonNull ToolContext context) {
        List<AiMessage> messages = new ArrayList<>();
        messages.add(AiMessage.user(task));
        return executeWithTools(null, messages, context);
    }

    /**
     * 异步执行带安全检查的工具调用对话。
     */
    public @NonNull CompletableFuture<AiChatResponse> executeWithToolsAsync(
            @Nullable String systemPrompt,
            @NonNull List<AiMessage> messages
    ) {
        return CompletableFuture.supplyAsync(() -> executeWithTools(systemPrompt, messages));
    }

    /**
     * 异步执行带安全检查的工具调用对话（简化版）。
     */
    public @NonNull CompletableFuture<AiChatResponse> executeWithToolsAsync(@NonNull String task) {
        return CompletableFuture.supplyAsync(() -> executeWithTools(task));
    }

    /**
     * 执行单个安全工具（带完整安全检查流程）。
     *
     * @param toolName  工具名称
     * @param arguments 工具参数
     * @param context   工具上下文
     * @return 工具执行结果，null 表示执行失败
     */
    public @Nullable String executeToolSecure(
            @NonNull String toolName,
            @NonNull Map<String, Object> arguments,
            @NonNull ToolContext context
    ) {
        log.debug("Executing tool securely: {}", toolName);
        Instant start = Instant.now();
        String callId = "call-" + System.currentTimeMillis();
        String recordId = null;

        // 1. 查找工具
        var toolOpt = toolRegistry.getTool(toolName);
        if (toolOpt.isEmpty()) {
            log.warn("Tool not found: {}", toolName);
            return "Error: Tool not found: " + toolName;
        }
        Tool<?, ?> tool = toolOpt.get();

        // 2. 权限检查
        if (tool instanceof SecureTool<?, ?> secureTool && permissionChecker != null) {
            var result = permissionChecker.checkSecureTool(secureTool, context);
            if (result.isDenied()) {
                log.warn("Tool {} permission denied: {}", toolName, result.getDenyReason());
                if (auditLogger != null && secureTool.isAuditable()) {
                    auditLogger.logPermissionDenied(callId, toolName, result.getDenyReason(), context);
                }
                return "Error: Permission denied: " + result.getDenyReason();
            }
        }

        // 3. 内容安全检查（输入）
        if (enableContentSafety) {
            String inputContent = serializeArguments(arguments);
            var checkResult = contentSafetyChecker.checkInput(inputContent, buildSafetyContext(context));
            if (checkResult.isBlocked()) {
                log.warn("Tool {} input blocked by content safety: {}", toolName, checkResult.getReason());
                return "Error: Content blocked: " + checkResult.getReason();
            }
        }

        // 4. 记录审计开始
        if (auditLogger != null && isAuditable(tool)) {
            Map<String, Object> auditArgs = new HashMap<>(arguments);
            auditArgs.put("__tool__", tool);
            recordId = auditLogger.logStart(callId, toolName, auditArgs, context);
        }

        try {
            // 5. 执行工具
            Object output = executeWithTimeout(tool, arguments, context);

            // 6. 内容安全检查（输出）
            if (enableContentSafety && output != null) {
                var checkResult = contentSafetyChecker.checkOutput(output.toString(), buildSafetyContext(context));
                if (checkResult.isBlocked()) {
                    log.warn("Tool {} output blocked by content safety: {}", toolName, checkResult.getReason());
                    if (recordId != null) {
                        auditLogger.logFailure(recordId, "Output blocked: " + checkResult.getReason(),
                            Duration.between(start, Instant.now()));
                    }
                    return "Error: Output blocked: " + checkResult.getReason();
                }
            }

            // 7. 记录审计成功
            Duration duration = Duration.between(start, Instant.now());
            if (recordId != null) {
                auditLogger.logSuccess(recordId, output, duration);
            }

            log.debug("Tool {} executed successfully in {}ms", toolName, duration.toMillis());
            return output != null ? output.toString() : "";

        } catch (java.util.concurrent.TimeoutException e) {
            Duration duration = Duration.between(start, Instant.now());
            log.error("Tool {} execution timeout after {}ms", toolName, timeoutMs);
            if (recordId != null) {
                auditLogger.logTimeout(callId, toolName, context, Duration.ofMillis(timeoutMs));
            }
            return "Error: Tool execution timeout after " + timeoutMs + "ms";

        } catch (Exception e) {
            Duration duration = Duration.between(start, Instant.now());
            log.error("Tool {} execution failed: {}", toolName, e.getMessage());
            if (recordId != null) {
                auditLogger.logFailure(recordId, e.getMessage(), duration);
            }
            return "Error: " + e.getMessage();
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
     * 序列化消息列表。
     */
    private @NonNull String serializeMessages(@NonNull List<AiMessage> messages) {
        StringBuilder sb = new StringBuilder();
        for (AiMessage msg : messages) {
            if (msg.content() != null) {
                sb.append(msg.content()).append("\n");
            }
        }
        return sb.toString();
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
            public @NonNull String getOperationType() {
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