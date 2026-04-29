package io.github.afgprojects.framework.core.web.security.signature;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * SignatureAlgorithm 测试
 */
@DisplayName("SignatureAlgorithm 测试")
class SignatureAlgorithmTest {

    @Nested
    @DisplayName("算法属性测试")
    class AlgorithmPropertyTests {

        @Test
        @DisplayName("HMAC-SHA256 应该有正确的属性")
        void hmacSha256ShouldHaveCorrectProperties() {
            // when
            SignatureAlgorithm algorithm = SignatureAlgorithm.HMAC_SHA256;

            // then
            assertThat(algorithm.getAlgorithm()).isEqualTo("HmacSHA256");
            assertThat(algorithm.getShortName()).isEqualTo("HS256");
        }

        @Test
        @DisplayName("HMAC-SHA384 应该有正确的属性")
        void hmacSha384ShouldHaveCorrectProperties() {
            // when
            SignatureAlgorithm algorithm = SignatureAlgorithm.HMAC_SHA384;

            // then
            assertThat(algorithm.getAlgorithm()).isEqualTo("HmacSHA384");
            assertThat(algorithm.getShortName()).isEqualTo("HS384");
        }

        @Test
        @DisplayName("HMAC-SHA512 应该有正确的属性")
        void hmacSha512ShouldHaveCorrectProperties() {
            // when
            SignatureAlgorithm algorithm = SignatureAlgorithm.HMAC_SHA512;

            // then
            assertThat(algorithm.getAlgorithm()).isEqualTo("HmacSHA512");
            assertThat(algorithm.getShortName()).isEqualTo("HS512");
        }
    }

    @Nested
    @DisplayName("简称查找测试")
    class FromShortNameTests {

        @Test
        @DisplayName("应该根据简称找到算法")
        void shouldFindAlgorithmByShortName() {
            // when
            SignatureAlgorithm algorithm = SignatureAlgorithm.fromShortName("HS256");

            // then
            assertThat(algorithm).isEqualTo(SignatureAlgorithm.HMAC_SHA256);
        }

        @Test
        @DisplayName("简称查找应该不区分大小写")
        void shouldBeCaseInsensitive() {
            // when
            SignatureAlgorithm algorithm = SignatureAlgorithm.fromShortName("hs256");

            // then
            assertThat(algorithm).isEqualTo(SignatureAlgorithm.HMAC_SHA256);
        }

        @Test
        @DisplayName("无效简称应该返回 null")
        void shouldReturnNullForInvalidShortName() {
            // when
            SignatureAlgorithm algorithm = SignatureAlgorithm.fromShortName("INVALID");

            // then
            assertThat(algorithm).isNull();
        }
    }
}
