package io.github.afgprojects.framework.starter.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AfgOpenApiAutoConfiguration 单元测试
 * <p>
 * 测试场景：
 * <ul>
 *   <li>默认配置下应创建 OpenAPI Bean</li>
 *   <li>配置属性应正确绑定到 AfgOpenApiProperties</li>
 *   <li>当 afg.openapi.enabled=false 时不创建 OpenAPI Bean</li>
 *   <li>当用户自定义 OpenAPI 时自动配置退避</li>
 *   <li>非 Web 环境下不触发自动配置</li>
 * </ul>
 */
@DisplayName("AfgOpenApiAutoConfiguration 测试")
class AfgOpenApiAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AfgOpenApiAutoConfiguration.class));

    @Test
    @DisplayName("默认配置下应创建 OpenAPI Bean")
    void withDefaultConfiguration_shouldCreateOpenApiBean() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(OpenAPI.class);
            assertThat(context).hasSingleBean(AfgOpenApiProperties.class);

            OpenAPI openAPI = context.getBean(OpenAPI.class);
            assertThat(openAPI.getInfo()).isNotNull();
            assertThat(openAPI.getInfo().getTitle()).isEqualTo("AFG Platform API");
            assertThat(openAPI.getInfo().getVersion()).isEqualTo("1.0.0");
        });
    }

    @Test
    @DisplayName("自定义配置属性应正确绑定")
    void withCustomProperties_shouldBindCorrectly() {
        contextRunner
                .withPropertyValues(
                        "afg.openapi.title=Custom API",
                        "afg.openapi.description=Custom API Description",
                        "afg.openapi.version=2.0.0",
                        "afg.openapi.contact-name=Custom Team",
                        "afg.openapi.contact-email=custom@example.com",
                        "afg.openapi.contact-url=https://custom.example.com",
                        "afg.openapi.license=MIT",
                        "afg.openapi.license-url=https://opensource.org/licenses/MIT"
                )
                .run(context -> {
                    AfgOpenApiProperties properties = context.getBean(AfgOpenApiProperties.class);
                    assertThat(properties.getTitle()).isEqualTo("Custom API");
                    assertThat(properties.getDescription()).isEqualTo("Custom API Description");
                    assertThat(properties.getVersion()).isEqualTo("2.0.0");
                    assertThat(properties.getContactName()).isEqualTo("Custom Team");
                    assertThat(properties.getContactEmail()).isEqualTo("custom@example.com");
                    assertThat(properties.getContactUrl()).isEqualTo("https://custom.example.com");
                    assertThat(properties.getLicense()).isEqualTo("MIT");
                    assertThat(properties.getLicenseUrl()).isEqualTo("https://opensource.org/licenses/MIT");

                    OpenAPI openAPI = context.getBean(OpenAPI.class);
                    assertThat(openAPI.getInfo().getTitle()).isEqualTo("Custom API");
                    assertThat(openAPI.getInfo().getVersion()).isEqualTo("2.0.0");
                    assertThat(openAPI.getInfo().getContact().getName()).isEqualTo("Custom Team");
                    assertThat(openAPI.getInfo().getContact().getEmail()).isEqualTo("custom@example.com");
                    assertThat(openAPI.getInfo().getLicense().getName()).isEqualTo("MIT");
                });
    }

    @Test
    @DisplayName("当 afg.openapi.enabled=false 时不创建 OpenAPI Bean")
    void whenDisabled_shouldNotCreateOpenApiBean() {
        contextRunner
                .withPropertyValues("afg.openapi.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(OpenAPI.class);
                    assertThat(context).doesNotHaveBean(AfgOpenApiProperties.class);
                });
    }

    @Test
    @DisplayName("当用户自定义 OpenAPI 时自动配置应退避")
    void whenCustomOpenApiExists_shouldBackOff() {
        contextRunner
                .withBean(OpenAPI.class, () -> new OpenAPI()
                        .info(new io.swagger.v3.oas.models.info.Info()
                                .title("User Custom API")
                                .version("3.0.0")))
                .run(context -> {
                    assertThat(context).hasSingleBean(OpenAPI.class);
                    OpenAPI openAPI = context.getBean(OpenAPI.class);
                    assertThat(openAPI.getInfo().getTitle()).isEqualTo("User Custom API");
                });
    }

    @Test
    @DisplayName("OpenAPI Info 应包含完整的联系信息")
    void openApiInfoShouldContainCompleteContactInfo() {
        contextRunner
                .withPropertyValues(
                        "afg.openapi.contact-name=Test Team",
                        "afg.openapi.contact-email=test@test.com",
                        "afg.openapi.contact-url=https://test.com"
                )
                .run(context -> {
                    OpenAPI openAPI = context.getBean(OpenAPI.class);
                    assertThat(openAPI.getInfo().getContact()).isNotNull();
                    assertThat(openAPI.getInfo().getContact().getName()).isEqualTo("Test Team");
                    assertThat(openAPI.getInfo().getContact().getEmail()).isEqualTo("test@test.com");
                    assertThat(openAPI.getInfo().getContact().getUrl()).isEqualTo("https://test.com");
                });
    }

    @Test
    @DisplayName("OpenAPI Info 应包含完整的许可证信息")
    void openApiInfoShouldContainCompleteLicenseInfo() {
        contextRunner
                .withPropertyValues(
                        "afg.openapi.license=Apache 2.0",
                        "afg.openapi.license-url=https://www.apache.org/licenses/LICENSE-2.0"
                )
                .run(context -> {
                    OpenAPI openAPI = context.getBean(OpenAPI.class);
                    assertThat(openAPI.getInfo().getLicense()).isNotNull();
                    assertThat(openAPI.getInfo().getLicense().getName()).isEqualTo("Apache 2.0");
                    assertThat(openAPI.getInfo().getLicense().getUrl()).isEqualTo("https://www.apache.org/licenses/LICENSE-2.0");
                });
    }

    @Test
    @DisplayName("AfgOpenApiProperties 默认值应正确")
    void afgOpenApiPropertiesDefaultValuesShouldBeCorrect() {
        contextRunner.run(context -> {
            AfgOpenApiProperties properties = context.getBean(AfgOpenApiProperties.class);
            assertThat(properties.isEnabled()).isTrue();
            assertThat(properties.getTitle()).isEqualTo("AFG Platform API");
            assertThat(properties.getDescription()).isEqualTo("AFG Platform REST API Documentation");
            assertThat(properties.getVersion()).isEqualTo("1.0.0");
            assertThat(properties.getContactName()).isEqualTo("AFG Team");
            assertThat(properties.getContactEmail()).isEqualTo("support@afg.io");
            assertThat(properties.getContactUrl()).isEqualTo("https://github.com/afgprojects/afg");
            assertThat(properties.getLicense()).isEqualTo("Apache 2.0");
            assertThat(properties.getLicenseUrl()).isEqualTo("https://www.apache.org/licenses/LICENSE-2.0");
        });
    }
}
