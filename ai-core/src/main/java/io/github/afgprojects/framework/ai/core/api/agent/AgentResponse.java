package io.github.afgprojects.framework.ai.core.api.agent;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Agent 响应对象
 *
 * <p>封装 Agent 执行后的响应信息，包含：
 * <ul>
 *   <li>输出 - Agent 的输出内容</li>
 *   <li>工具调用 - 需要执行的工具调用列表</li>
 *   <li>状态 - 执行状态</li>
 *   <li>元数据 - 额外的元数据信息</li>
 * </ul>
 *
 * @param output     Agent 输出
 * @param toolCalls  工具调用列表
 * @param status     执行状态
 * @param metadata   元数据
 * @author afg-projects
 * @since 1.0.0
 */
public record AgentResponse(
    @Nullable String output,
    @NonNull List<Object> toolCalls,
    @NonNull AgentStatus status,
    @NonNull Map<String, Object> metadata
) {

    /**
     * 创建成功完成的响应
     *
     * @param output 输出内容
     * @return 响应对象
     */
    @NonNull
    public static AgentResponse completed(@Nullable String output) {
        return new AgentResponse(output, List.of(), AgentStatus.COMPLETED, Map.of());
    }

    /**
     * 创建需要用户输入的响应
     *
     * @param prompt 提示信息
     * @return 响应对象
     */
    @NonNull
    public static AgentResponse needsInput(@NonNull String prompt) {
        return new AgentResponse(prompt, List.of(), AgentStatus.NEEDS_INPUT, Map.of());
    }

    /**
     * 创建工具调用的响应
     *
     * @param toolCalls 工具调用列表
     * @return 响应对象
     */
    @NonNull
    public static AgentResponse toolCalling(@NonNull List<Object> toolCalls) {
        return new AgentResponse(null, toolCalls, AgentStatus.TOOL_CALLING, Map.of());
    }

    /**
     * 创建错误的响应
     *
     * @param errorMessage 错误信息
     * @return 响应对象
     */
    @NonNull
    public static AgentResponse error(@NonNull String errorMessage) {
        return new AgentResponse(
            errorMessage,
            List.of(),
            AgentStatus.ERROR,
            Map.of("error", true)
        );
    }

    /**
     * 创建错误的响应（带异常）
     *
     * @param errorMessage 错误信息
     * @param throwable    异常
     * @return 响应对象
     */
    @NonNull
    public static AgentResponse error(@NonNull String errorMessage, @NonNull Throwable throwable) {
        return new AgentResponse(
            errorMessage,
            List.of(),
            AgentStatus.ERROR,
            Map.of("error", true, "exception", throwable)
        );
    }

    /**
     * 判断是否已完成
     *
     * @return 是否已完成
     */
    public boolean isCompleted() {
        return status == AgentStatus.COMPLETED;
    }

    /**
     * 判断是否需要输入
     *
     * @return 是否需要输入
     */
    public boolean needsInput() {
        return status == AgentStatus.NEEDS_INPUT;
    }

    /**
     * 判断是否正在调用工具
     *
     * @return 是否正在调用工具
     */
    public boolean isToolCalling() {
        return status == AgentStatus.TOOL_CALLING;
    }

    /**
     * 判断是否出错
     *
     * @return 是否出错
     */
    public boolean isError() {
        return status == AgentStatus.ERROR;
    }
}
