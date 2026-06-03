package io.github.afgprojects.framework.core.properties.datasource;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;

/**
 * 数据源配置。
 */
@Data
public class AfgCoreDataSourceProperties {

    /**
     * 是否启用多数据源。
     */
    private boolean enabled = false;

    /**
     * 主数据源名称。
     */
    private String primary = "master";

    /**
     * 严格模式。
     */
    private boolean strict = false;

    /**
     * 数据源配置映射。
     */
    private Map<String, AfgCoreDataSourceInstanceProperties> datasources = new HashMap<>();

    /**
     * 读写分离配置。
     */
    private AfgCoreReadWriteSeparationProperties readWriteSeparation = new AfgCoreReadWriteSeparationProperties();
}
