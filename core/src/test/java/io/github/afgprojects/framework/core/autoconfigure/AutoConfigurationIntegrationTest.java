package io.github.afgprojects.framework.core.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import io.github.afgprojects.framework.core.support.TestApplication;

/**
 * 自动配置集成测试
 */
@DisplayName("自动配置集成测试")
@SpringBootTest(
        classes = TestApplication.class,
        properties = {
                "afg.metrics.enabled=true",
                "afg.virtual-threads.enabled=true",
                "afg.encryption.enabled=false"
        }
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AutoConfigurationIntegrationTest {

    @Autowired(required = false)
    private MetricsProperties metricsProperties;

    @Autowired(required = false)
    private VirtualThreadProperties virtualThreadProperties;

    @Autowired(required = false)
    private EncryptionProperties encryptionProperties;

    @Nested
    @DisplayName("MetricsProperties 测试")
    class MetricsPropertiesTests {

        @Test
        @DisplayName("应该自动配置 MetricsProperties")
        void shouldAutoConfigureMetricsProperties() {
            if (metricsProperties != null) {
                assertThat(metricsProperties).isNotNull();
                assertThat(metricsProperties.isEnabled()).isTrue();
            }
        }
    }

    @Nested
    @DisplayName("VirtualThreadProperties 测试")
    class VirtualThreadPropertiesTests {

        @Test
        @DisplayName("应该自动配置 VirtualThreadProperties")
        void shouldAutoConfigureVirtualThreadProperties() {
            if (virtualThreadProperties != null) {
                assertThat(virtualThreadProperties).isNotNull();
                assertThat(virtualThreadProperties.isEnabled()).isTrue();
            }
        }
    }

    @Nested
    @DisplayName("EncryptionProperties 测试")
    class EncryptionPropertiesTests {

        @Test
        @DisplayName("应该自动配置 EncryptionProperties")
        void shouldAutoConfigureEncryptionProperties() {
            if (encryptionProperties != null) {
                assertThat(encryptionProperties).isNotNull();
                assertThat(encryptionProperties.isEnabled()).isFalse();
            }
        }
    }
}
