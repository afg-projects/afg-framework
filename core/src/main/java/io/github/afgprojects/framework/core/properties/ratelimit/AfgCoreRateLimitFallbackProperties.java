package io.github.afgprojects.framework.core.properties.ratelimit;

import lombok.Data;

/**
 * 限流回退配置。
 */
@Data
public class AfgCoreRateLimitFallbackProperties {

    private boolean enabled = true;
    private String defaultMessage = "请求过于频繁，请稍后再试";
    private FailureMode failureMode = FailureMode.ALLOW;
}
