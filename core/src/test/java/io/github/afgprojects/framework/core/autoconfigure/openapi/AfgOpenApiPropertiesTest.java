package io.github.afgprojects.framework.core.autoconfigure.openapi;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AfgOpenApiProperties 测试")
class AfgOpenApiPropertiesTest {

    @Test
    @DisplayName("默认属性值应正确")
    void defaultValues_shouldBeCorrect() {
        AfgOpenApiProperties properties = new AfgOpenApiProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getTitle()).isEqualTo("AFG Application API");
        assertThat(properties.getDescription()).isEqualTo("AFG Application REST API Documentation");
        assertThat(properties.getVersion()).isEqualTo("1.0.0");
        assertThat(properties.getContactName()).isEqualTo("AFG Team");
        assertThat(properties.getContactEmail()).isEmpty();
        assertThat(properties.getContactUrl()).isEmpty();
        assertThat(properties.getLicense()).isEqualTo("Apache 2.0");
        assertThat(properties.getLicenseUrl()).isEqualTo("https://www.apache.org/licenses/LICENSE-2.0");
    }

    @Test
    @DisplayName("属性设置器应正确更新值")
    void setters_shouldUpdateValues() {
        AfgOpenApiProperties properties = new AfgOpenApiProperties();

        properties.setEnabled(false);
        properties.setTitle("Test API");
        properties.setDescription("Test Description");
        properties.setVersion("2.0.0");
        properties.setContactName("Test Team");
        properties.setContactEmail("test@example.com");
        properties.setContactUrl("https://test.example.com");
        properties.setLicense("MIT");
        properties.setLicenseUrl("https://opensource.org/licenses/MIT");

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getTitle()).isEqualTo("Test API");
        assertThat(properties.getDescription()).isEqualTo("Test Description");
        assertThat(properties.getVersion()).isEqualTo("2.0.0");
        assertThat(properties.getContactName()).isEqualTo("Test Team");
        assertThat(properties.getContactEmail()).isEqualTo("test@example.com");
        assertThat(properties.getContactUrl()).isEqualTo("https://test.example.com");
        assertThat(properties.getLicense()).isEqualTo("MIT");
        assertThat(properties.getLicenseUrl()).isEqualTo("https://opensource.org/licenses/MIT");
    }
}