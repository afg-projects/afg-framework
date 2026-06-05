package io.github.afgprojects.framework.security.core.oauth2.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AuthorizationRequest 测试
 */
@DisplayName("AuthorizationRequest 测试")
class AuthorizationRequestTest {

    @Nested
    @DisplayName("PKCE 判断")
    class PkceTests {

        @Test
        @DisplayName("有 codeChallenge 应识别为 PKCE")
        void shouldIdentifyPkceWhenCodeChallengePresent() {
            AuthorizationRequest request = new AuthorizationRequest(
                    "code", "my-client", "https://app.example.com/callback",
                    null, null, "code-challenge-abc", "S256"
            );

            assertThat(request.isPkce()).isTrue();
        }

        @Test
        @DisplayName("无 codeChallenge 应识别为非 PKCE")
        void shouldIdentifyNonPkceWhenCodeChallengeAbsent() {
            AuthorizationRequest request = new AuthorizationRequest(
                    "code", "my-client", "https://app.example.com/callback",
                    null, null, null, null
            );

            assertThat(request.isPkce()).isFalse();
        }

        @Test
        @DisplayName("空 codeChallenge 应识别为非 PKCE")
        void shouldIdentifyNonPkceWhenCodeChallengeEmpty() {
            AuthorizationRequest request = new AuthorizationRequest(
                    "code", "my-client", "https://app.example.com/callback",
                    null, null, "", null
            );

            assertThat(request.isPkce()).isFalse();
        }
    }

    @Nested
    @DisplayName("record 特性")
    class RecordTests {

        @Test
        @DisplayName("应正确实现 equals 和 hashCode")
        void shouldImplementEqualsAndHashCode() {
            AuthorizationRequest request1 = new AuthorizationRequest(
                    "code", "my-client", "https://app.example.com/callback",
                    null, null, null, null
            );

            AuthorizationRequest request2 = new AuthorizationRequest(
                    "code", "my-client", "https://app.example.com/callback",
                    null, null, null, null
            );

            assertThat(request1).isEqualTo(request2);
            assertThat(request1).hasSameHashCodeAs(request2);
        }
    }
}
