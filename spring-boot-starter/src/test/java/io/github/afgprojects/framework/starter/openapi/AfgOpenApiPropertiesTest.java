package io.github.afgprojects.framework.starter.openapi;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.ConfigurationProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AfgOpenApiProperties 单元测试
 * <p>
 * 测试配置属性的默认值和 setter 方法
 */
@DisplayName("AfgOpenApiProperties 测试")
class AfgOpenApiPropertiesTest {

    @Test
    @DisplayName("默认构造函数应设置正确的默认值")
    void defaultConstructorShouldSetCorrectDefaultValues() {
        AfgOpenApiProperties properties = new AfgOpenApiProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getTitle()).isEqualTo("AFG Platform API");
        assertThat(properties.getDescription()).isEqualTo("AFG Platform REST API Documentation");
        assertThat(properties.getVersion()).isEqualTo("1.0.0");
        assertThat(properties.getContactName()).isEqualTo("AFG Team");
        assertThat(properties.getContactEmail()).isEqualTo("support@afg.io");
        assertThat(properties.getContactUrl()).isEqualTo("https://github.com/afgprojects/afg");
        assertThat(properties.getLicense()).isEqualTo("Apache 2.0");
        assertThat(properties.getLicenseUrl()).isEqualTo("https://www.apache.org/licenses/LICENSE-2.0");
    }

    @Test
    @DisplayName("setter 方法应正确更新属性值")
    void setterMethodsShouldUpdatePropertyValues() {
        AfgOpenApiProperties properties = new AfgOpenApiProperties();

        properties.setEnabled(false);
        assertThat(properties.isEnabled()).isFalse();

        properties.setTitle("Test API");
        assertThat(properties.getTitle()).isEqualTo("Test API");

        properties.setDescription("Test Description");
        assertThat(properties.getDescription()).isEqualTo("Test Description");

        properties.setVersion("2.0.0");
        assertThat(properties.getVersion()).isEqualTo("2.0.0");

        properties.setContactName("Test Team");
        assertThat(properties.getContactName()).isEqualTo("Test Team");

        properties.setContactEmail("test@test.com");
        assertThat(properties.getContactEmail()).isEqualTo("test@test.com");

        properties.setContactUrl("https://test.com");
        assertThat(properties.getContactUrl()).isEqualTo("https://test.com");

        properties.setLicense("MIT");
        assertThat(properties.getLicense()).isEqualTo("MIT");

        properties.setLicenseUrl("https://opensource.org/licenses/MIT");
        assertThat(properties.getLicenseUrl()).isEqualTo("https://opensource.org/licenses/MIT");
    }

    @Test
    @DisplayName("配置前缀应为 afg.openapi")
    void configurationPropertiesPrefixShouldBeCorrect() {
        ConfigurationProperties annotation = AfgOpenApiProperties.class.getAnnotation(ConfigurationProperties.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.prefix()).isEqualTo("afg.openapi");
    }
}
