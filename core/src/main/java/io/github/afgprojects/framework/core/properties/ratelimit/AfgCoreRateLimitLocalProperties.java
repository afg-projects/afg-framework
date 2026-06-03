package io.github.afgprojects.framework.core.properties.ratelimit;

import lombok.Data;

/**
 * 本地限流配置。
 */
@Data
public class AfgCoreRateLimitLocalProperties {

    private boolean enabled = false;
    private int cacheSize = 10000;
    private long expireAfterSeconds = 3600;
}
