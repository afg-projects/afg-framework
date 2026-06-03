package io.github.afgprojects.framework.core.properties.feature;

import lombok.Data;

/**
 * 功能开关 Redis 配置。
 */
@Data
public class AfgCoreFeatureRedisProperties {

    private String keyPrefix = "afg:feature:";
    private String flagsMapKey = "flags";
    private String rulesMapKey = "rules";
}
