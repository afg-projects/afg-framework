package io.github.afgprojects.framework.ai.core.properties.resilience;

import lombok.Data;

/**
 * 韧性配置。
 */
@Data
public class ResilienceConfig {

    /**
     * 是否启用韧性机制。
     */
    private boolean enabled = true;

    /**
     * 重试配置。
     */
    private RetryConfig retry = new RetryConfig();

    /**
     * 熔断器配置。
     */
    private CircuitBreakerConfig circuitBreaker = new CircuitBreakerConfig();
}
