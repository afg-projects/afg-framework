package io.github.afgprojects.framework.core.properties.health;

import lombok.Data;

/**
 * 健康检查配置。
 */
@Data
public class AfgCoreHealthProperties {

    /**
     * 是否启用存活探针。
     */
    private boolean livenessEnabled = true;

    /**
     * 是否启用就绪探针。
     */
    private boolean readinessEnabled = true;

    /**
     * 是否启用深度检查。
     */
    private boolean deepEnabled = true;

    /**
     * 存活探针配置。
     */
    private AfgCoreHealthLivenessProperties liveness = new AfgCoreHealthLivenessProperties();

    /**
     * 就绪探针配置。
     */
    private AfgCoreHealthReadinessProperties readiness = new AfgCoreHealthReadinessProperties();

    /**
     * 深度检查配置。
     */
    private AfgCoreHealthDeepProperties deep = new AfgCoreHealthDeepProperties();

    /**
     * 数据源健康检查配置。
     */
    private AfgCoreHealthDataSourceProperties datasource = new AfgCoreHealthDataSourceProperties();
}
