package io.github.afgprojects.framework.ai.agentic;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.tool.ToolExecutor;
import io.github.afgprojects.framework.ai.core.tool.Tool;
import io.github.afgprojects.framework.ai.core.tool.ToolResult;
import org.jspecify.annotations.NonNull;

/**
 * AFG Tool 与 LangChain4j Tool 的适配器
 *
 * <p>将 AFG Tool 接口适配到 LangChain4j 的 ToolSpecification。
 *
 * <p>注意：LangChain4j 1.0.0 的 API 与之前版本有较大差异，
 * 本适配器提供基础的转换功能。
 *
 * @author afg-projects
 * @since 1.0.0
 */
public final class ToolAdapter {

    private ToolAdapter() {
        // 私有构造函数，防止实例化
    }

    /**
     * 将 AFG Tool 转换为 LangChain4j ToolSpecification
     *
     * @param tool AFG Tool
     * @return LangChain4j ToolSpecification
     */
    public static ToolSpecification toToolSpecification(@NonNull Tool<?, ?> tool) {
        return ToolSpecification.builder()
            .name(tool.name())
            .description(tool.description())
            .build();
    }

    /**
     * 创建 LangChain4j ToolExecutor 包装器
     *
     * <p>返回一个可以执行 AFG Tool 的 LangChain4j ToolExecutor。
     * 注意：此实现假设 Tool 的输入类型为 ToolContext 或类似结构。
     *
     * @param tool AFG Tool
     * @return LangChain4j ToolExecutor
     */
    @NonNull
    @SuppressWarnings("unchecked")
    public static ToolExecutor toToolExecutor(@NonNull Tool<?, ?> tool) {
        return (toolExecutionRequest, memoryId) -> {
            // 使用 null 作为输入（简化实现）
            // 实际使用时需要根据 Tool 的输入类型进行处理
            Object result = tool.execute(null);

            if (result instanceof ToolResult toolResult) {
                if (toolResult.isSuccess()) {
                    return toolResult.output() != null ? toolResult.output() : "Success";
                } else {
                    return "Error: " + toolResult.error();
                }
            }

            return result != null ? result.toString() : "Success";
        };
    }
}