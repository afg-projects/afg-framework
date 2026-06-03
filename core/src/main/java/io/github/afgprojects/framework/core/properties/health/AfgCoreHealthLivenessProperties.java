package io.github.afgprojects.framework.core.properties.health;

import java.time.Duration;

import lombok.Data;

/**
 * 存活探针配置。
 */
@Data
public class AfgCoreHealthLivenessProperties {

    private boolean deadlockDetectionEnabled = true;
    private Duration deadlockDetectionTimeout = Duration.ofSeconds(5);
    private boolean memoryCheckEnabled = true;
    private int memoryWarningThreshold = 80;
    private int memoryCriticalThreshold = 95;
}
