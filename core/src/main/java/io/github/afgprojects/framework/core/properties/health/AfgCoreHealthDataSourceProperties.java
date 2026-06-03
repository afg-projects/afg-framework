package io.github.afgprojects.framework.core.properties.health;

import lombok.Data;

/**
 * 数据源健康检查配置。
 */
@Data
public class AfgCoreHealthDataSourceProperties {

    private boolean enabled = true;
    private String validationQuery = "SELECT 1";
    private int poolUsageWarningThreshold = 70;
    private int poolUsageCriticalThreshold = 90;
    private int threadsAwaitingWarningThreshold = 5;
    private int threadsAwaitingCriticalThreshold = 10;
    private int connectionTimeout = 3000;
}
