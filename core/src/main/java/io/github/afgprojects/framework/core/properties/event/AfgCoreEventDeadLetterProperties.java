package io.github.afgprojects.framework.core.properties.event;

import lombok.Data;

/**
 * 死信队列配置。
 */
@Data
public class AfgCoreEventDeadLetterProperties {

    private boolean enabled = true;
    private String topicPrefix = "dlq.";
    private long retentionMs;
}
