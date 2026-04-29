package io.github.afgprojects.framework.integration.event.rabbitmq;

import java.util.concurrent.CompletableFuture;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.amqp.rabbit.core.RabbitTemplate;

import io.github.afgprojects.framework.core.api.event.DomainEvent;
import io.github.afgprojects.framework.core.api.event.EventPublisher;

/**
 * RabbitMQ 事件发布实现
 *
 * <p>基于 Spring AMQP 实现的分布式事件发布器。
 * 适用于需要灵活路由和可靠消息传递的场景。
 *
 * <p>特点：
 * <ul>
 *   <li>支持多种交换机类型（Direct、Topic、Fanout、Headers）</li>
 *   <li>支持灵活的路由规则</li>
 *   <li>支持消息确认和持久化</li>
 *   <li>支持死信队列和延迟队列</li>
 * </ul>
 *
 * @since 1.0.0
 */
@SuppressWarnings("PMD.AvoidCatchingGenericException")
public class RabbitMQEventPublisher<T> implements EventPublisher<T> {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMQEventProperties properties;

    /**
     * 创建 RabbitMQ 事件发布器
     *
     * @param rabbitTemplate RabbitTemplate 实例
     * @param properties     事件配置属性
     */
    public RabbitMQEventPublisher(RabbitTemplate rabbitTemplate, RabbitMQEventProperties properties) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    @Override
    public void publish(@NonNull DomainEvent<T> event) {
        String exchange = properties.getExchange();
        String routingKey = resolveRoutingKey(event);

        log.debug("Publishing RabbitMQ event: exchange={}, routingKey={}, eventId={}",
                exchange, routingKey, event.eventId());

        rabbitTemplate.convertAndSend(exchange, routingKey, event.payload());

        log.debug("Successfully published RabbitMQ event: exchange={}, routingKey={}, eventId={}",
                exchange, routingKey, event.eventId());
    }

    @Override
    public CompletableFuture<Void> publishAsync(@NonNull DomainEvent<T> event) {
        String exchange = properties.getExchange();
        String routingKey = resolveRoutingKey(event);

        log.debug("Publishing RabbitMQ event asynchronously: exchange={}, routingKey={}, eventId={}",
                exchange, routingKey, event.eventId());

        // RabbitMQ 不直接支持异步发送的 CompletableFuture，
        // 这里使用线程池异步执行同步发送操作
        return CompletableFuture.runAsync(() -> {
            try {
                rabbitTemplate.convertAndSend(exchange, routingKey, event.payload());
                log.debug("Successfully published RabbitMQ event asynchronously: exchange={}, routingKey={}, eventId={}",
                        exchange, routingKey, event.eventId());
            } catch (Exception e) {
                log.error("Failed to publish RabbitMQ event: exchange={}, routingKey={}, eventId={}",
                        exchange, routingKey, event.eventId(), e);
                throw new RuntimeException("Failed to publish RabbitMQ event: " + event.eventId(), e);
            }
        });
    }

    /**
     * 解析路由键
     *
     * <p>RabbitMQ 使用路由键来将消息路由到不同的队列。
     * 默认使用事件的 topic 作为路由键。
     *
     * @param event 领域事件
     * @return 路由键
     */
    private String resolveRoutingKey(DomainEvent<T> event) {
        String topic = event.topic();
        if (topic != null && !topic.isEmpty()) {
            return topic;
        }
        return properties.getDefaultRoutingKey();
    }
}