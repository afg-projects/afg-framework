package io.github.afgprojects.framework.core.properties.scheduler;

import java.time.Duration;

import lombok.Data;

/**
 * 调度器日志存储配置。
 */
@Data
public class AfgCoreSchedulerLogStorageProperties {

    private String type = "memory";
    private int maxSize = 10000;
    private Duration retention = Duration.ofDays(7);
    private boolean logSuccess = true;
    private boolean logErrorStack = true;
}
