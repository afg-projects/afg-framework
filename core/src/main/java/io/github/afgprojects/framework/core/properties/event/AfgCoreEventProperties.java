package io.github.afgprojects.framework.core.properties.event;

import lombok.Data;

/**
 * 事件配置。
 */
@Data
public class AfgCoreEventProperties {

    /**
     * 是否启用事件驱动。
     */
    private boolean enabled = true;

    /**
     * 事件发布类型。
     */
    private EventType type = EventType.LOCAL;

    /**
     * 默认主题名称。
     */
    private String defaultTopic = "afg.events";

    /**
     * 本地事件配置。
     */
    private AfgCoreEventLocalProperties local = new AfgCoreEventLocalProperties();

    /**
     * Kafka 配置。
     */
    private AfgCoreEventKafkaProperties kafka = new AfgCoreEventKafkaProperties();

    /**
     * RabbitMQ 配置。
     */
    private AfgCoreEventRabbitMqProperties rabbitmq = new AfgCoreEventRabbitMqProperties();

    /**
     * 重试配置。
     */
    private AfgCoreEventRetryProperties retry = new AfgCoreEventRetryProperties();

    /**
     * 死信队列配置。
     */
    private AfgCoreEventDeadLetterProperties deadLetter = new AfgCoreEventDeadLetterProperties();
}
