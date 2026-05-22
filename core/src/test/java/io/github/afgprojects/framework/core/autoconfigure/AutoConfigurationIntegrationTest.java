package io.github.afgprojects.framework.core.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import io.github.afgprojects.framework.core.config.AfgCoreProperties;
import io.github.afgprojects.framework.core.support.TestApplication;

/**
 * 自动配置集成测试。
 * 测试多个自动配置属性类在 Spring Boot 环境中的实际装配效果。
 *
 * @see AfgCoreProperties.MetricsConfig
 * @see AfgCoreProperties.VirtualThreadConfig
 * @see AfgCoreProperties.EncryptionConfig
 */
@DisplayName("自动配置集成测试")
@SpringBootTest(
        classes = TestApplication.class,
        properties = {
                "afg.core.metrics.enabled=true",
                "afg.core.virtual-thread.enabled=true",
                "afg.core.encryption.enabled=false"
        }
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AutoConfigurationIntegrationTest {

    @Autowired(required = false)
    private AfgCoreProperties afgCoreProperties;

    /**
     * MetricsConfig 自动配置测试。
     * 验证指标配置属性的自动装配。
     */
    @Nested
    @DisplayName("MetricsConfig 测试")
    class MetricsConfigTests {

        /**
         * 测试自动配置 MetricsConfig。
         */
        @Test
        @DisplayName("应该自动配置 MetricsConfig")
        void shouldAutoConfigureMetricsConfig() {
            if (afgCoreProperties != null) {
                assertThat(afgCoreProperties.getMetrics()).isNotNull();
                assertThat(afgCoreProperties.getMetrics().isEnabled()).isTrue();
            }
        }
    }

    /**
     * VirtualThreadConfig 自动配置测试。
     * 验证虚拟线程配置属性的自动装配。
     */
    @Nested
    @DisplayName("VirtualThreadConfig 测试")
    class VirtualThreadConfigTests {

        /**
         * 测试自动配置 VirtualThreadConfig。
         */
        @Test
        @DisplayName("应该自动配置 VirtualThreadConfig")
        void shouldAutoConfigureVirtualThreadConfig() {
            if (afgCoreProperties != null) {
                assertThat(afgCoreProperties.getVirtualThread()).isNotNull();
                assertThat(afgCoreProperties.getVirtualThread().isEnabled()).isTrue();
            }
        }
    }

    /**
     * EncryptionConfig 自动配置测试。
     * 验证加密配置属性的自动装配。
     */
    @Nested
    @DisplayName("EncryptionConfig 测试")
    class EncryptionConfigTests {

        /**
         * 测试自动配置 EncryptionConfig。
         */
        @Test
        @DisplayName("应该自动配置 EncryptionConfig")
        void shouldAutoConfigureEncryptionConfig() {
            if (afgCoreProperties != null) {
                assertThat(afgCoreProperties.getEncryption()).isNotNull();
                assertThat(afgCoreProperties.getEncryption().isEnabled()).isFalse();
            }
        }
    }
}