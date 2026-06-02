package io.github.afgprojects.framework.ai.langchain4j.tool;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import io.github.afgprojects.framework.ai.core.api.tool.Tool;
import io.github.afgprojects.framework.ai.core.api.tool.ToolExecutionException;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

/**
 * LangChain4j 工作流工具节点 - 封装 AFG Tool 以便在 LangChain4j Agent 工作流中使用
 *
 * <p>将 AFG 的 {@link Tool} 包装为 LangChain4j 可调用的工具节点，
 * 处理 JSON 序列化/反序列化和执行逻辑。
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class Lc4jToolNode {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final Tool<?, ?> tool;
    private final ToolSpecification toolSpecification;

    public Lc4jToolNode(@NonNull Tool<?, ?> tool) {
        this.tool = tool;
        this.toolSpecification = Lc4jToolAdapter.toToolSpecification(tool);
    }

    /**
     * 获取 LangChain4j ToolSpecification
     */
    public ToolSpecification toolSpecification() {
        return toolSpecification;
    }

    /**
     * 获取原始 AFG Tool
     */
    public Tool<?, ?> tool() {
        return tool;
    }

    /**
     * 执行工具调用
     *
     * @param request LangChain4j 工具执行请求
     * @return 执行结果字符串
     */
    @SuppressWarnings("unchecked")
    public String execute(@NonNull ToolExecutionRequest request) {
        try {
            Object input = deserializeInput(request.arguments());
            Object result = tool.execute(input);
            return serializeOutput(result);
        } catch (ToolExecutionException e) {
            return "Error: " + e.getMessage();
        } catch (Exception e) {
            return "Error executing tool '" + tool.name() + "': " + e.getMessage();
        }
    }

    /**
     * 反序列化工具输入参数
     */
    @SuppressWarnings("unchecked")
    private Object deserializeInput(@Nullable String arguments) {
        if (arguments == null || arguments.isBlank()) {
            return Map.of();
        }

        try {
            TypeReference<?> inputType = tool.inputType();
            if (inputType != null) {
                return OBJECT_MAPPER.readValue(arguments, inputType);
            }
            // 默认反序列化为 Map
            return OBJECT_MAPPER.readValue(arguments, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new ToolExecutionException(
                "Failed to deserialize tool input for '" + tool.name() + "': " + e.getMessage(), e);
        }
    }

    /**
     * 序列化工具输出
     */
    private String serializeOutput(@Nullable Object result) {
        if (result == null) {
            return "";
        }
        if (result instanceof String s) {
            return s;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(result);
        } catch (Exception e) {
            return String.valueOf(result);
        }
    }

    /**
     * 批量创建工具节点
     */
    public static List<Lc4jToolNode> fromTools(@NonNull List<Tool<?, ?>> tools) {
        return tools.stream().map(Lc4jToolNode::new).toList();
    }

    /**
     * 批量获取 ToolSpecification 列表
     */
    public static List<ToolSpecification> toToolSpecifications(@NonNull List<Lc4jToolNode> nodes) {
        return nodes.stream().map(Lc4jToolNode::toolSpecification).toList();
    }
}
