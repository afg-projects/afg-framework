package io.github.afgprojects.framework.ai.core.agent;

import io.github.afgprojects.framework.ai.core.api.multiagent.communication.AgentMessage;
import io.github.afgprojects.framework.ai.core.api.multiagent.communication.MessageHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * 日志消息处理器
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
public class LoggingMessageHandler implements MessageHandler {

    @Override
    public void handle(AgentMessage message) {
        log.info("Agent message: from=[{}] to=[{}] type=[{}] payload={}",
                message.fromAgent(), message.toAgent(),
                message.type(), message.payload());
    }
}
