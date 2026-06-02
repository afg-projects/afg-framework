package io.github.afgprojects.framework.ai.langchain4j.tool;

import dev.langchain4j.agent.tool.ToolSpecification;
import io.github.afgprojects.framework.ai.core.api.tool.Tool;
import org.jspecify.annotations.NonNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 将 AFG Tool 接口适配为 LangChain4j ToolSpecification
 *
 * <p>AFG 的 {@link Tool} 接口定义了 name、description、inputSchema，
 * 此适配器将其转换为 LangChain4j 的 {@link ToolSpecification}，
 * 以便在 LangChain4j 的 function calling 中使用。
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class Lc4jToolAdapter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 将 AFG Tool 转换为 LangChain4j ToolSpecification
     *
     * @param tool AFG Tool 实例
     * @return LangChain4j ToolSpecification
     */
    public static ToolSpecification toToolSpecification(@NonNull Tool<?, ?> tool) {
        var builder = ToolSpecification.builder()
                .name(tool.name())
                .description(tool.description());

        // 解析 inputSchema 为 ToolSpecification 的 parameters
        String inputSchema = tool.inputSchema();
        if (inputSchema != null && !inputSchema.isBlank() && !inputSchema.equals("{}")) {
            try {
                JsonNode schemaNode = OBJECT_MAPPER.readTree(inputSchema);
                builder.parameters(schemaNode);
            } catch (Exception e) {
                // 如果解析失败，忽略 schema
            }
        }

        return builder.build();
    }
}
