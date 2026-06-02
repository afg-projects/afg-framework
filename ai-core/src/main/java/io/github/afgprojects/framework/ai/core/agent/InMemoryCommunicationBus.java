package io.github.afgprojects.framework.ai.core.agent;

import io.github.afgprojects.framework.ai.core.api.multiagent.communication.AgentMessage;
import io.github.afgprojects.framework.ai.core.api.multiagent.communication.CommunicationBus;
import io.github.afgprojects.framework.ai.core.api.multiagent.communication.MessageHandler;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

/**
 * 内存通信总线实现
 *
 * <p>提供基于内存的消息传递机制，适用于单机部署场景。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
public class InMemoryCommunicationBus implements CommunicationBus {

    private final ConcurrentHashMap<String, BlockingQueue<AgentMessage>> agentQueues = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Map<String, MessageHandler>> topicSubscribers = new ConcurrentHashMap<>();

    @Override
    public void send(@NonNull String fromAgent, String toAgent, @NonNull AgentMessage message) {
        if (toAgent == null) {
            broadcast(message);
            return;
        }

        log.debug("Sending message from {} to {}", fromAgent, toAgent);
        BlockingQueue<AgentMessage> queue = agentQueues.computeIfAbsent(toAgent, k -> new LinkedBlockingQueue<>());
        queue.offer(message);
    }

    @Override
    @NonNull
    public Optional<AgentMessage> receive(@NonNull String agentId, @NonNull Duration timeout) {
        BlockingQueue<AgentMessage> queue = agentQueues.get(agentId);
        if (queue == null) {
            return Optional.empty();
        }

        try {
            AgentMessage message = queue.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
            return Optional.ofNullable(message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    @Override
    @NonNull
    public List<AgentMessage> getPendingMessages(@NonNull String agentId) {
        BlockingQueue<AgentMessage> queue = agentQueues.get(agentId);
        if (queue == null) {
            return List.of();
        }

        List<AgentMessage> messages = new ArrayList<>();
        queue.drainTo(messages);
        return messages;
    }

    @Override
    public void clearMessages(@NonNull String agentId) {
        log.debug("Clearing messages for agent: {}", agentId);
        BlockingQueue<AgentMessage> queue = agentQueues.get(agentId);
        if (queue != null) {
            queue.clear();
        }
    }

    @Override
    public void subscribe(@NonNull String topic, @NonNull String agentId, @NonNull MessageHandler handler) {
        log.debug("Agent {} subscribing to topic: {}", agentId, topic);
        topicSubscribers.computeIfAbsent(topic, k -> new ConcurrentHashMap<>())
                .put(agentId, handler);
    }

    @Override
    public void unsubscribe(@NonNull String topic, @NonNull String agentId) {
        log.debug("Agent {} unsubscribing from topic: {}", agentId, topic);
        Map<String, MessageHandler> subscribers = topicSubscribers.get(topic);
        if (subscribers != null) {
            subscribers.remove(agentId);
        }
    }

    @Override
    public void publish(@NonNull String topic, @NonNull AgentMessage message) {
        log.debug("Publishing message to topic: {}", topic);
        Map<String, MessageHandler> subscribers = topicSubscribers.get(topic);
        if (subscribers != null) {
            for (MessageHandler handler : subscribers.values()) {
                try {
                    handler.handle(message);
                } catch (Exception e) {
                    log.error("Error handling message for topic {}: {}", topic, e.getMessage());
                }
            }
        }
    }

    @Override
    public void broadcast(@NonNull AgentMessage message) {
        log.debug("Broadcasting message from {}", message.fromAgent());
        for (Map.Entry<String, BlockingQueue<AgentMessage>> entry : agentQueues.entrySet()) {
            if (!entry.getKey().equals(message.fromAgent())) {
                entry.getValue().offer(message);
            }
        }
    }
}