package io.github.afgprojects.framework.core.web.security.signature;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SignatureAlgorithm")
class SignatureAlgorithmTest {

    @Nested
    @DisplayName("getAlgorithm")
    class GetAlgorithm {

        @Test
        @DisplayName("should return HmacSHA256 for HMAC_SHA256")
        void shouldReturnHmacSha256() {
            assertThat(SignatureAlgorithm.HMAC_SHA256.getAlgorithm()).isEqualTo("HmacSHA256");
        }

        @Test
        @DisplayName("should return HmacSHA384 for HMAC_SHA384")
        void shouldReturnHmacSha384() {
            assertThat(SignatureAlgorithm.HMAC_SHA384.getAlgorithm()).isEqualTo("HmacSHA384");
        }

        @Test
        @DisplayName("should return HmacSHA512 for HMAC_SHA512")
        void shouldReturnHmacSha512() {
            assertThat(SignatureAlgorithm.HMAC_SHA512.getAlgorithm()).isEqualTo("HmacSHA512");
        }
    }

    @Nested
    @DisplayName("getShortName")
    class GetShortName {

        @Test
        @DisplayName("should return HS256 for HMAC_SHA256")
        void shouldReturnHs256() {
            assertThat(SignatureAlgorithm.HMAC_SHA256.getShortName()).isEqualTo("HS256");
        }

        @Test
        @DisplayName("should return HS384 for HMAC_SHA384")
        void shouldReturnHs384() {
            assertThat(SignatureAlgorithm.HMAC_SHA384.getShortName()).isEqualTo("HS384");
        }

        @Test
        @DisplayName("should return HS512 for HMAC_SHA512")
        void shouldReturnHs512() {
            assertThat(SignatureAlgorithm.HMAC_SHA512.getShortName()).isEqualTo("HS512");
        }
    }

    @Nested
    @DisplayName("fromShortName")
    class FromShortName {

        @ParameterizedTest
        @CsvSource({"HS256, HMAC_SHA256", "HS384, HMAC_SHA384", "HS512, HMAC_SHA512"})
        @DisplayName("should resolve algorithm from short name")
        void shouldResolveAlgorithmFromShortName(String shortName, SignatureAlgorithm expected) {
            assertThat(SignatureAlgorithm.fromShortName(shortName)).isEqualTo(expected);
        }

        @Test
        @DisplayName("should resolve case insensitive")
        void shouldResolveCaseInsensitive() {
            assertThat(SignatureAlgorithm.fromShortName("hs256")).isEqualTo(SignatureAlgorithm.HMAC_SHA256);
            assertThat(SignatureAlgorithm.fromShortName("Hs256")).isEqualTo(SignatureAlgorithm.HMAC_SHA256);
        }

        @Test
        @DisplayName("should return null for unknown short name")
        void shouldReturnNull_forUnknownShortName() {
            assertThat(SignatureAlgorithm.fromShortName("UNKNOWN")).isNull();
            assertThat(SignatureAlgorithm.fromShortName("RS256")).isNull();
        }
    }
}
