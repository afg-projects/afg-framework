package io.github.afgprojects.framework.security.core.oauth2.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TokenResponse 测试
 */
@DisplayName("TokenResponse 测试")
class TokenResponseTest {

    @Nested
    @DisplayName("工厂方法")
    class FactoryMethodTests {

        @Test
        @DisplayName("of 应创建完整令牌响应")
        void shouldCreateFullTokenResponse() {
            TokenResponse response = TokenResponse.of("access-token-123", 7200, "refresh-token-456", "read write");

            assertThat(response.accessToken()).isEqualTo("access-token-123");
            assertThat(response.tokenType()).isEqualTo("Bearer");
            assertThat(response.expiresIn()).isEqualTo(7200);
            assertThat(response.refreshToken()).isEqualTo("refresh-token-456");
            assertThat(response.scope()).isEqualTo("read write");
        }

        @Test
        @DisplayName("of 应使用默认 tokenType")
        void shouldUseDefaultTokenType() {
            TokenResponse response = TokenResponse.of("access-token-123", 7200, null, null);

            assertThat(response.tokenType()).isEqualTo("Bearer");
        }

        @Test
        @DisplayName("ofAccessToken 应创建只有访问令牌的响应")
        void shouldCreateAccessTokenOnlyResponse() {
            TokenResponse response = TokenResponse.ofAccessToken("access-token-123", 7200);

            assertThat(response.accessToken()).isEqualTo("access-token-123");
            assertThat(response.tokenType()).isEqualTo("Bearer");
            assertThat(response.expiresIn()).isEqualTo(7200);
            assertThat(response.refreshToken()).isNull();
            assertThat(response.scope()).isNull();
        }
    }

    @Nested
    @DisplayName("record 特性")
    class RecordTests {

        @Test
        @DisplayName("应正确实现 equals 和 hashCode")
        void shouldImplementEqualsAndHashCode() {
            TokenResponse response1 = TokenResponse.ofAccessToken("token-123", 7200);
            TokenResponse response2 = TokenResponse.ofAccessToken("token-123", 7200);
            TokenResponse response3 = TokenResponse.ofAccessToken("token-456", 7200);

            assertThat(response1).isEqualTo(response2);
            assertThat(response1).hasSameHashCodeAs(response2);
            assertThat(response1).isNotEqualTo(response3);
        }

        @Test
        @DisplayName("应正确实现 toString")
        void shouldImplementToString() {
            TokenResponse response = TokenResponse.ofAccessToken("token-123", 7200);

            String str = response.toString();

            assertThat(str).contains("TokenResponse");
            assertThat(str).contains("Bearer");
        }
    }
}
