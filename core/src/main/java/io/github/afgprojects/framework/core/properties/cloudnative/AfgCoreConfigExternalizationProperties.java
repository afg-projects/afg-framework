package io.github.afgprojects.framework.core.properties.cloudnative;

import java.time.Duration;

import org.jspecify.annotations.Nullable;

import lombok.Data;

/**
 * 配置外部化配置。
 */
@Data
public class AfgCoreConfigExternalizationProperties {

    private boolean enabled = true;
    private @Nullable String configMap;
    private @Nullable String secret;
    private boolean autoRefresh = true;
    private Duration refreshInterval = Duration.ofMinutes(1);
}
