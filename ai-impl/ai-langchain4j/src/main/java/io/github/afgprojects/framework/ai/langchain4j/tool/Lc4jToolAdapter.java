package io.github.afgprojects.framework.ai.langchain4j.tool;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import io.github.afgprojects.framework.ai.core.api.tool.Tool;

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
@lombok.extern.slf4j.Slf4j
public class Lc4jToolAdapter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 将 AFG Tool 转换为 LangChain4j ToolSpecification
     *
     * @param tool AFG Tool 实例
     * @return LangChain4j ToolSpecification
     */
    public static ToolSpecification toToolSpecification(Tool<?, ?> tool) {
        var builder = ToolSpecification.builder()
                .name(tool.name())
                .description(tool.description());

        // 解析 inputSchema 为 ToolSpecification 的 parameters（JsonObjectSchema）
        String inputSchema = tool.inputSchema();
        if (inputSchema != null && !inputSchema.isBlank() && !inputSchema.equals("{}")) {
            try {
                JsonObjectSchema schema = parseToJsonObjectSchema(inputSchema);
                if (schema != null) {
                    builder.parameters(schema);
                }
            } catch (Exception e) {
                log.warn("Failed to parse input schema for tool '{}', proceeding without schema: {}",
                        tool.name(), e.getMessage());
            }
        }

        return builder.build();
    }

    /**
     * 将 JSON schema 字符串解析为 LangChain4j JsonObjectSchema
     * <p>
     * 遍历 JSON schema 的 properties，将每个属性转换为对应的 JsonSchemaElement。
     * 当前仅支持 string、integer、number、boolean 类型的顶层属性。
     */
    private static JsonObjectSchema parseToJsonObjectSchema(String inputSchema) throws Exception {
        JsonNode schemaNode = OBJECT_MAPPER.readTree(inputSchema);
        var schemaBuilder = JsonObjectSchema.builder();

        JsonNode properties = schemaNode.get("properties");
        if (properties != null && properties.isObject()) {
            var fields = properties.fields();
            while (fields.hasNext()) {
                var entry = fields.next();
                String propName = entry.getKey();
                JsonNode propNode = entry.getValue();
                String type = propNode.has("type") ? propNode.get("type").asText() : "string";
                String description = propNode.has("description") ? propNode.get("description").asText() : null;

                switch (type) {
                    case "string" -> schemaBuilder.addStringProperty(propName, description);
                    case "integer" -> schemaBuilder.addIntegerProperty(propName, description);
                    case "number" -> schemaBuilder.addNumberProperty(propName, description);
                    case "boolean" -> schemaBuilder.addBooleanProperty(propName, description);
                    default -> schemaBuilder.addStringProperty(propName, description);
                }
            }
        }

        JsonNode required = schemaNode.get("required");
        if (required != null && required.isArray()) {
            var requiredList = new java.util.ArrayList<String>();
            for (JsonNode req : required) {
                requiredList.add(req.asText());
            }
            schemaBuilder.required(requiredList);
        }

        return schemaBuilder.build();
    }
}
