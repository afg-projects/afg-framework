package io.github.afgprojects.framework.core.web.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.afgprojects.framework.core.config.AfgCoreProperties;

/**
 * ValidationAutoConfiguration 相关测试。
 * <p>
 * AutoConfiguration 的条件注册逻辑由 Spring Boot 测试框架验证，
 * 此处验证 Properties 默认值和自定义配置。
 */
@DisplayName("ValidationAutoConfiguration")
class ValidationAutoConfigurationTest {

    @Nested
    @DisplayName("ValidationConfig 默认值")
    class PropertyDefaults {

        @Test
        @DisplayName("应该默认启用 Bean Validation")
        void shouldDefaultEnabled() {
            AfgCoreProperties properties = new AfgCoreProperties();
            assertThat(properties.getValidation().isEnabled()).isTrue();
        }

        @Test
        @DisplayName("应该默认包含字段错误详情")
        void shouldDefaultIncludeFieldErrors() {
            AfgCoreProperties properties = new AfgCoreProperties();
            assertThat(properties.getValidation().isIncludeFieldErrors()).isTrue();
        }

        @Test
        @DisplayName("应该有默认错误消息")
        void shouldHaveDefaultErrorMessage() {
            AfgCoreProperties properties = new AfgCoreProperties();
            assertThat(properties.getValidation().getDefaultErrorMessage()).isEqualTo("参数校验失败");
        }
    }

    @Nested
    @DisplayName("ValidationConfig 自定义")
    class PropertyCustomization {

        @Test
        @DisplayName("应该允许禁用 Bean Validation")
        void shouldAllowDisabling() {
            AfgCoreProperties properties = new AfgCoreProperties();
            properties.getValidation().setEnabled(false);
            assertThat(properties.getValidation().isEnabled()).isFalse();
        }

        @Test
        @DisplayName("应该允许自定义默认错误消息")
        void shouldAllowCustomErrorMessage() {
            AfgCoreProperties properties = new AfgCoreProperties();
            properties.getValidation().setDefaultErrorMessage("Validation failed");
            assertThat(properties.getValidation().getDefaultErrorMessage()).isEqualTo("Validation failed");
        }

        @Test
        @DisplayName("应该允许禁用字段错误详情")
        void shouldAllowDisablingFieldErrors() {
            AfgCoreProperties properties = new AfgCoreProperties();
            properties.getValidation().setIncludeFieldErrors(false);
            assertThat(properties.getValidation().isIncludeFieldErrors()).isFalse();
        }
    }
}
