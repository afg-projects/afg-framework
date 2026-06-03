package io.github.afgprojects.framework.core.properties.batch;

import lombok.Data;

/**
 * 批量操作限流配置。
 */
@Data
public class AfgCoreBatchRateLimitProperties {

    private boolean enabled = false;
    private int permitsPerSecond = 100;
    private long maxWaitMillis = 5000;
}
