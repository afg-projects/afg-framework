package io.github.afgprojects.framework.ai.llm.ollama;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.afgprojects.framework.ai.core.tool.SecureTool;
import io.github.afgprojects.framework.ai.core.tool.Tool;
import io.github.afgprojects.framework.ai.core.tool.ToolDefinition;
import io.github.afgprojects.framework.ai.core.tool.ToolRegistry;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;

import java.util.Map;

/**
 * AFG 框架的 Spring AI ToolCallback 实现
 *
 * <p>将 AFG 框架的 {@link ToolDefinition} 和 {@link ToolRegistry}
 * 适配为 Spring AI 的 {@link ToolCallback} 接口。
 *
 * <p>正确处理泛型类型和 ToolContext：
 * <ul>
 *   <li>Tool 提供了 inputType() → 使用 TypeReference 精确反序列化</li>
 *   <li>Tool 未提供 inputType() → 使用 Map 作为默认类型</li>
 *   <li>call(String, ToolContext) → 将 Spring AI ToolContext 映射为 AFG ToolContext</li>
 * </ul>
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class AfgToolCallback implements ToolCallback {

    private static final Logger log = LoggerFactory.getLogger(AfgToolCallback.class);
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final ToolDefinition toolDefinition;
    private final @Nullable ToolRegistry toolRegistry;
    private final ObjectMapper objectMapper;

    private final org.springframework.ai.tool.definition.ToolDefinition springAiToolDefinition;

    public AfgToolCallback(
            @NonNull ToolDefinition toolDefinition,
            @Nullable ToolRegistry toolRegistry,
            @NonNull ObjectMapper objectMapper
    ) {
        this.toolDefinition = toolDefinition;
        this.toolRegistry = toolRegistry;
        this.objectMapper = objectMapper;

        this.springAiToolDefinition = org.springframework.ai.tool.definition.ToolDefinition.builder()
                .name(toolDefinition.name())
                .description(toolDefinition.description())
                .inputSchema(toolDefinition.inputSchema())
                .build();
    }

    @Override
    public org.springframework.ai.tool.definition.ToolDefinition getToolDefinition() {
        return springAiToolDefinition;
    }

    @Override
    public String call(@NonNull String functionInput) {
        return call(functionInput, null);
    }

    @Override
    public String call(@NonNull String functionInput,
            org.springframework.ai.chat.model.@Nullable ToolContext toolContext) {
        log.debug("Tool {} called with input: {}", toolDefinition.name(), functionInput);

        if (toolRegistry == null) {
            return "Tool '" + toolDefinition.name() + "' is defined but ToolRegistry is not available.";
        }

        var toolOpt = toolRegistry.getTool(toolDefinition.name());
        if (toolOpt.isEmpty()) {
            return "Tool '" + toolDefinition.name() + "' is not registered in ToolRegistry.";
        }

        try {
            @SuppressWarnings("unchecked")
            Tool<Object, Object> tool = (Tool<Object, Object>) toolOpt.get();

            // 获取 Tool 提供的输入类型
            TypeReference<?> inputTypeRef = tool.inputType();

            // 根据类型反序列化
            Object input;
            if (inputTypeRef != null) {
                input = objectMapper.readValue(functionInput, inputTypeRef);
            } else {
                input = objectMapper.readValue(functionInput, MAP_TYPE);
            }

            // 执行工具
            Object result;
            if (tool instanceof SecureTool<Object, Object> secureTool) {
                // SecureTool: 使用 ToolContext 执行
                io.github.afgprojects.framework.ai.core.tool.ToolContext afgContext = convertToAfgToolContext(toolContext);
                result = secureTool.execute(input, afgContext);
            } else {
                // 普通 Tool: 无上下文执行
                result = tool.execute(input);
            }

            // 序列化结果
            if (result == null) {
                return "";
            }
            if (result instanceof String str) {
                return str;
            }
            return objectMapper.writeValueAsString(result);

        } catch (Exception e) {
            log.error("Tool {} execution failed: {}", toolDefinition.name(), e.getMessage(), e);
            return "Error: " + e.getClass().getSimpleName() + " - " + e.getMessage();
        }
    }

    /**
     * 将 Spring AI ToolContext 转换为 AFG ToolContext
     */
    private io.github.afgprojects.framework.ai.core.tool.@NonNull ToolContext convertToAfgToolContext(
            org.springframework.ai.chat.model.@Nullable ToolContext springAiContext) {
        if (springAiContext == null) {
            return io.github.afgprojects.framework.ai.core.tool.ToolContext.builder().build();
        }

        io.github.afgprojects.framework.ai.core.tool.ToolContext.Builder builder =
                io.github.afgprojects.framework.ai.core.tool.ToolContext.builder();

        // 从 Spring AI ToolContext 提取上下文信息
        Object contextInfo = springAiContext.getContext();
        if (contextInfo instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> contextMap = (Map<String, Object>) contextInfo;

            // 提取常用字段
            if (contextMap.containsKey("userId")) {
                builder.userId((String) contextMap.get("userId"));
            }
            if (contextMap.containsKey("tenantId")) {
                builder.tenantId((String) contextMap.get("tenantId"));
            }
            if (contextMap.containsKey("sessionId")) {
                builder.sessionId((String) contextMap.get("sessionId"));
            }
            if (contextMap.containsKey("originalHeaders")) {
                @SuppressWarnings("unchecked")
                Map<String, String> headers = (Map<String, String>) contextMap.get("originalHeaders");
                builder.originalHeaders(headers);
            }
        }

        return builder.build();
    }
}