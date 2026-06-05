package io.github.afgprojects.framework.security.core.login.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LoginResponse 测试
 */
@DisplayName("LoginResponse 测试")
class LoginResponseTest {

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("应使用 Builder 构建完整响应")
        void shouldBuildCompleteResponse() {
            LoginResponse response = LoginResponse.builder()
                    .accessToken("access-token-123")
                    .refreshToken("refresh-token-456")
                    .tokenType("Bearer")
                    .expiresIn(7200)
                    .userId("user-001")
                    .username("admin")
                    .roles(List.of("ADMIN", "USER"))
                    .permissions(List.of("user:read", "user:write"))
                    .tenantId("tenant-001")
                    .build();

            assertThat(response.accessToken()).isEqualTo("access-token-123");
            assertThat(response.refreshToken()).isEqualTo("refresh-token-456");
            assertThat(response.tokenType()).isEqualTo("Bearer");
            assertThat(response.expiresIn()).isEqualTo(7200);
            assertThat(response.userId()).isEqualTo("user-001");
            assertThat(response.username()).isEqualTo("admin");
            assertThat(response.roles()).containsExactly("ADMIN", "USER");
            assertThat(response.permissions()).containsExactly("user:read", "user:write");
            assertThat(response.tenantId()).isEqualTo("tenant-001");
        }

        @Test
        @DisplayName("Builder 应使用默认 tokenType")
        void shouldUseDefaultTokenType() {
            LoginResponse response = LoginResponse.builder()
                    .accessToken("access-token-123")
                    .expiresIn(7200)
                    .userId("user-001")
                    .username("admin")
                    .build();

            assertThat(response.tokenType()).isEqualTo("Bearer");
        }

        @Test
        @DisplayName("refreshToken 可为 null")
        void shouldAllowNullRefreshToken() {
            LoginResponse response = LoginResponse.builder()
                    .accessToken("access-token-123")
                    .expiresIn(7200)
                    .userId("user-001")
                    .username("admin")
                    .build();

            assertThat(response.refreshToken()).isNull();
        }

        @Test
        @DisplayName("roles 可为 null")
        void shouldAllowNullRoles() {
            LoginResponse response = LoginResponse.builder()
                    .accessToken("access-token-123")
                    .expiresIn(7200)
                    .userId("user-001")
                    .username("admin")
                    .build();

            assertThat(response.roles()).isNull();
        }

        @Test
        @DisplayName("permissions 可为 null")
        void shouldAllowNullPermissions() {
            LoginResponse response = LoginResponse.builder()
                    .accessToken("access-token-123")
                    .expiresIn(7200)
                    .userId("user-001")
                    .username("admin")
                    .build();

            assertThat(response.permissions()).isNull();
        }

        @Test
        @DisplayName("tenantId 可为 null")
        void shouldAllowNullTenantId() {
            LoginResponse response = LoginResponse.builder()
                    .accessToken("access-token-123")
                    .expiresIn(7200)
                    .userId("user-001")
                    .username("admin")
                    .build();

            assertThat(response.tenantId()).isNull();
        }
    }

    @Nested
    @DisplayName("record 特性")
    class RecordTests {

        @Test
        @DisplayName("应正确实现 equals 和 hashCode")
        void shouldImplementEqualsAndHashCode() {
            LoginResponse response1 = LoginResponse.builder()
                    .accessToken("token-123")
                    .expiresIn(7200)
                    .userId("user-001")
                    .username("admin")
                    .build();

            LoginResponse response2 = LoginResponse.builder()
                    .accessToken("token-123")
                    .expiresIn(7200)
                    .userId("user-001")
                    .username("admin")
                    .build();

            LoginResponse response3 = LoginResponse.builder()
                    .accessToken("token-456")
                    .expiresIn(7200)
                    .userId("user-001")
                    .username("admin")
                    .build();

            assertThat(response1).isEqualTo(response2);
            assertThat(response1).hasSameHashCodeAs(response2);
            assertThat(response1).isNotEqualTo(response3);
        }

        @Test
        @DisplayName("应正确实现 toString")
        void shouldImplementToString() {
            LoginResponse response = LoginResponse.builder()
                    .accessToken("token-123")
                    .expiresIn(7200)
                    .userId("user-001")
                    .username("admin")
                    .build();

            String str = response.toString();

            assertThat(str).contains("LoginResponse");
            assertThat(str).contains("token-123");
            assertThat(str).contains("admin");
        }
    }
}
