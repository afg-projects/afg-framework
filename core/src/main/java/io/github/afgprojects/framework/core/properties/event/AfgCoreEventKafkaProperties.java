package io.github.afgprojects.framework.core.properties.event;

import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import lombok.Data;

/**
 * Kafka 事件配置。
 */
@Data
public class AfgCoreEventKafkaProperties {

    private @Nullable String bootstrapServers;
    private Map<String, Object> producer = new HashMap<>();
    private Map<String, Object> consumer = new HashMap<>();
    private boolean autoCreateTopics = true;
    private int partitions = 3;
    private short replicationFactor = 1;
}
