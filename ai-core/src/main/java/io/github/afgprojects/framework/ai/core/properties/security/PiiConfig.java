package io.github.afgprojects.framework.ai.core.properties.security;

import lombok.Data;

/**
 * PII 检测配置。
 */
@Data
public class PiiConfig {

    /**
     * 是否启用 PII 检测。
     */
    private boolean enabled = true;
}
