package io.github.afgprojects.framework.core.properties.openapi;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * OpenAPI 文档配置属性。
 *
 * @since 1.1.0
 */
@Data
@ConfigurationProperties(prefix = "afg.openapi")
public class AfgOpenApiProperties {

    private boolean enabled = true;
    private String title = "AFG Application API";
    private String description = "AFG Application REST API Documentation";
    private String version = "1.0.0";
    private String contactName = "AFG Team";
    private String contactEmail = "";
    private String contactUrl = "";
    private String license = "Apache 2.0";
    private String licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0";
}