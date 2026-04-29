package io.github.afgprojects.framework.core.web.version;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * ApiVersionRequestMappingHandlerMapping 测试
 */
@DisplayName("ApiVersionRequestMappingHandlerMapping 测试")
class ApiVersionRequestMappingHandlerMappingTest {

    private ApiVersionProperties properties;
    private ApiVersionRequestMappingHandlerMapping handlerMapping;

    @BeforeEach
    void setUp() {
        properties = new ApiVersionProperties();
        handlerMapping = new ApiVersionRequestMappingHandlerMapping(properties);
    }

    @Nested
    @DisplayName("构造方法测试")
    class ConstructorTests {

        @Test
        @DisplayName("应该正确初始化")
        void shouldInitializeCorrectly() {
            // then
            assertThat(handlerMapping).isNotNull();
        }
    }

    @Nested
    @DisplayName("配置测试")
    class ConfigurationTests {

        @Test
        @DisplayName("应该使用配置的默认值")
        void shouldUseConfiguredDefaultValues() {
            // then
            assertThat(properties.getDefaultVersion()).isEqualTo("1.0.0");
            assertThat(properties.getUrlPrefix()).isEqualTo("/v");
        }

        @Test
        @DisplayName("应该使用自定义配置")
        void shouldUseCustomConfiguration() {
            // given
            properties.setDefaultVersion("2.0.0");
            properties.setUrlPrefix("/api/v");
            ApiVersionRequestMappingHandlerMapping customMapping =
                    new ApiVersionRequestMappingHandlerMapping(properties);

            // then
            assertThat(customMapping).isNotNull();
            assertThat(properties.getDefaultVersion()).isEqualTo("2.0.0");
            assertThat(properties.getUrlPrefix()).isEqualTo("/api/v");
        }
    }

    @Nested
    @DisplayName("Order 测试")
    class OrderTests {

        @Test
        @DisplayName("应该有正确的顺序")
        void shouldHaveCorrectOrder() {
            // when
            int order = handlerMapping.getOrder();

            // then
            // RequestMappingHandlerMapping 默认 order 为 0，但版本映射可能需要更高优先级
            assertThat(order).isGreaterThanOrEqualTo(0);
        }
    }
}