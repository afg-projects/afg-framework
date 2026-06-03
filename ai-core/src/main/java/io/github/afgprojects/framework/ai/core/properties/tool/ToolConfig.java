package io.github.afgprojects.framework.ai.core.properties.tool;

import lombok.Data;

/**
 * 工具系统配置。
 */
@Data
public class ToolConfig {

    /**
     * 是否启用工具系统。
     */
    private boolean enabled = true;

    /**
     * 是否启用持久化工具注册。
     */
    private boolean persistentEnabled = false;

    /**
     * 工具发现配置。
     */
    private DiscoveryConfig discovery = new DiscoveryConfig();

    /**
     * 工具安全配置。
     */
    private ToolSecurityConfig security = new ToolSecurityConfig();
}
