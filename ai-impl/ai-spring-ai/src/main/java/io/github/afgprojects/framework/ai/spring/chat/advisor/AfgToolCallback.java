package io.github.afgprojects.framework.ai.spring.chat.advisor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.afgprojects.framework.ai.core.api.tool.SecureTool;
import io.github.afgprojects.framework.ai.core.api.tool.Tool;
import io.github.afgprojects.framework.ai.core.api.tool.ToolRegistry;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.chat.model.ToolContext;

import java.util.Map;

/**
 * AFG 框架的 Spring AI ToolCallback 实现
 *
 * <p>将 AFG 框架的 {@link Tool} 和 {@link ToolRegistry}
 * 适配为 Spring AI 的 {@link ToolCallback} 接口。
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class AfgToolCallback implements ToolCallback {

    private static final Logger log = LoggerFactory.getLogger(AfgToolCallback.class);
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final Tool<?, ?> tool;
    private final @Nullable ToolRegistry toolRegistry;
    private final ObjectMapper objectMapper;

    private final org.springframework.ai.tool.definition.ToolDefinition springAiToolDefinition;

    public AfgToolCallback(
            @NonNull Tool<?, ?> tool,
            @Nullable ToolRegistry toolRegistry,
            @NonNull ObjectMapper objectMapper
    ) {
        this.tool = tool;
        this.toolRegistry = toolRegistry;
        this.objectMapper = objectMapper;

        this.springAiToolDefinition = org.springframework.ai.tool.definition.ToolDefinition.builder()
                .name(tool.name())
                .description(tool.description())
                .inputSchema(tool.inputSchema())
                .build();
    }

    @Override
    public org.springframework.ai.tool.definition.@NonNull ToolDefinition getToolDefinition() {
        return springAiToolDefinition;
    }

    @Override
    public @NonNull String call(@NonNull String functionInput) {
        return call(functionInput, null);
    }

    @Override
    public @NonNull String call(@NonNull String functionInput, @Nullable ToolContext toolContext) {
        log.debug("Tool {} called with input: {}", tool.name(), functionInput);

        if (toolRegistry == null) {
            return "Tool '" + tool.name() + "' is defined but ToolRegistry is not available.";
        }

        var toolOpt = toolRegistry.getTool(tool.name());
        if (toolOpt.isEmpty()) {
            return "Tool '" + tool.name() + "' is not registered in ToolRegistry.";
        }

        try {
            @SuppressWarnings("unchecked")
            Tool<Object, Object> targetTool = (Tool<Object, Object>) toolOpt.get();

            TypeReference<?> inputTypeRef = targetTool.inputType();

            Object input;
            if (inputTypeRef != null) {
                input = objectMapper.readValue(functionInput, inputTypeRef);
            } else {
                input = objectMapper.readValue(functionInput, MAP_TYPE);
            }

            Object result;
            if (targetTool instanceof SecureTool<Object, Object> secureTool) {
                io.github.afgprojects.framework.ai.core.api.tool.ToolContext afgContext = convertToAfgToolContext(toolContext);
                result = secureTool.execute(input, afgContext);
            } else {
                result = targetTool.execute(input);
            }

            if (result == null) {
                return "";
            }
            if (result instanceof String str) {
                return str;
            }
            return objectMapper.writeValueAsString(result);

        } catch (Exception e) {
            log.error("Tool {} execution failed: {}", tool.name(), e.getMessage(), e);
            throw new org.springframework.ai.tool.execution.ToolExecutionException(springAiToolDefinition, e);
        }
    }

    private io.github.afgprojects.framework.ai.core.api.tool.@NonNull ToolContext convertToAfgToolContext(
            @Nullable ToolContext springAiContext) {
        if (springAiContext == null) {
            return io.github.afgprojects.framework.ai.core.api.tool.ToolContext.builder().build();
        }

        io.github.afgprojects.framework.ai.core.api.tool.ToolContext.Builder builder =
                io.github.afgprojects.framework.ai.core.api.tool.ToolContext.builder();

        Object contextInfo = springAiContext.getContext();
        if (contextInfo instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> contextMap = (Map<String, Object>) contextInfo;

            if (contextMap.containsKey("userId")) {
                builder.userId((String) contextMap.get("userId"));
            }
            if (contextMap.containsKey("tenantId")) {
                builder.tenantId((String) contextMap.get("tenantId"));
            }
            if (contextMap.containsKey("sessionId")) {
                builder.sessionId((String) contextMap.get("sessionId"));
            }
        }

        return builder.build();
    }
}
