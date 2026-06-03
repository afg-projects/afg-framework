package io.github.afgprojects.framework.core.properties.event;

import lombok.Data;

/**
 * 事件重试配置。
 */
@Data
public class AfgCoreEventRetryProperties {

    private boolean enabled = true;
    private int maxAttempts = 3;
    private long initialInterval = 1000;
    private double multiplier = 2.0;
    private long maxInterval = 30000;
}
