package io.github.afgprojects.framework.integration.discovery.nacos;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Nacos 工具发现配置属性。
 *
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "afg.ai.tool.discovery.nacos")
public class NacosToolDiscoveryProperties {

    /**
     * 是否启用 Nacos 工具发现。
     */
    private boolean enabled = false;

    /**
     * 工具元数据键。
     */
    private String metadataKey = "ai-tools";

    /**
     * 刷新间隔（秒）。
     */
    private int refreshIntervalSeconds = 30;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getMetadataKey() {
        return metadataKey;
    }

    public void setMetadataKey(String metadataKey) {
        this.metadataKey = metadataKey;
    }

    public int getRefreshIntervalSeconds() {
        return refreshIntervalSeconds;
    }

    public void setRefreshIntervalSeconds(int refreshIntervalSeconds) {
        this.refreshIntervalSeconds = refreshIntervalSeconds;
    }
}
