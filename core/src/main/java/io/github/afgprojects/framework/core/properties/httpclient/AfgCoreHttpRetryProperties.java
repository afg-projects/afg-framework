package io.github.afgprojects.framework.core.properties.httpclient;

import java.util.Set;

import lombok.Data;

/**
 * HTTP 重试配置。
 */
@Data
public class AfgCoreHttpRetryProperties {

    private boolean enabled = true;
    private int maxAttempts = 3;
    private long initialInterval = 1000;
    private double multiplier = 2.0;
    private long maxInterval = 10000;
    private Set<Integer> retryOnStatus = Set.of(502, 503, 504);
}
