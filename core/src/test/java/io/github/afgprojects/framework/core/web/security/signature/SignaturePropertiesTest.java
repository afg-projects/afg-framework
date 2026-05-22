package io.github.afgprojects.framework.core.web.security.signature;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.config.AfgCoreProperties;

/**
 * SignatureProperties 测试
 */
@DisplayName("SignatureProperties 测试")
class SignaturePropertiesTest {

    @Nested
    @DisplayName("默认值测试")
    class DefaultValueTests {

        @Test
        @DisplayName("应该有正确的默认值")
        void shouldHaveCorrectDefaultValues() {
            // when
            AfgCoreProperties properties = new AfgCoreProperties();

            // then
            assertThat(properties.getSecurity().getSignature().isEnabled()).isTrue();
            assertThat(properties.getSecurity().getSignature().getDefaultKeyId()).isEqualTo("default");
            assertThat(properties.getSecurity().getSignature().getTimestampTolerance()).isEqualTo(300);
            assertThat(properties.getSecurity().getSignature().getNonceCacheSize()).isEqualTo(10000);
            assertThat(properties.getSecurity().getSignature().isNonceRequired()).isTrue();
            assertThat(properties.getSecurity().getSignature().getDefaultAlgorithm()).isEqualTo(AfgCoreProperties.SecurityConfig.SignatureAlgorithm.HMAC_SHA256);
        }
    }

    @Nested
    @DisplayName("密钥配置测试")
    class KeyConfigTests {

        @Test
        @DisplayName("应该正确获取密钥配置")
        void shouldGetKeyConfig() {
            // given
            AfgCoreProperties properties = new AfgCoreProperties();
            AfgCoreProperties.SecurityConfig.SignatureConfig.SignatureKeyConfig keyConfig = new AfgCoreProperties.SecurityConfig.SignatureConfig.SignatureKeyConfig();
            keyConfig.setSecret("test-secret");
            keyConfig.setEnabled(true);
            properties.getSecurity().getSignature().getKeys().put("app1", keyConfig);

            // when
            AfgCoreProperties.SecurityConfig.SignatureConfig.SignatureKeyConfig retrieved = properties.getSecurity().getSignature().getKeys().get("app1");

            // then
            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getSecret()).isEqualTo("test-secret");
            assertThat(retrieved.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("不存在的密钥应该返回 null")
        void shouldReturnNullForMissingKey() {
            // given
            AfgCoreProperties properties = new AfgCoreProperties();

            // when
            AfgCoreProperties.SecurityConfig.SignatureConfig.SignatureKeyConfig retrieved = properties.getSecurity().getSignature().getKeys().get("nonexistent");

            // then
            assertThat(retrieved).isNull();
        }

        @Test
        @DisplayName("应该正确获取默认密钥配置")
        void shouldGetDefaultKeyConfig() {
            // given
            AfgCoreProperties properties = new AfgCoreProperties();
            properties.getSecurity().getSignature().setDefaultKeyId("default");
            AfgCoreProperties.SecurityConfig.SignatureConfig.SignatureKeyConfig keyConfig = new AfgCoreProperties.SecurityConfig.SignatureConfig.SignatureKeyConfig();
            keyConfig.setSecret("default-secret");
            properties.getSecurity().getSignature().getKeys().put("default", keyConfig);

            // when
            AfgCoreProperties.SecurityConfig.SignatureConfig.SignatureKeyConfig retrieved = properties.getSecurity().getSignature().getKeys().get(properties.getSecurity().getSignature().getDefaultKeyId());

            // then
            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getSecret()).isEqualTo("default-secret");
        }
    }

    @Nested
    @DisplayName("KeyConfig 测试")
    class KeyConfigInnerTests {

        @Test
        @DisplayName("KeyConfig 应该有正确的默认值")
        void keyConfigShouldHaveCorrectDefaults() {
            // when
            AfgCoreProperties.SecurityConfig.SignatureConfig.SignatureKeyConfig config = new AfgCoreProperties.SecurityConfig.SignatureConfig.SignatureKeyConfig();

            // then
            assertThat(config.isEnabled()).isTrue();
            assertThat(config.getDescription()).isNull();
        }
    }
}
