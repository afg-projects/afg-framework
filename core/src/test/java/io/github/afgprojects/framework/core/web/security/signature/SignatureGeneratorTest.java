package io.github.afgprojects.framework.core.web.security.signature;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SignatureGenerator")
class SignatureGeneratorTest {

    private final SignatureGenerator generator = new SignatureGenerator();

    @Nested
    @DisplayName("generate")
    class Generate {

        @Test
        @DisplayName("should generate non-empty Base64 signature")
        void shouldGenerateNonEmptyBase64Signature() {
            String signature = generator.generate(
                    SignatureAlgorithm.HMAC_SHA256, "secret", "1234567890", "abc123", null);

            assertThat(signature).isNotBlank();
            // Base64 encoded 32 bytes = 44 chars
            assertThat(signature).hasSize(44);
        }

        @Test
        @DisplayName("should generate deterministic signature for same inputs")
        void shouldGenerateDeterministicSignature() {
            String sig1 = generator.generate(
                    SignatureAlgorithm.HMAC_SHA256, "secret", "1234567890", "abc123", "body");
            String sig2 = generator.generate(
                    SignatureAlgorithm.HMAC_SHA256, "secret", "1234567890", "abc123", "body");

            assertThat(sig1).isEqualTo(sig2);
        }

        @Test
        @DisplayName("should generate different signatures for different secrets")
        void shouldGenerateDifferentSignatures_forDifferentSecrets() {
            String sig1 = generator.generate(
                    SignatureAlgorithm.HMAC_SHA256, "secret1", "1234567890", "abc123", null);
            String sig2 = generator.generate(
                    SignatureAlgorithm.HMAC_SHA256, "secret2", "1234567890", "abc123", null);

            assertThat(sig1).isNotEqualTo(sig2);
        }

        @Test
        @DisplayName("should generate different signatures for different bodies")
        void shouldGenerateDifferentSignatures_forDifferentBodies() {
            String sig1 = generator.generate(
                    SignatureAlgorithm.HMAC_SHA256, "secret", "1234567890", "abc123", "body1");
            String sig2 = generator.generate(
                    SignatureAlgorithm.HMAC_SHA256, "secret", "1234567890", "abc123", "body2");

            assertThat(sig1).isNotEqualTo(sig2);
        }

        @ParameterizedTest
        @EnumSource(SignatureAlgorithm.class)
        @DisplayName("should generate signature for all algorithms")
        void shouldGenerateSignature_forAllAlgorithms(SignatureAlgorithm algorithm) {
            String signature = generator.generate(algorithm, "secret", "1234567890", "abc123", "body");

            assertThat(signature).isNotBlank();
        }

        @Test
        @DisplayName("should generate different signature lengths for different algorithms")
        void shouldGenerateDifferentSignatureLengths_forDifferentAlgorithms() {
            String sha256 = generator.generate(
                    SignatureAlgorithm.HMAC_SHA256, "secret", "1234567890", "abc123", null);
            String sha384 = generator.generate(
                    SignatureAlgorithm.HMAC_SHA384, "secret", "1234567890", "abc123", null);
            String sha512 = generator.generate(
                    SignatureAlgorithm.HMAC_SHA512, "secret", "1234567890", "abc123", null);

            // SHA-256: 32 bytes -> 44 Base64 chars
            // SHA-384: 48 bytes -> 64 Base64 chars
            // SHA-512: 64 bytes -> 88 Base64 chars
            assertThat(sha256).hasSize(44);
            assertThat(sha384).hasSize(64);
            assertThat(sha512).hasSize(88);
        }

        @Test
        @DisplayName("should throw when secret is invalid")
        void shouldThrow_whenSecretIsInvalid() {
            assertThatThrownBy(() ->
                    generator.generate(SignatureAlgorithm.HMAC_SHA256, "", "1234567890", "abc123", null))
                    .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("verify")
    class Verify {

        @Test
        @DisplayName("should verify valid signature")
        void shouldVerifyValidSignature() {
            String signature = generator.generate(
                    SignatureAlgorithm.HMAC_SHA256, "secret", "1234567890", "abc123", "body");

            boolean result = generator.verify(
                    SignatureAlgorithm.HMAC_SHA256, "secret", "1234567890", "abc123", "body", signature);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should reject wrong secret")
        void shouldRejectWrongSecret() {
            String signature = generator.generate(
                    SignatureAlgorithm.HMAC_SHA256, "secret1", "1234567890", "abc123", "body");

            boolean result = generator.verify(
                    SignatureAlgorithm.HMAC_SHA256, "secret2", "1234567890", "abc123", "body", signature);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should reject wrong body")
        void shouldRejectWrongBody() {
            String signature = generator.generate(
                    SignatureAlgorithm.HMAC_SHA256, "secret", "1234567890", "abc123", "body1");

            boolean result = generator.verify(
                    SignatureAlgorithm.HMAC_SHA256, "secret", "1234567890", "abc123", "body2", signature);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should reject wrong timestamp")
        void shouldRejectWrongTimestamp() {
            String signature = generator.generate(
                    SignatureAlgorithm.HMAC_SHA256, "secret", "1234567890", "abc123", "body");

            boolean result = generator.verify(
                    SignatureAlgorithm.HMAC_SHA256, "secret", "9999999999", "abc123", "body", signature);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should reject wrong nonce")
        void shouldRejectWrongNonce() {
            String signature = generator.generate(
                    SignatureAlgorithm.HMAC_SHA256, "secret", "1234567890", "abc123", "body");

            boolean result = generator.verify(
                    SignatureAlgorithm.HMAC_SHA256, "secret", "1234567890", "xyz789", "body", signature);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should reject wrong algorithm")
        void shouldRejectWrongAlgorithm() {
            String signature = generator.generate(
                    SignatureAlgorithm.HMAC_SHA256, "secret", "1234567890", "abc123", "body");

            boolean result = generator.verify(
                    SignatureAlgorithm.HMAC_SHA512, "secret", "1234567890", "abc123", "body", signature);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should verify signature with null body")
        void shouldVerifySignatureWithNullBody() {
            String signature = generator.generate(
                    SignatureAlgorithm.HMAC_SHA256, "secret", "1234567890", "abc123", null);

            boolean result = generator.verify(
                    SignatureAlgorithm.HMAC_SHA256, "secret", "1234567890", "abc123", null, signature);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should treat null body same as empty body")
        void shouldTreatNullBodySameAsEmptyBody() {
            // Both null and empty body result in the same signing string (no body appended)
            String signature = generator.generate(
                    SignatureAlgorithm.HMAC_SHA256, "secret", "1234567890", "abc123", null);

            boolean result = generator.verify(
                    SignatureAlgorithm.HMAC_SHA256, "secret", "1234567890", "abc123", "", signature);

            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("buildSigningString")
    class BuildSigningString {

        @Test
        @DisplayName("should build signing string with timestamp and nonce")
        void shouldBuildSigningStringWithTimestampAndNonce() {
            String result = generator.buildSigningString("1234567890", "abc123", null);

            assertThat(result).isEqualTo("1234567890\nabc123");
        }

        @Test
        @DisplayName("should append body when present")
        void shouldAppendBodyWhenPresent() {
            String result = generator.buildSigningString("1234567890", "abc123", "request body");

            assertThat(result).isEqualTo("1234567890\nabc123\nrequest body");
        }

        @Test
        @DisplayName("should not append body when empty")
        void shouldNotAppendBodyWhenEmpty() {
            String result = generator.buildSigningString("1234567890", "abc123", "");

            assertThat(result).isEqualTo("1234567890\nabc123");
        }
    }

    @Nested
    @DisplayName("constantTimeEquals")
    class ConstantTimeEquals {

        @Test
        @DisplayName("should return true for equal strings")
        void shouldReturnTrue_forEqualStrings() {
            assertThat(generator.constantTimeEquals("hello", "hello")).isTrue();
        }

        @Test
        @DisplayName("should return false for different strings")
        void shouldReturnFalse_forDifferentStrings() {
            assertThat(generator.constantTimeEquals("hello", "world")).isFalse();
        }

        @Test
        @DisplayName("should return false for strings of different length")
        void shouldReturnFalse_forDifferentLengths() {
            assertThat(generator.constantTimeEquals("short", "much_longer_string")).isFalse();
        }

        @Test
        @DisplayName("should return true for empty strings")
        void shouldReturnTrue_forEmptyStrings() {
            assertThat(generator.constantTimeEquals("", "")).isTrue();
        }
    }
}
