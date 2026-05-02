package io.github.afgprojects.framework.core.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.config.ConfigEncryptor;

/**
 * EncryptionAutoConfiguration 测试
 */
@DisplayName("EncryptionAutoConfiguration 测试")
class EncryptionAutoConfigurationTest {

    private EncryptionAutoConfiguration configuration;

    @BeforeEach
    void setUp() {
        configuration = new EncryptionAutoConfiguration();
    }

    @Nested
    @DisplayName("configEncryptor 配置测试")
    class ConfigEncryptorTests {

        @Test
        @DisplayName("应该使用有效密钥创建加密器")
        void shouldCreateEncryptorWithValidKey() {
            EncryptionProperties properties = new EncryptionProperties();
            properties.setSecretKey("1234567890123456"); // 16 字节密钥

            ConfigEncryptor encryptor = configuration.configEncryptor(properties);

            assertThat(encryptor).isNotNull();
        }

        @Test
        @DisplayName("密钥为空时应该抛出异常")
        void shouldThrowExceptionWhenSecretKeyIsNull() {
            EncryptionProperties properties = new EncryptionProperties();
            properties.setSecretKey(null);

            assertThatThrownBy(() -> configuration.configEncryptor(properties))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Encryption is enabled but no secret key is configured");
        }

        @Test
        @DisplayName("密钥为空白时应该抛出异常")
        void shouldThrowExceptionWhenSecretKeyIsBlank() {
            EncryptionProperties properties = new EncryptionProperties();
            properties.setSecretKey("   ");

            assertThatThrownBy(() -> configuration.configEncryptor(properties))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Encryption is enabled but no secret key is configured");
        }

        @Test
        @DisplayName("应该能够加密和解密")
        void shouldEncryptAndDecrypt() {
            EncryptionProperties properties = new EncryptionProperties();
            properties.setSecretKey("1234567890123456");

            ConfigEncryptor encryptor = configuration.configEncryptor(properties);

            String original = "test-value";
            String encrypted = encryptor.encrypt(original);
            String decrypted = encryptor.decrypt(encrypted);

            assertThat(decrypted).isEqualTo(original);
        }
    }
}
