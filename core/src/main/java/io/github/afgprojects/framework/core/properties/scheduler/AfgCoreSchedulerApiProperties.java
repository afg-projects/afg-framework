package io.github.afgprojects.framework.core.properties.scheduler;

import lombok.Data;

/**
 * 调度器管理 API 配置。
 */
@Data
public class AfgCoreSchedulerApiProperties {

    private boolean enabled = false;
    private String basePath = "/afg/scheduler";
}
