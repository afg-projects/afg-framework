package io.github.afgprojects.framework.core.properties.cloudnative;

import java.time.Duration;

import lombok.Data;

/**
 * 探针详细配置。
 */
@Data
public class AfgCoreProbeDetailProperties {

    private String path = "/health/default";
    private Duration initialDelay = Duration.ofSeconds(10);
    private Duration period = Duration.ofSeconds(10);
    private Duration timeout = Duration.ofSeconds(5);
    private int successThreshold = 1;
    private int failureThreshold = 3;
}
