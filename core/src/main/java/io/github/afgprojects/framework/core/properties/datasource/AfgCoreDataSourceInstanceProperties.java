package io.github.afgprojects.framework.core.properties.datasource;

import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import lombok.Data;

/**
 * 数据源实例配置。
 */
@Data
public class AfgCoreDataSourceInstanceProperties {

    private @Nullable String url;
    private @Nullable String username;
    private @Nullable String password;
    private @Nullable String driverClassName;
    private boolean lazyInit = false;
    private Map<String, Object> poolConfig = new HashMap<>();
}
