package io.github.afgprojects.framework.core.properties.scheduler;

import java.time.Duration;

import lombok.Data;

/**
 * 动态任务配置。
 */
@Data
public class AfgCoreSchedulerDynamicTaskProperties {

    private boolean enabled = false;
    private String sourceType = "config-center";
    private Duration refreshInterval = Duration.ofMinutes(1);
    private String configPrefix = "afg.tasks";
}
