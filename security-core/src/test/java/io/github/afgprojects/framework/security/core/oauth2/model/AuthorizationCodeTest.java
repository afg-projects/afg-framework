package io.github.afgprojects.framework.security.core.oauth2.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AuthorizationCode 测试
 */
@DisplayName("AuthorizationCode 测试")
class AuthorizationCodeTest {

    @Nested
    @DisplayName("PKCE 判断")
    class PkceTests {

        @Test
        @DisplayName("有 codeChallenge 应识别为 PKCE")
        void shouldIdentifyPkceWhenCodeChallengePresent() {
            AuthorizationCode code = new AuthorizationCode(
                    "code-123", "my-client", "user-001",
                    "https://app.example.com/callback", Set.of("read"),
                    "code-challenge-abc", "S256",
                    Instant.now().plusSeconds(300), Instant.now()
            );

            assertThat(code.isPkce()).isTrue();
        }

        @Test
        @DisplayName("无 codeChallenge 应识别为非 PKCE")
        void shouldIdentifyNonPkceWhenCodeChallengeAbsent() {
            AuthorizationCode code = new AuthorizationCode(
                    "code-123", "my-client", "user-001",
                    "https://app.example.com/callback", Set.of("read"),
                    null, null,
                    Instant.now().plusSeconds(300), Instant.now()
            );

            assertThat(code.isPkce()).isFalse();
        }

        @Test
        @DisplayName("空 codeChallenge 应识别为非 PKCE")
        void shouldIdentifyNonPkceWhenCodeChallengeEmpty() {
            AuthorizationCode code = new AuthorizationCode(
                    "code-123", "my-client", "user-001",
                    "https://app.example.com/callback", Set.of("read"),
                    "", null,
                    Instant.now().plusSeconds(300), Instant.now()
            );

            assertThat(code.isPkce()).isFalse();
        }
    }

    @Nested
    @DisplayName("过期判断")
    class ExpiryTests {

        @Test
        @DisplayName("过期时间在未来应判断为未过期")
        void shouldNotBeExpiredWhenExpiresAtIsInFuture() {
            AuthorizationCode code = new AuthorizationCode(
                    "code-123", "my-client", "user-001",
                    "https://app.example.com/callback", Set.of("read"),
                    null, null,
                    Instant.now().plusSeconds(300), Instant.now()
            );

            assertThat(code.isExpired()).isFalse();
            assertThat(code.isValid()).isTrue();
        }

        @Test
        @DisplayName("过期时间在过去应判断为已过期")
        void shouldBeExpiredWhenExpiresAtIsInPast() {
            AuthorizationCode code = new AuthorizationCode(
                    "code-123", "my-client", "user-001",
                    "https://app.example.com/callback", Set.of("read"),
                    null, null,
                    Instant.now().minusSeconds(10), Instant.now().minusSeconds(310)
            );

            assertThat(code.isExpired()).isTrue();
            assertThat(code.isValid()).isFalse();
        }
    }

    @Nested
    @DisplayName("record 特性")
    class RecordTests {

        @Test
        @DisplayName("应正确实现 equals 和 hashCode")
        void shouldImplementEqualsAndHashCode() {
            Instant now = Instant.now();
            Instant expires = now.plusSeconds(300);

            AuthorizationCode code1 = new AuthorizationCode(
                    "code-123", "my-client", "user-001",
                    "https://app.example.com/callback", Set.of("read"),
                    null, null, expires, now
            );

            AuthorizationCode code2 = new AuthorizationCode(
                    "code-123", "my-client", "user-001",
                    "https://app.example.com/callback", Set.of("read"),
                    null, null, expires, now
            );

            assertThat(code1).isEqualTo(code2);
            assertThat(code1).hasSameHashCodeAs(code2);
        }

        @Test
        @DisplayName("应正确实现 toString")
        void shouldImplementToString() {
            AuthorizationCode code = new AuthorizationCode(
                    "code-123", "my-client", "user-001",
                    "https://app.example.com/callback", Set.of("read"),
                    null, null,
                    Instant.now().plusSeconds(300), Instant.now()
            );

            String str = code.toString();

            assertThat(str).contains("AuthorizationCode");
            assertThat(str).contains("code-123");
            assertThat(str).contains("my-client");
        }
    }
}
