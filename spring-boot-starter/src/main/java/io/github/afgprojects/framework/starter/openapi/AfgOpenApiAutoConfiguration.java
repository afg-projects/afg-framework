package io.github.afgprojects.framework.starter.openapi;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

/**
 * SpringDoc OpenAPI 自动配置类
 * <p>
 * 提供 REST API 文档自动生成功能：
 * <ul>
 *   <li>Swagger UI 界面 - 可通过 /swagger-ui.html 访问</li>
 *   <li>OpenAPI JSON - 可通过 /v3/api-docs 访问</li>
 *   <li>OpenAPI YAML - 可通过 /v3/api-docs.yaml 访问</li>
 * </ul>
 * <p>
 * 配置项（application.yml）：
 * <pre>
 * springdoc:
 *   api-docs:
 *     enabled: true                    # 启用 API 文档
 *     path: /v3/api-docs               # API 文档路径
 *   swagger-ui:
 *     enabled: true                    # 启用 Swagger UI
 *     path: /swagger-ui.html           # Swagger UI 路径
 *     tags-sorter: alpha               # 标签排序方式
 *     operations-sorter: alpha         # 操作排序方式
 * </pre>
 * <p>
 * AFG 扩展配置：
 * <pre>
 * afg:
 *   openapi:
 *     enabled: true                    # 启用 OpenAPI 配置
 *     title: AFG Platform API          # API 标题
 *     description: AFG Platform API 文档 # API 描述
 *     version: 1.0.0                   # API 版本
 *     contact-name: AFG Team           # 联系人名称
 *     contact-email: support@afg.io    # 联系人邮箱
 *     license: Apache 2.0              # 许可证
 *     license-url: https://www.apache.org/licenses/LICENSE-2.0
 * </pre>
 *
 * @since 1.0.0
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(OpenAPI.class)
@ConditionalOnProperty(prefix = "afg.openapi", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(AfgOpenApiProperties.class)
public class AfgOpenApiAutoConfiguration {

    /**
     * 配置 OpenAPI 基本信息
     *
     * @param properties OpenAPI 配置属性
     * @return OpenAPI 配置
     */
    @Bean
    public OpenAPI afgOpenApi(AfgOpenApiProperties properties) {
        return new OpenAPI()
                .info(new Info()
                        .title(properties.getTitle())
                        .description(properties.getDescription())
                        .version(properties.getVersion())
                        .contact(new Contact()
                                .name(properties.getContactName())
                                .email(properties.getContactEmail())
                                .url(properties.getContactUrl()))
                        .license(new License()
                                .name(properties.getLicense())
                                .url(properties.getLicenseUrl())));
    }
}
