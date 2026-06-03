package io.github.afgprojects.framework.ai.core.properties.security;

import lombok.Data;

/**
 * 安全配置。
 */
@Data
public class SecurityConfig {

    /**
     * 是否启用安全功能。
     */
    private boolean enabled = true;

    /**
     * API Key 配置。
     */
    private ApiKeyConfig apiKey = new ApiKeyConfig();

    /**
     * 内容安全配置。
     */
    private ContentSafetyConfig contentSafety = new ContentSafetyConfig();

    /**
     * PII 检测配置。
     */
    private PiiConfig pii = new PiiConfig();
}
