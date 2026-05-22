package io.github.afgprojects.framework.ai.agent.skill;

import io.github.afgprojects.framework.ai.core.model.LlmClient;
import io.github.afgprojects.framework.ai.core.model.LlmRequest;
import io.github.afgprojects.framework.ai.core.model.LlmResponse;
import io.github.afgprojects.framework.ai.core.tool.ToolDefinition;
import io.github.afgprojects.framework.ai.core.tool.ToolRegistry;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 默认 Skill 执行器实现
 *
 * <p>集成 Spring AI 的 ChatClient 和 Advisor 机制：
 * <ul>
 *   <li>提示词模板渲染（支持 {{variable}} 语法）</li>
 *   <li>系统提示注入（通过 Advisor）</li>
 *   <li>工具调用（自动处理）</li>
 *   <li>流式响应支持</li>
 * </ul>
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class DefaultSkillExecutor implements SkillExecutor {

    private static final Logger log = LoggerFactory.getLogger(DefaultSkillExecutor.class);
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*(\\w+)\\s*\\}\\}");

    private final SkillRegistry registry;
    private final LlmClient llmClient;
    private final @Nullable ToolRegistry toolRegistry;

    public DefaultSkillExecutor(@NonNull SkillRegistry registry, @NonNull LlmClient llmClient) {
        this(registry, llmClient, null);
    }

    public DefaultSkillExecutor(
            @NonNull SkillRegistry registry,
            @NonNull LlmClient llmClient,
            @Nullable ToolRegistry toolRegistry) {
        this.registry = registry;
        this.llmClient = llmClient;
        this.toolRegistry = toolRegistry;
    }

    @Override
    @NonNull
    public SkillResult execute(@NonNull String name, @NonNull Map<String, Object> inputs) {
        SkillContext context = new SkillContext(name, inputs);
        return execute(context);
    }

    @Override
    @NonNull
    public SkillResult execute(@NonNull SkillContext context) {
        log.info("Executing skill: {} with inputs: {}", context.getSkillName(), context.getInputs().keySet());

        // 1. 获取 Skill 定义
        var definitionOpt = registry.get(context.getSkillName());
        if (definitionOpt.isEmpty()) {
            return SkillResult.failure("Skill not found: " + context.getSkillName());
        }

        SkillDefinition definition = definitionOpt.get();

        // 2. 验证必填参数
        var validationResult = validateInputs(definition, context);
        if (!validationResult.isEmpty()) {
            return SkillResult.failure("Validation failed: " + validationResult);
        }

        // 3. 渲染提示词
        String prompt = renderPrompt(definition.prompt(), context);
        log.debug("Rendered prompt: {}", prompt);

        // 4. 构建 LLM 请求
        LlmRequest request = buildLlmRequest(definition, prompt, context);

        // 5. 调用 LLM
        try {
            LlmResponse response = llmClient.chat(request);

            // 6. 构建结果
            return new SkillResult(
                    true,
                    response.content(),
                    null,
                    convertToolCalls(response.toolCalls()),
                    null,
                    buildMetadata(definition, response)
            );

        } catch (Exception e) {
            log.error("Skill execution failed: {}", e.getMessage(), e);
            return SkillResult.failure("Execution failed: " + e.getMessage());
        }
    }

    @Override
    @NonNull
    public Flux<SkillResult> executeStream(@NonNull SkillContext context) {
        log.info("Executing skill (stream): {}", context.getSkillName());

        var definitionOpt = registry.get(context.getSkillName());
        if (definitionOpt.isEmpty()) {
            return Flux.just(SkillResult.failure("Skill not found: " + context.getSkillName()));
        }

        SkillDefinition definition = definitionOpt.get();
        String prompt = renderPrompt(definition.prompt(), context);
        LlmRequest request = buildLlmRequest(definition, prompt, context);

        return llmClient.chatStream(request)
                .map(response -> new SkillResult(
                        true,
                        response.content(),
                        null,
                        convertToolCalls(response.toolCalls()),
                        null,
                        Map.of("streaming", true)
                ));
    }

    @Override
    @NonNull
    public LlmResponse executeRaw(@NonNull SkillContext context) {
        var definitionOpt = registry.get(context.getSkillName());
        if (definitionOpt.isEmpty()) {
            throw new IllegalArgumentException("Skill not found: " + context.getSkillName());
        }

        SkillDefinition definition = definitionOpt.get();
        String prompt = renderPrompt(definition.prompt(), context);
        LlmRequest request = buildLlmRequest(definition, prompt, context);

        return llmClient.chat(request);
    }

    @Override
    @NonNull
    public String renderPrompt(@NonNull String prompt, @NonNull SkillContext context) {
        Matcher matcher = VARIABLE_PATTERN.matcher(prompt);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String variableName = matcher.group(1);
            Object value = resolveVariable(variableName, context);
            String replacement = value != null ? value.toString() : "";
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    @Override
    @NonNull
    public SkillRegistry getRegistry() {
        return registry;
    }

    /**
     * 构建 LLM 请求
     */
    @NonNull
    private LlmRequest buildLlmRequest(
            @NonNull SkillDefinition definition,
            @NonNull String prompt,
            @NonNull SkillContext context) {

        LlmRequest.Builder builder = LlmRequest.builder()
                .addMessage(io.github.afgprojects.framework.ai.core.memory.Message.user(prompt));

        // 添加系统提示（如果有）
        String systemPrompt = context.getVariable("systemPrompt");
        if (systemPrompt != null) {
            builder.systemPrompt(systemPrompt);
        }

        // 添加工具（如果有）
        List<ToolDefinition> tools = getTools(definition);
        if (!tools.isEmpty()) {
            builder.addTools(tools);
        }

        // 添加选项
        Map<String, Object> options = context.getVariable("options");
        if (options != null) {
            builder.options(options);
        }

        return builder.build();
    }

    @Override
    @NonNull
    public List<ToolDefinition> getTools(@NonNull SkillDefinition definition) {
        if (definition.tools() == null || definition.tools().isEmpty() || toolRegistry == null) {
            return List.of();
        }

        List<ToolDefinition> result = new ArrayList<>();
        for (String toolName : definition.tools()) {
            toolRegistry.getTool(toolName).ifPresent(tool -> {
                result.add(ToolDefinition.from(tool));
            });
        }
        return result;
    }

    /**
     * 解析变量值
     */
    private Object resolveVariable(String name, SkillContext context) {
        // 优先从输入参数获取
        Object value = context.getInput(name);
        if (value != null) {
            return value;
        }

        // 从变量获取
        value = context.getVariable(name);
        if (value != null) {
            return value;
        }

        // 从父上下文获取
        if (context.getParent() != null) {
            return resolveVariable(name, context.getParent());
        }

        return null;
    }

    /**
     * 验证输入参数
     */
    @NonNull
    private String validateInputs(@NonNull SkillDefinition definition, @NonNull SkillContext context) {
        if (definition.inputs() == null || definition.inputs().isEmpty()) {
            return "";
        }

        StringBuilder errors = new StringBuilder();

        for (var input : definition.inputs()) {
            if (input.required()) {
                Object value = context.getInput(input.name());
                if (value == null && input.defaultValue() == null) {
                    errors.append("Missing required parameter: ").append(input.name()).append("; ");
                }
            }
        }

        return errors.toString();
    }

    /**
     * 转换工具调用记录
     */
    @Nullable
    private List<SkillResult.ToolCallRecord> convertToolCalls(
            @Nullable List<io.github.afgprojects.framework.ai.core.tool.ToolCall> toolCalls) {
        if (toolCalls == null || toolCalls.isEmpty()) {
            return null;
        }

        return toolCalls.stream()
                .map(tc -> new SkillResult.ToolCallRecord(
                        tc.name(),
                        tc.arguments() != null ? tc.arguments().toString() : "{}",
                        null,
                        true
                ))
                .toList();
    }

    /**
     * 构建元数据
     */
    @NonNull
    private Map<String, Object> buildMetadata(@NonNull SkillDefinition definition, @NonNull LlmResponse response) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("skillName", definition.name());
        metadata.put("tools", definition.tools());
        metadata.put("dependsOn", definition.dependsOn());
        if (response.tokenUsage() != null) {
            metadata.put("tokenUsage", response.tokenUsage());
        }
        if (response.finishReason() != null) {
            metadata.put("finishReason", response.finishReason());
        }
        return metadata;
    }
}
