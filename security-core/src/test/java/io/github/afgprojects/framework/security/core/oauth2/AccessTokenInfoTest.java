package io.github.afgprojects.framework.security.core.oauth2;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AccessTokenInfo 测试
 */
@DisplayName("AccessTokenInfo 测试")
class AccessTokenInfoTest {

    @Nested
    @DisplayName("过期判断")
    class ExpiryTests {

        @Test
        @DisplayName("过期时间在未来应判断为未过期")
        void shouldNotBeExpiredWhenExpiresAtIsInFuture() {
            AccessTokenInfo info = new AccessTokenInfo(
                    "user-001", "admin", "my-client",
                    Set.of("read", "write"), "tenant-001",
                    Instant.now().plusSeconds(7200), Instant.now()
            );

            assertThat(info.isExpired()).isFalse();
            assertThat(info.isValid()).isTrue();
        }

        @Test
        @DisplayName("过期时间在过去应判断为已过期")
        void shouldBeExpiredWhenExpiresAtIsInPast() {
            AccessTokenInfo info = new AccessTokenInfo(
                    "user-001", "admin", "my-client",
                    Set.of("read", "write"), "tenant-001",
                    Instant.now().minusSeconds(10), Instant.now().minusSeconds(7210)
            );

            assertThat(info.isExpired()).isTrue();
            assertThat(info.isValid()).isFalse();
        }
    }

    @Nested
    @DisplayName("record 特性")
    class RecordTests {

        @Test
        @DisplayName("应正确实现 equals 和 hashCode")
        void shouldImplementEqualsAndHashCode() {
            Instant now = Instant.now();
            Instant expires = now.plusSeconds(7200);

            AccessTokenInfo info1 = new AccessTokenInfo(
                    "user-001", "admin", "my-client",
                    Set.of("read"), "tenant-001", expires, now
            );

            AccessTokenInfo info2 = new AccessTokenInfo(
                    "user-001", "admin", "my-client",
                    Set.of("read"), "tenant-001", expires, now
            );

            assertThat(info1).isEqualTo(info2);
            assertThat(info1).hasSameHashCodeAs(info2);
        }

        @Test
        @DisplayName("应正确实现 toString")
        void shouldImplementToString() {
            AccessTokenInfo info = new AccessTokenInfo(
                    "user-001", "admin", "my-client",
                    Set.of("read"), "tenant-001",
                    Instant.now().plusSeconds(7200), Instant.now()
            );

            String str = info.toString();

            assertThat(str).contains("AccessTokenInfo");
            assertThat(str).contains("user-001");
            assertThat(str).contains("admin");
        }
    }
}
