package io.github.afgprojects.framework.core.properties.logging;

import lombok.Data;

/**
 * 异步日志配置。
 */
@Data
public class AfgCoreLoggingAsyncProperties {

    private int queueSize = 512;
    private int discardingThreshold = 0;
    private boolean includeCallerData = true;
    private boolean neverBlock = true;
}
