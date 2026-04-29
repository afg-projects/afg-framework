package io.github.afgprojects.framework.integration.kafka;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Kafka 事件发布配置属性
 */
@Data
@ConfigurationProperties(prefix = "afg.kafka.event")
public class KafkaEventProperties {

    /**
     * 是否启用
     */
    private boolean enabled = true;

    /**
     * 默认 Topic
     */
    private String defaultTopic = "afg-events";

    /**
     * 同步发送超时时间（毫秒）
     */
    private long syncTimeout = 5000;

    /**
     * 是否包含元数据（traceId、eventId 等）
     */
    private boolean includeMetadata = true;
}
