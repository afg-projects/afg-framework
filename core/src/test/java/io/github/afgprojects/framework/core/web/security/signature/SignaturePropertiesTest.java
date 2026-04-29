package io.github.afgprojects.framework.core.web.security.signature;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
            SignatureProperties properties = new SignatureProperties();

            // then
            assertThat(properties.isEnabled()).isTrue();
            assertThat(properties.getDefaultKeyId()).isEqualTo("default");
            assertThat(properties.getTimestampTolerance()).isEqualTo(300);
            assertThat(properties.getNonceCacheSize()).isEqualTo(10000);
            assertThat(properties.isNonceRequired()).isTrue();
            assertThat(properties.getDefaultAlgorithm()).isEqualTo(SignatureAlgorithm.HMAC_SHA256);
        }
    }

    @Nested
    @DisplayName("密钥配置测试")
    class KeyConfigTests {

        @Test
        @DisplayName("应该正确获取密钥配置")
        void shouldGetKeyConfig() {
            // given
            SignatureProperties properties = new SignatureProperties();
            SignatureProperties.KeyConfig keyConfig = new SignatureProperties.KeyConfig();
            keyConfig.setSecret("test-secret");
            keyConfig.setEnabled(true);
            properties.getKeys().put("app1", keyConfig);

            // when
            SignatureProperties.KeyConfig retrieved = properties.getKeyConfig("app1");

            // then
            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getSecret()).isEqualTo("test-secret");
            assertThat(retrieved.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("不存在的密钥应该返回 null")
        void shouldReturnNullForMissingKey() {
            // given
            SignatureProperties properties = new SignatureProperties();

            // when
            SignatureProperties.KeyConfig retrieved = properties.getKeyConfig("nonexistent");

            // then
            assertThat(retrieved).isNull();
        }

        @Test
        @DisplayName("应该正确获取默认密钥配置")
        void shouldGetDefaultKeyConfig() {
            // given
            SignatureProperties properties = new SignatureProperties();
            properties.setDefaultKeyId("default");
            SignatureProperties.KeyConfig keyConfig = new SignatureProperties.KeyConfig();
            keyConfig.setSecret("default-secret");
            properties.getKeys().put("default", keyConfig);

            // when
            SignatureProperties.KeyConfig retrieved = properties.getDefaultKeyConfig();

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
            SignatureProperties.KeyConfig config = new SignatureProperties.KeyConfig();

            // then
            assertThat(config.isEnabled()).isTrue();
            assertThat(config.getDescription()).isNull();
        }
    }
}
