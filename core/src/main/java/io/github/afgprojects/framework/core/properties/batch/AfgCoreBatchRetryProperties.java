package io.github.afgprojects.framework.core.properties.batch;

import lombok.Data;

/**
 * 批量操作重试配置。
 */
@Data
public class AfgCoreBatchRetryProperties {

    private boolean enabled = false;
    private int maxAttempts = 3;
    private long initialInterval = 1000;
    private double multiplier = 2.0;
    private long maxInterval = 10000;
}
