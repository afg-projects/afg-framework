package io.github.afgprojects.framework.ai.core.properties.security;

import lombok.Data;

/**
 * API Key 配置。
 */
@Data
public class ApiKeyConfig {

    /**
     * API Key 存储前缀。
     */
    private String keyPrefix = "afg:ai:apikey";
}
