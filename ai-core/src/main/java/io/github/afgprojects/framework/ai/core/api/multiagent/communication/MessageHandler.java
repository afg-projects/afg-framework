package io.github.afgprojects.framework.ai.core.api.multiagent.communication;

/**
 * 消息处理器
 *
 * @author afg-projects
 * @since 1.0.0
 */
@FunctionalInterface
public interface MessageHandler {
    /**
     * 处理消息
     *
     * @param message 消息
     */
    void handle(AgentMessage message);
}
