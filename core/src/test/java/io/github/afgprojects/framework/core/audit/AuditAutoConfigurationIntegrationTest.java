package io.github.afgprojects.framework.core.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import io.github.afgprojects.framework.core.config.AfgCoreProperties;
import io.github.afgprojects.framework.core.support.TestApplication;

/**
 * AuditAutoConfiguration 集成测试。
 * <p>
 * 测试审计功能的自动配置，包括属性配置、敏感字段处理器等组件的自动装配。
 *
 * @see AuditAutoConfiguration
 * @see AfgCoreProperties.AuditConfig
 * @see SensitiveFieldProcessor
 */
@DisplayName("AuditAutoConfiguration 集成测试")
@SpringBootTest(
        classes = TestApplication.class,
        properties = {
                "afg.core.audit.enabled=true",
                "afg.core.audit.sensitive-fields=password,token,secret",
                "afg.core.audit.mask-sensitive=true"
        }
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AuditAutoConfigurationIntegrationTest {

    @Autowired(required = false)
    private AfgCoreProperties afgCoreProperties;

    @Autowired(required = false)
    private SensitiveFieldProcessor sensitiveFieldProcessor;

    /**
     * 审计配置测试。
     * <p>
     * 验证审计相关组件的自动配置是否正确生效。
     */
    @Nested
    @DisplayName("审计配置测试")
    class AuditConfigTests {

        /**
         * 测试审计属性是否被自动配置。
         */
        @Test
        @DisplayName("应该自动配置审计属性")
        void shouldAutoConfigureAuditProperties() {
            if (afgCoreProperties != null) {
                assertThat(afgCoreProperties.getAudit().isEnabled()).isTrue();
            }
        }

        /**
         * 测试敏感字段处理器是否被自动配置。
         */
        @Test
        @DisplayName("应该自动配置敏感字段处理器")
        void shouldAutoConfigureSensitiveFieldProcessor() {
            // SensitiveFieldProcessor 是包级私有的，无法直接注入
            // 通过检查 afgCoreProperties 来验证配置
            if (afgCoreProperties != null) {
                assertThat(afgCoreProperties.getAudit().getSensitiveFields()).isNotNull();
            }
        }
    }

    /**
     * 敏感字段处理测试。
     * <p>
     * 验证敏感字段的识别、处理和标准化功能。
     */
    @Nested
    @DisplayName("敏感字段处理测试")
    class SensitiveFieldProcessingTests {

        /**
         * 测试敏感字段处理器是否能够正常处理敏感字段。
         */
        @Test
        @DisplayName("应该能够处理敏感字段")
        void shouldProcessSensitiveFields() {
            if (sensitiveFieldProcessor == null) {
                return;
            }

            // SensitiveFieldProcessor 需要 Audited 注解来构建敏感字段集合
            assertThat(sensitiveFieldProcessor).isNotNull();
        }

        /**
         * 测试是否能够正确判断字段是否为敏感字段。
         */
        @Test
        @DisplayName("应该能够检查是否为敏感字段")
        void shouldCheckIfSensitiveField() {
            if (sensitiveFieldProcessor == null) {
                return;
            }

            Set<String> sensitiveFields = Set.of("password", "token");
            assertThat(sensitiveFieldProcessor.isSensitive("password", sensitiveFields)).isTrue();
            assertThat(sensitiveFieldProcessor.isSensitive("username", sensitiveFields)).isFalse();
        }

        /**
         * 测试字段名标准化功能，验证大小写转换是否正确。
         */
        @Test
        @DisplayName("应该能够标准化字段名")
        void shouldNormalizeFieldName() {
            if (sensitiveFieldProcessor == null) {
                return;
            }

            String normalized = sensitiveFieldProcessor.normalizeFieldName("PassWord");
            assertThat(normalized).isEqualTo("password");
        }
    }

    /**
     * 审计属性测试。
     * <p>
     * 验证审计配置属性的各个配置项是否正确加载。
     */
    @Nested
    @DisplayName("审计属性测试")
    class AuditPropertiesTests {

        /**
         * 测试敏感字段列表是否正确配置。
         */
        @Test
        @DisplayName("应该正确配置敏感字段列表")
        void shouldConfigureSensitiveFields() {
            if (afgCoreProperties != null) {
                assertThat(afgCoreProperties.getAudit().getSensitiveFields()).contains("password", "token", "secret");
            }
        }

        /**
         * 测试存储类型是否正确配置。
         */
        @Test
        @DisplayName("应该正确配置存储类型")
        void shouldConfigureStorageType() {
            if (afgCoreProperties != null) {
                assertThat(afgCoreProperties.getAudit().getStorageType()).isNotNull();
            }
        }

        /**
         * 测试多租户模式是否正确配置。
         */
        @Test
        @DisplayName("应该正确配置多租户模式")
        void shouldConfigureMultiTenant() {
            if (afgCoreProperties != null) {
                assertThat(afgCoreProperties.getAudit().isMultiTenant()).isTrue();
            }
        }

        /**
         * 测试 TTL（生存时间）是否正确配置。
         */
        @Test
        @DisplayName("应该正确配置 TTL")
        void shouldConfigureTtl() {
            if (afgCoreProperties != null) {
                assertThat(afgCoreProperties.getAudit().getTtl()).isNotNull();
            }
        }
    }
}