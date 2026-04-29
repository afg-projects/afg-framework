package io.github.afgprojects.framework.core.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * AesConfigEncryptor 测试
 */
@DisplayName("AesConfigEncryptor 测试")
class AesConfigEncryptorTest {

    @Nested
    @DisplayName("构造函数测试")
    class ConstructorTests {

        @Test
        @DisplayName("应该使用 16 字节密钥创建加密器")
        void shouldCreateWith16ByteKey() {
            // when
            AesConfigEncryptor encryptor = new AesConfigEncryptor("1234567890123456");

            // then
            assertThat(encryptor).isNotNull();
        }

        @Test
        @DisplayName("应该使用 24 字节密钥创建加密器")
        void shouldCreateWith24ByteKey() {
            // when
            AesConfigEncryptor encryptor = new AesConfigEncryptor("123456789012345678901234");

            // then
            assertThat(encryptor).isNotNull();
        }

        @Test
        @DisplayName("应该使用 32 字节密钥创建加密器")
        void shouldCreateWith32ByteKey() {
            // when
            AesConfigEncryptor encryptor = new AesConfigEncryptor("12345678901234567890123456789012");

            // then
            assertThat(encryptor).isNotNull();
        }

        @Test
        @DisplayName("无效密钥长度应该抛出异常")
        void shouldThrowOnInvalidKeyLength() {
            // when & then
            assertThatThrownBy(() -> new AesConfigEncryptor("short"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("16, 24, or 32 bytes");
        }

        @Test
        @DisplayName("null 密钥应该抛出异常")
        void shouldThrowOnNullKey() {
            // when & then
            assertThatThrownBy(() -> new AesConfigEncryptor(null)).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("加密解密测试")
    class EncryptDecryptTests {

        private final AesConfigEncryptor encryptor = new AesConfigEncryptor("1234567890123456");

        @Test
        @DisplayName("应该正确加密和解密字符串")
        void shouldEncryptAndDecrypt() {
            // given
            String plaintext = "secret-value";

            // when
            String encrypted = encryptor.encrypt(plaintext);
            String decrypted = encryptor.decrypt(encrypted);

            // then
            assertThat(decrypted).isEqualTo(plaintext);
        }

        @Test
        @DisplayName("加密结果应该包含前缀和后缀")
        void shouldContainPrefixAndSuffix() {
            // given
            String plaintext = "secret";

            // when
            String encrypted = encryptor.encrypt(plaintext);

            // then
            assertThat(encrypted).startsWith("ENC(");
            assertThat(encrypted).endsWith(")");
        }

        @Test
        @DisplayName("相同明文每次加密结果应该不同（随机 IV）")
        void shouldProduceDifferentEncryptionsForSamePlaintext() {
            // given
            String plaintext = "secret";

            // when
            String encrypted1 = encryptor.encrypt(plaintext);
            String encrypted2 = encryptor.encrypt(plaintext);

            // then
            assertThat(encrypted1).isNotEqualTo(encrypted2);
        }

        @Test
        @DisplayName("应该加密空字符串")
        void shouldEncryptEmptyString() {
            // given
            String plaintext = "";

            // when
            String encrypted = encryptor.encrypt(plaintext);
            String decrypted = encryptor.decrypt(encrypted);

            // then
            assertThat(decrypted).isEqualTo(plaintext);
        }

        @Test
        @DisplayName("应该加密包含特殊字符的字符串")
        void shouldEncryptSpecialCharacters() {
            // given
            String plaintext = "password@123!#$%^&*()";

            // when
            String encrypted = encryptor.encrypt(plaintext);
            String decrypted = encryptor.decrypt(encrypted);

            // then
            assertThat(decrypted).isEqualTo(plaintext);
        }

        @Test
        @DisplayName("应该加密 Unicode 字符")
        void shouldEncryptUnicodeCharacters() {
            // given
            String plaintext = "密码测试123";

            // when
            String encrypted = encryptor.encrypt(plaintext);
            String decrypted = encryptor.decrypt(encrypted);

            // then
            assertThat(decrypted).isEqualTo(plaintext);
        }

        @Test
        @DisplayName("null 明文应该抛出异常")
        void shouldThrowOnNullPlaintext() {
            // JSpecify @NonNull doesn't enforce at runtime, but the implementation
            // catches NullPointerException and wraps it in RuntimeException
            // when & then
            assertThatThrownBy(() -> encryptor.encrypt(null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Encryption failed");
        }

        @Test
        @DisplayName("null 密文应该抛出异常或产生意外行为")
        void shouldThrowOnNullCiphertext() {
            // JSpecify @NonNull doesn't enforce at runtime, so we test behavior
            // when & then
            assertThatThrownBy(() -> encryptor.decrypt(null))
                    .isInstanceOfAny(NullPointerException.class, IllegalArgumentException.class);
        }

        @Test
        @DisplayName("无效密文格式应该抛出异常")
        void shouldThrowOnInvalidCiphertextFormat() {
            // when & then
            assertThatThrownBy(() -> encryptor.decrypt("invalid"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Decryption failed");
        }

        @Test
        @DisplayName("错误的密钥应该无法解密")
        void shouldNotDecryptWithWrongKey() {
            // given
            AesConfigEncryptor wrongEncryptor = new AesConfigEncryptor("0987654321098765");
            String encrypted = encryptor.encrypt("secret");

            // when & then
            assertThatThrownBy(() -> wrongEncryptor.decrypt(encrypted)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("前缀后缀测试")
    class PrefixSuffixTests {

        private final AesConfigEncryptor encryptor = new AesConfigEncryptor("1234567890123456");

        @Test
        @DisplayName("应该返回正确的前缀")
        void shouldReturnCorrectPrefix() {
            assertThat(encryptor.prefix()).isEqualTo("ENC(");
        }

        @Test
        @DisplayName("应该返回正确的后缀")
        void shouldReturnCorrectSuffix() {
            assertThat(encryptor.suffix()).isEqualTo(")");
        }
    }

    @Nested
    @DisplayName("边界条件测试")
    class EdgeCaseTests {

        private final AesConfigEncryptor encryptor = new AesConfigEncryptor("1234567890123456");

        @Test
        @DisplayName("应该加密长字符串")
        void shouldEncryptLongString() {
            // given
            String plaintext = "a".repeat(10000);

            // when
            String encrypted = encryptor.encrypt(plaintext);
            String decrypted = encryptor.decrypt(encrypted);

            // then
            assertThat(decrypted).isEqualTo(plaintext);
        }

        @Test
        @DisplayName("应该正确处理只有前缀后缀的字符串")
        void shouldHandlePrefixSuffixOnlyString() {
            // given
            String encrypted = encryptor.encrypt("value");

            // when
            String decrypted = encryptor.decrypt(encrypted);

            // then
            assertThat(decrypted).isEqualTo("value");
        }
    }
}
