package io.github.afgprojects.framework.integration.event.rabbitmq;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RabbitMQ 事件发布配置属性
 *
 * <h3>配置示例</h3>
 * <pre>
 * afg:
 *   rabbitmq:
 *     event:
 *       enabled: true
 *       exchange: afg-events
 *       default-routing-key: event.default
 * </pre>
 *
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "afg.rabbitmq.event")
public class RabbitMQEventProperties {

    /**
     * 是否启用
     */
    private boolean enabled = true;

    /**
     * 默认交换机名称
     */
    private String exchange = "afg-events";

    /**
     * 默认路由键
     */
    private String defaultRoutingKey = "event.default";

    /**
     * 是否包含元数据（traceId、eventId 等）
     */
    private boolean includeMetadata = true;

    /**
     * 消息 TTL（毫秒），0 表示不设置
     */
    private long messageTtl = 0;
}