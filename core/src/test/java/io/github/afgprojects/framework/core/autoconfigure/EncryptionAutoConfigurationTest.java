package io.github.afgprojects.framework.core.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.config.AfgCoreProperties;
import io.github.afgprojects.framework.core.config.ConfigEncryptor;

/**
 * EncryptionAutoConfiguration 单元测试。
 * 测试加密自动配置类的 Bean 创建和加密解密功能。
 *
 * @see EncryptionAutoConfiguration
 */
@DisplayName("EncryptionAutoConfiguration 测试")
class EncryptionAutoConfigurationTest {

    private EncryptionAutoConfiguration configuration;

    @BeforeEach
    void setUp() {
        configuration = new EncryptionAutoConfiguration();
    }

    /**
     * ConfigEncryptor 配置测试。
     * 验证 configEncryptor Bean 的创建和异常处理。
     */
    @Nested
    @DisplayName("configEncryptor 配置测试")
    class ConfigEncryptorTests {

        /**
         * 测试使用有效密钥创建加密器。
         */
        @Test
        @DisplayName("应该使用有效密钥创建加密器")
        void shouldCreateEncryptorWithValidKey() {
            AfgCoreProperties properties = new AfgCoreProperties();
            properties.getEncryption().setSecretKey("1234567890123456"); // 16 字节密钥

            ConfigEncryptor encryptor = configuration.configEncryptor(properties);

            assertThat(encryptor).isNotNull();
        }

        /**
         * 测试密钥为空时抛出异常。
         */
        @Test
        @DisplayName("密钥为空时应该抛出异常")
        void shouldThrowExceptionWhenSecretKeyIsNull() {
            AfgCoreProperties properties = new AfgCoreProperties();
            properties.getEncryption().setSecretKey(null);

            assertThatThrownBy(() -> configuration.configEncryptor(properties))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Encryption is enabled but no secret key is configured");
        }

        /**
         * 测试密钥为空白时抛出异常。
         */
        @Test
        @DisplayName("密钥为空白时应该抛出异常")
        void shouldThrowExceptionWhenSecretKeyIsBlank() {
            AfgCoreProperties properties = new AfgCoreProperties();
            properties.getEncryption().setSecretKey("   ");

            assertThatThrownBy(() -> configuration.configEncryptor(properties))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Encryption is enabled but no secret key is configured");
        }

        /**
         * 测试加密和解密功能。
         */
        @Test
        @DisplayName("应该能够加密和解密")
        void shouldEncryptAndDecrypt() {
            AfgCoreProperties properties = new AfgCoreProperties();
            properties.getEncryption().setSecretKey("1234567890123456");

            ConfigEncryptor encryptor = configuration.configEncryptor(properties);

            String original = "test-value";
            String encrypted = encryptor.encrypt(original);
            String decrypted = encryptor.decrypt(encrypted);

            assertThat(decrypted).isEqualTo(original);
        }
    }
}
