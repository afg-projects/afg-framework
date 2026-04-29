package io.github.afgprojects.framework.core.web.version;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * ApiVersionProperties 测试
 */
@DisplayName("ApiVersionProperties 测试")
class ApiVersionPropertiesTest {

    @Nested
    @DisplayName("默认值测试")
    class DefaultValueTests {

        @Test
        @DisplayName("应该有正确的默认值")
        void shouldHaveCorrectDefaultValues() {
            // when
            ApiVersionProperties properties = new ApiVersionProperties();

            // then
            assertThat(properties.isEnabled()).isTrue();
            assertThat(properties.getDefaultVersion()).isEqualTo("1.0.0");
            assertThat(properties.getHeaderName()).isEqualTo("X-API-Version");
            assertThat(properties.getParameterName()).isEqualTo("version");
            assertThat(properties.getUrlPrefix()).isEqualTo("/v");
            assertThat(properties.getResolutionOrder()).containsExactly("HEADER", "URL", "PARAMETER");
        }

        @Test
        @DisplayName("废弃配置应该有正确的默认值")
        void shouldHaveCorrectDeprecationDefaultValues() {
            // when
            ApiVersionProperties properties = new ApiVersionProperties();

            // then
            assertThat(properties.getDeprecation().isEnabled()).isTrue();
            assertThat(properties.getDeprecation().getWarningHeader()).isEqualTo("X-API-Deprecated");
            assertThat(properties.getDeprecation().isLogDeprecation()).isTrue();
        }
    }

    @Nested
    @DisplayName("配置修改测试")
    class ConfigurationModificationTests {

        @Test
        @DisplayName("应该可以修改配置")
        void shouldAllowConfigurationModification() {
            // given
            ApiVersionProperties properties = new ApiVersionProperties();

            // when
            properties.setEnabled(false);
            properties.setDefaultVersion("2.0.0");
            properties.setHeaderName("X-Version");
            properties.setParameterName("v");
            properties.setUrlPrefix("/api/v");
            properties.setResolutionOrder(new String[] {"URL", "HEADER", "PARAMETER"});

            // then
            assertThat(properties.isEnabled()).isFalse();
            assertThat(properties.getDefaultVersion()).isEqualTo("2.0.0");
            assertThat(properties.getHeaderName()).isEqualTo("X-Version");
            assertThat(properties.getParameterName()).isEqualTo("v");
            assertThat(properties.getUrlPrefix()).isEqualTo("/api/v");
            assertThat(properties.getResolutionOrder()).containsExactly("URL", "HEADER", "PARAMETER");
        }

        @Test
        @DisplayName("应该可以修改废弃配置")
        void shouldAllowDeprecationConfigurationModification() {
            // given
            ApiVersionProperties properties = new ApiVersionProperties();

            // when
            properties.getDeprecation().setEnabled(false);
            properties.getDeprecation().setWarningHeader("X-Warn");
            properties.getDeprecation().setLogDeprecation(false);

            // then
            assertThat(properties.getDeprecation().isEnabled()).isFalse();
            assertThat(properties.getDeprecation().getWarningHeader()).isEqualTo("X-Warn");
            assertThat(properties.getDeprecation().isLogDeprecation()).isFalse();
        }
    }
}