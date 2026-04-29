package io.github.afgprojects.framework.starter.openapi;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * OpenAPI 配置属性
 * <p>
 * 用于配置 API 文档的基本信息。
 *
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "afg.openapi")
public class AfgOpenApiProperties {

    /**
     * 是否启用 OpenAPI 配置
     */
    private boolean enabled = true;

    /**
     * API 文档标题
     */
    private String title = "AFG Platform API";

    /**
     * API 文档描述
     */
    private String description = "AFG Platform REST API Documentation";

    /**
     * API 版本
     */
    private String version = "1.0.0";

    /**
     * 联系人名称
     */
    private String contactName = "AFG Team";

    /**
     * 联系人邮箱
     */
    private String contactEmail = "support@afg.io";

    /**
     * 联系人 URL
     */
    private String contactUrl = "https://github.com/afgprojects/afg";

    /**
     * 许可证名称
     */
    private String license = "Apache 2.0";

    /**
     * 许可证 URL
     */
    private String licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0";
}
