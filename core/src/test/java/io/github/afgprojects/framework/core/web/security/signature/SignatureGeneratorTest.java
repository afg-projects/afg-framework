package io.github.afgprojects.framework.core.web.security.signature;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * SignatureGenerator 测试
 */
@DisplayName("SignatureGenerator 测试")
class SignatureGeneratorTest {

    private SignatureGenerator generator;
    private static final String SECRET = "test-secret-key-1234567890";
    private static final String TIMESTAMP = "1709875200000";
    private static final String NONCE = "abc123-def456";
    private static final String BODY = "{\"name\":\"test\",\"value\":123}";

    @BeforeEach
    void setUp() {
        generator = new SignatureGenerator();
    }

    @Nested
    @DisplayName("签名生成测试")
    class GenerateTests {

        @Test
        @DisplayName("应该使用 HMAC-SHA256 生成签名")
        void shouldGenerateWithHmacSha256() {
            // when
            String signature = generator.generate(SignatureAlgorithm.HMAC_SHA256, SECRET, TIMESTAMP, NONCE, BODY);

            // then
            assertThat(signature).isNotEmpty();
            assertThat(signature).doesNotContain("\n");
        }

        @Test
        @DisplayName("应该使用 HMAC-SHA384 生成签名")
        void shouldGenerateWithHmacSha384() {
            // when
            String signature = generator.generate(SignatureAlgorithm.HMAC_SHA384, SECRET, TIMESTAMP, NONCE, BODY);

            // then
            assertThat(signature).isNotEmpty();
        }

        @Test
        @DisplayName("应该使用 HMAC-SHA512 生成签名")
        void shouldGenerateWithHmacSha512() {
            // when
            String signature = generator.generate(SignatureAlgorithm.HMAC_SHA512, SECRET, TIMESTAMP, NONCE, BODY);

            // then
            assertThat(signature).isNotEmpty();
        }

        @Test
        @DisplayName("相同参数应该生成相同签名")
        void shouldGenerateSameSignatureForSameParams() {
            // when
            String signature1 = generator.generate(SignatureAlgorithm.HMAC_SHA256, SECRET, TIMESTAMP, NONCE, BODY);
            String signature2 = generator.generate(SignatureAlgorithm.HMAC_SHA256, SECRET, TIMESTAMP, NONCE, BODY);

            // then
            assertThat(signature1).isEqualTo(signature2);
        }

        @Test
        @DisplayName("不同密钥应该生成不同签名")
        void shouldGenerateDifferentSignatureForDifferentSecret() {
            // given
            String otherSecret = "other-secret-key-0987654321";

            // when
            String signature1 = generator.generate(SignatureAlgorithm.HMAC_SHA256, SECRET, TIMESTAMP, NONCE, BODY);
            String signature2 = generator.generate(SignatureAlgorithm.HMAC_SHA256, otherSecret, TIMESTAMP, NONCE, BODY);

            // then
            assertThat(signature1).isNotEqualTo(signature2);
        }

        @Test
        @DisplayName("不同时间戳应该生成不同签名")
        void shouldGenerateDifferentSignatureForDifferentTimestamp() {
            // when
            String signature1 = generator.generate(SignatureAlgorithm.HMAC_SHA256, SECRET, TIMESTAMP, NONCE, BODY);
            String signature2 = generator.generate(SignatureAlgorithm.HMAC_SHA256, SECRET, "1709875200001", NONCE, BODY);

            // then
            assertThat(signature1).isNotEqualTo(signature2);
        }

        @Test
        @DisplayName("不同 nonce 应该生成不同签名")
        void shouldGenerateDifferentSignatureForDifferentNonce() {
            // when
            String signature1 = generator.generate(SignatureAlgorithm.HMAC_SHA256, SECRET, TIMESTAMP, NONCE, BODY);
            String signature2 = generator.generate(SignatureAlgorithm.HMAC_SHA256, SECRET, TIMESTAMP, "xyz789", BODY);

            // then
            assertThat(signature1).isNotEqualTo(signature2);
        }

        @Test
        @DisplayName("不同 body 应该生成不同签名")
        void shouldGenerateDifferentSignatureForDifferentBody() {
            // when
            String signature1 = generator.generate(SignatureAlgorithm.HMAC_SHA256, SECRET, TIMESTAMP, NONCE, BODY);
            String signature2 = generator.generate(SignatureAlgorithm.HMAC_SHA256, SECRET, TIMESTAMP, NONCE, "{\"name\":\"other\"}");

            // then
            assertThat(signature1).isNotEqualTo(signature2);
        }

        @Test
        @DisplayName("null body 应该正常处理")
        void shouldHandleNullBody() {
            // when
            String signature = generator.generate(SignatureAlgorithm.HMAC_SHA256, SECRET, TIMESTAMP, NONCE, null);

            // then
            assertThat(signature).isNotEmpty();
        }

        @Test
        @DisplayName("空 body 应该正常处理")
        void shouldHandleEmptyBody() {
            // when
            String signature = generator.generate(SignatureAlgorithm.HMAC_SHA256, SECRET, TIMESTAMP, NONCE, "");

            // then
            assertThat(signature).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("签名验证测试")
    class VerifyTests {

        @Test
        @DisplayName("应该验证正确的签名")
        void shouldVerifyCorrectSignature() {
            // given
            String signature = generator.generate(SignatureAlgorithm.HMAC_SHA256, SECRET, TIMESTAMP, NONCE, BODY);

            // when
            boolean valid = generator.verify(SignatureAlgorithm.HMAC_SHA256, SECRET, TIMESTAMP, NONCE, BODY, signature);

            // then
            assertThat(valid).isTrue();
        }

        @Test
        @DisplayName("应该拒绝错误的签名")
        void shouldRejectWrongSignature() {
            // when
            boolean valid = generator.verify(SignatureAlgorithm.HMAC_SHA256, SECRET, TIMESTAMP, NONCE, BODY, "wrong-signature");

            // then
            assertThat(valid).isFalse();
        }

        @Test
        @DisplayName("应该拒绝错误密钥生成的签名")
        void shouldRejectSignatureFromWrongKey() {
            // given
            String wrongSecret = "wrong-secret-key-0987654321";
            String signature = generator.generate(SignatureAlgorithm.HMAC_SHA256, wrongSecret, TIMESTAMP, NONCE, BODY);

            // when
            boolean valid = generator.verify(SignatureAlgorithm.HMAC_SHA256, SECRET, TIMESTAMP, NONCE, BODY, signature);

            // then
            assertThat(valid).isFalse();
        }

        @Test
        @DisplayName("应该拒绝篡改后的签名")
        void shouldRejectTamperedSignature() {
            // given
            String signature = generator.generate(SignatureAlgorithm.HMAC_SHA256, SECRET, TIMESTAMP, NONCE, BODY);
            String tampered = signature.substring(0, signature.length() - 4) + "XXXX";

            // when
            boolean valid = generator.verify(SignatureAlgorithm.HMAC_SHA256, SECRET, TIMESTAMP, NONCE, BODY, tampered);

            // then
            assertThat(valid).isFalse();
        }
    }

    @Nested
    @DisplayName("签名字符串构建测试")
    class SigningStringTests {

        @Test
        @DisplayName("应该正确构建签名字符串")
        void shouldBuildSigningString() {
            // when
            String signingString = generator.buildSigningString(TIMESTAMP, NONCE, BODY);

            // then
            assertThat(signingString).isEqualTo(TIMESTAMP + "\n" + NONCE + "\n" + BODY);
        }

        @Test
        @DisplayName("null body 应该只包含时间戳和 nonce")
        void shouldBuildSigningStringWithoutBody() {
            // when
            String signingString = generator.buildSigningString(TIMESTAMP, NONCE, null);

            // then
            assertThat(signingString).isEqualTo(TIMESTAMP + "\n" + NONCE);
        }

        @Test
        @DisplayName("空 body 应该只包含时间戳和 nonce")
        void shouldBuildSigningStringWithEmptyBody() {
            // when
            String signingString = generator.buildSigningString(TIMESTAMP, NONCE, "");

            // then
            assertThat(signingString).isEqualTo(TIMESTAMP + "\n" + NONCE);
        }
    }

    @Nested
    @DisplayName("常量时间比较测试")
    class ConstantTimeEqualsTests {

        @Test
        @DisplayName("相同字符串应该返回 true")
        void shouldReturnTrueForEqualStrings() {
            // when
            boolean result = generator.constantTimeEquals("hello", "hello");

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("不同字符串应该返回 false")
        void shouldReturnFalseForDifferentStrings() {
            // when
            boolean result = generator.constantTimeEquals("hello", "world");

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("不同长度应该返回 false")
        void shouldReturnFalseForDifferentLengths() {
            // when
            boolean result = generator.constantTimeEquals("hello", "helloo");

            // then
            assertThat(result).isFalse();
        }
    }
}
