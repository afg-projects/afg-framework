package io.github.afgprojects.framework.core.config;

import io.github.afgprojects.framework.commons.exception.BusinessException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AesConfigEncryptor")
class AesConfigEncryptorTest {

    // 32-byte key for AES-256
    private static final String SECRET_KEY = "0123456789abcdef0123456789abcdef";

    private final AesConfigEncryptor encryptor = new AesConfigEncryptor(SECRET_KEY);

    @Nested
    @DisplayName("constructor")
    class Constructor {

        @Test
        @DisplayName("should accept 16-byte key for AES-128")
        void shouldAccept16ByteKey() {
            AesConfigEncryptor enc = new AesConfigEncryptor("0123456789abcdef");
            assertThat(enc).isNotNull();
        }

        @Test
        @DisplayName("should accept 24-byte key for AES-192")
        void shouldAccept24ByteKey() {
            AesConfigEncryptor enc = new AesConfigEncryptor("0123456789abcdef01234567");
            assertThat(enc).isNotNull();
        }

        @Test
        @DisplayName("should accept 32-byte key for AES-256")
        void shouldAccept32ByteKey() {
            AesConfigEncryptor enc = new AesConfigEncryptor("0123456789abcdef0123456789abcdef");
            assertThat(enc).isNotNull();
        }

        @Test
        @DisplayName("should reject invalid key length")
        void shouldRejectInvalidKeyLength() {
            assertThatThrownBy(() -> new AesConfigEncryptor("short"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Secret key must be 16, 24, or 32 bytes");
        }
    }

    @Nested
    @DisplayName("encrypt / decrypt roundtrip")
    class EncryptDecryptRoundtrip {

        @Test
        @DisplayName("should encrypt and decrypt text back to original")
        void shouldEncryptAndDecryptTextBackToOriginal() {
            String original = "my-secret-password";
            String encrypted = encryptor.encrypt(original);

            assertThat(encrypted).isNotEqualTo(original);
            assertThat(encryptor.decrypt(encrypted)).isEqualTo(original);
        }

        @Test
        @DisplayName("should handle empty string")
        void shouldHandleEmptyString() {
            String encrypted = encryptor.encrypt("");
            assertThat(encryptor.decrypt(encrypted)).isEmpty();
        }

        @Test
        @DisplayName("should handle unicode text")
        void shouldHandleUnicodeText() {
            String original = "密码测试🔑";
            String encrypted = encryptor.encrypt(original);

            assertThat(encryptor.decrypt(encrypted)).isEqualTo(original);
        }

        @Test
        @DisplayName("should handle long text")
        void shouldHandleLongText() {
            String original = "a".repeat(10000);
            String encrypted = encryptor.encrypt(original);

            assertThat(encryptor.decrypt(encrypted)).isEqualTo(original);
        }

        @Test
        @DisplayName("should produce different ciphertext for same plaintext (random IV)")
        void shouldProduceDifferentCiphertext_forSamePlaintext() {
            String original = "same-text";
            String encrypted1 = encryptor.encrypt(original);
            String encrypted2 = encryptor.encrypt(original);

            // Due to random IV, the ciphertext should differ
            assertThat(encrypted1).isNotEqualTo(encrypted2);
            // But both should decrypt to the same value
            assertThat(encryptor.decrypt(encrypted1)).isEqualTo(original);
            assertThat(encryptor.decrypt(encrypted2)).isEqualTo(original);
        }
    }

    @Nested
    @DisplayName("encrypt format")
    class EncryptFormat {

        @Test
        @DisplayName("should wrap encrypted value in ENC() prefix and suffix")
        void shouldWrapEncryptedValueInEncPrefixAndSuffix() {
            String encrypted = encryptor.encrypt("test");

            assertThat(encrypted).startsWith("ENC(");
            assertThat(encrypted).endsWith(")");
        }

        @Test
        @DisplayName("should return correct prefix")
        void shouldReturnCorrectPrefix() {
            assertThat(encryptor.prefix()).isEqualTo("ENC(");
        }

        @Test
        @DisplayName("should return correct suffix")
        void shouldReturnCorrectSuffix() {
            assertThat(encryptor.suffix()).isEqualTo(")");
        }
    }

    @Nested
    @DisplayName("decrypt error handling")
    class DecryptErrorHandling {

        @Test
        @DisplayName("should throw on invalid ciphertext format")
        void shouldThrowOnInvalidCiphertextFormat() {
            assertThatThrownBy(() -> encryptor.decrypt("ENC(invalid-base64!!!)"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Decryption failed");
        }

        @Test
        @DisplayName("should throw on too-short ciphertext")
        void shouldThrowOnTooShortCiphertext() {
            // Base64 of 10 bytes (less than IV 12 + tag 16 = 28 minimum)
            assertThatThrownBy(() -> encryptor.decrypt("ENC(YWJjZGVmZ2hp)"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Decryption failed");
        }
    }

    @Nested
    @DisplayName("isEncrypted (ConfigEncryptor default method)")
    class IsEncrypted {

        @Test
        @DisplayName("should detect encrypted value")
        void shouldDetectEncryptedValue() {
            String encrypted = encryptor.encrypt("test");

            assertThat(encryptor.isEncrypted(encrypted)).isTrue();
        }

        @Test
        @DisplayName("should not detect plain text as encrypted")
        void shouldNotDetectPlainText_asEncrypted() {
            assertThat(encryptor.isEncrypted("plain-text")).isFalse();
        }

        @Test
        @DisplayName("should not detect null as encrypted")
        void shouldNotDetectNull_asEncrypted() {
            assertThat(encryptor.isEncrypted(null)).isFalse();
        }

        @Test
        @DisplayName("should detect ENC() wrapper as encrypted")
        void shouldDetectEncWrapper_asEncrypted() {
            assertThat(encryptor.isEncrypted("ENC(something)")).isTrue();
        }
    }

    @Nested
    @DisplayName("extractContent (ConfigEncryptor default method)")
    class ExtractContent {

        @Test
        @DisplayName("should extract content from ENC() wrapper")
        void shouldExtractContentFromEncWrapper() {
            String content = encryptor.extractContent("ENC(abc123)");

            assertThat(content).isEqualTo("abc123");
        }
    }

    @Nested
    @DisplayName("decryptIfNeeded (ConfigEncryptor default method)")
    class DecryptIfNeeded {

        @Test
        @DisplayName("should decrypt encrypted value")
        void shouldDecryptEncryptedValue() {
            String original = "test-value";
            String encrypted = encryptor.encrypt(original);

            assertThat(encryptor.decryptIfNeeded(encrypted)).isEqualTo(original);
        }

        @Test
        @DisplayName("should return plain value unchanged")
        void shouldReturnPlainValueUnchanged() {
            assertThat(encryptor.decryptIfNeeded("plain-value")).isEqualTo("plain-value");
        }

        @Test
        @DisplayName("should return null for null input")
        void shouldReturnNull_forNullInput() {
            assertThat(encryptor.decryptIfNeeded(null)).isNull();
        }
    }

    @Nested
    @DisplayName("cross-key isolation")
    class CrossKeyIsolation {

        @Test
        @DisplayName("should fail to decrypt with different key")
        void shouldFailToDecryptWithDifferentKey() {
            AesConfigEncryptor encryptor1 = new AesConfigEncryptor("0123456789abcdef0123456789abcdef");
            AesConfigEncryptor encryptor2 = new AesConfigEncryptor("fedcba9876543210fedcba9876543210");

            String encrypted = encryptor1.encrypt("test-data");

            assertThatThrownBy(() -> encryptor2.decrypt(encrypted))
                    .isInstanceOf(BusinessException.class);
        }
    }
}
