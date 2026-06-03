package io.github.afgprojects.framework.core.properties.httpclient;

import lombok.Data;

/**
 * HTTP 熔断器配置。
 */
@Data
public class AfgCoreHttpCircuitBreakerProperties {

    private boolean enabled = true;
    private int failureThreshold = 5;
    private long openDuration = 30000;
    private int halfOpenMaxCalls = 3;
    private int successThreshold = 3;
}
