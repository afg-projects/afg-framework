package io.github.afgprojects.framework.core.properties.ratelimit;

import lombok.Data;

/**
 * 限流响应头配置。
 */
@Data
public class AfgCoreRateLimitResponseHeadersProperties {

    private boolean enabled = true;
    private String limitHeader = "X-RateLimit-Limit";
    private String remainingHeader = "X-RateLimit-Remaining";
    private String resetHeader = "X-RateLimit-Reset";
    private String retryAfterHeader = "Retry-After";
}
