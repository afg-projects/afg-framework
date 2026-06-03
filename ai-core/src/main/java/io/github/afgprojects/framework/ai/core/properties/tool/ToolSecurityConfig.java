package io.github.afgprojects.framework.ai.core.properties.tool;

import lombok.Data;

/**
 * 工具安全配置。
 */
@Data
public class ToolSecurityConfig {

    /**
     * 是否启用工具安全校验。
     */
    private boolean enabled = false;
}
