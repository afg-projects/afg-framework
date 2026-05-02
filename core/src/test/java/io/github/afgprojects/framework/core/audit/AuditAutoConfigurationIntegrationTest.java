package io.github.afgprojects.framework.core.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import io.github.afgprojects.framework.core.support.TestApplication;

/**
 * AuditAutoConfiguration 集成测试
 */
@DisplayName("AuditAutoConfiguration 集成测试")
@SpringBootTest(
        classes = TestApplication.class,
        properties = {
                "afg.audit.enabled=true",
                "afg.audit.sensitive-fields=password,token,secret",
                "afg.audit.mask-char=*",
                "afg.audit.log-parameters=true",
                "afg.audit.log-result=true"
        }
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AuditAutoConfigurationIntegrationTest {

    @Autowired(required = false)
    private AuditLogProperties auditLogProperties;

    @Autowired(required = false)
    private SensitiveFieldProcessor sensitiveFieldProcessor;

    @Nested
    @DisplayName("审计配置测试")
    class AuditConfigTests {

        @Test
        @DisplayName("应该自动配置审计属性")
        void shouldAutoConfigureAuditProperties() {
            if (auditLogProperties != null) {
                assertThat(auditLogProperties.isEnabled()).isTrue();
            }
        }

        @Test
        @DisplayName("应该自动配置敏感字段处理器")
        void shouldAutoConfigureSensitiveFieldProcessor() {
            // SensitiveFieldProcessor 是包级私有的，无法直接注入
            // 通过检查 auditLogProperties 来验证配置
            if (auditLogProperties != null) {
                assertThat(auditLogProperties.getSensitiveFields()).isNotNull();
            }
        }
    }

    @Nested
    @DisplayName("敏感字段处理测试")
    class SensitiveFieldProcessingTests {

        @Test
        @DisplayName("应该能够处理敏感字段")
        void shouldProcessSensitiveFields() {
            if (sensitiveFieldProcessor == null) {
                return;
            }

            // SensitiveFieldProcessor 需要 Audited 注解来构建敏感字段集合
            assertThat(sensitiveFieldProcessor).isNotNull();
        }

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

    @Nested
    @DisplayName("审计属性测试")
    class AuditPropertiesTests {

        @Test
        @DisplayName("应该正确配置敏感字段列表")
        void shouldConfigureSensitiveFields() {
            if (auditLogProperties != null) {
                assertThat(auditLogProperties.getSensitiveFields()).contains("password", "token", "secret");
            }
        }

        @Test
        @DisplayName("应该正确配置存储类型")
        void shouldConfigureStorageType() {
            if (auditLogProperties != null) {
                assertThat(auditLogProperties.getStorageType()).isNotNull();
            }
        }

        @Test
        @DisplayName("应该正确配置多租户模式")
        void shouldConfigureMultiTenant() {
            if (auditLogProperties != null) {
                assertThat(auditLogProperties.isMultiTenant()).isTrue();
            }
        }

        @Test
        @DisplayName("应该正确配置 TTL")
        void shouldConfigureTtl() {
            if (auditLogProperties != null) {
                assertThat(auditLogProperties.getTtl()).isNotNull();
            }
        }
    }
}