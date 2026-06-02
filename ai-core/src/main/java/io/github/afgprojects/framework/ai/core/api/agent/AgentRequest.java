package io.github.afgprojects.framework.ai.core.api.agent;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Agent 请求对象
 *
 * <p>封装发送给 Agent 的请求信息，包含：
 * <ul>
 *   <li>会话 ID - 用于标识会话上下文</li>
 *   <li>用户输入 - 用户的原始输入</li>
 *   <li>上下文 - 额外的上下文信息</li>
 *   <li>历史记录 - 对话历史</li>
 * </ul>
 *
 * @param sessionId  会话 ID
 * @param userInput  用户输入
 * @param context    上下文信息
 * @param history    对话历史
 * @author afg-projects
 * @since 1.0.0
 */
public record AgentRequest(
    @NonNull String sessionId,
    @NonNull String userInput,
    @NonNull Map<String, Object> context,
    @NonNull List<Object> history
) {

    /**
     * 创建带有最小参数的请求
     *
     * @param sessionId 会话 ID
     * @param userInput 用户输入
     */
    public AgentRequest(@NonNull String sessionId, @NonNull String userInput) {
        this(sessionId, userInput, Map.of(), List.of());
    }

    /**
     * 创建带有上下文的请求
     *
     * @param sessionId 会话 ID
     * @param userInput 用户输入
     * @param context   上下文信息
     */
    public AgentRequest(
        @NonNull String sessionId,
        @NonNull String userInput,
        @NonNull Map<String, Object> context
    ) {
        this(sessionId, userInput, context, List.of());
    }
}
