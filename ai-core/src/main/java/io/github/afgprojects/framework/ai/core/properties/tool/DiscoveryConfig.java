package io.github.afgprojects.framework.ai.core.properties.tool;

import lombok.Data;

/**
 * 工具发现配置。
 */
@Data
public class DiscoveryConfig {

    /**
     * 是否启用远程工具发现。
     */
    private boolean enabled = false;
}
