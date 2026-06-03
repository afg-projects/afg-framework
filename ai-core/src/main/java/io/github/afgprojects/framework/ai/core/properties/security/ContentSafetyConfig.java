package io.github.afgprojects.framework.ai.core.properties.security;

import lombok.Data;

/**
 * 内容安全配置。
 */
@Data
public class ContentSafetyConfig {

    /**
     * 是否启用内容安全检查。
     */
    private boolean enabled = true;
}
