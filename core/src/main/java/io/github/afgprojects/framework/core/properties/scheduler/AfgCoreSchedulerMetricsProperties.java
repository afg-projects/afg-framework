package io.github.afgprojects.framework.core.properties.scheduler;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;

/**
 * 调度器指标配置。
 */
@Data
public class AfgCoreSchedulerMetricsProperties {

    private boolean enabled = true;
    private String prefix = "afg.scheduler";
    private Map<String, String> tags = new HashMap<>();
    private boolean recordDurationHistogram = true;
}
