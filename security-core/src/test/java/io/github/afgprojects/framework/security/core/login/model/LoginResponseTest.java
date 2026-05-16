package io.github.afgprojects.framework.security.core.login.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class LoginResponseTest {

    @Nested
    @DisplayName("Builder 模式测试")
    class BuilderTests {

        @Test
        @DisplayName("应使用 Builder 构建完整响应")
        void shouldBuildCompleteResponse() {
            LoginResponse response = LoginResponse.builder()
                    .accessToken("access-token-123")
                    .refreshToken("refresh-token-456")
                    .tokenType("Bearer")
                    .expiresIn(3600L)
                    .userId("user-001")
                    .username("admin")
                    .roles(List.of("ADMIN", "USER"))
                    .permissions(List.of("user:read", "user:write"))
                    .tenantId("tenant-001")
                    .build();

            assertThat(response.accessToken()).isEqualTo("access-token-123");
            assertThat(response.refreshToken()).isEqualTo("refresh-token-456");
            assertThat(response.tokenType()).isEqualTo("Bearer");
            assertThat(response.expiresIn()).isEqualTo(3600L);
            assertThat(response.userId()).isEqualTo("user-001");
            assertThat(response.username()).isEqualTo("admin");
            assertThat(response.roles()).containsExactly("ADMIN", "USER");
            assertThat(response.permissions()).containsExactly("user:read", "user:write");
            assertThat(response.tenantId()).isEqualTo("tenant-001");
        }

        @Test
        @DisplayName("应使用 Builder 构建最小响应")
        void shouldBuildMinimalResponse() {
            LoginResponse response = LoginResponse.builder()
                    .accessToken("access-token-123")
                    .expiresIn(3600L)
                    .userId("user-001")
                    .username("admin")
                    .build();

            assertThat(response.accessToken()).isEqualTo("access-token-123");
            assertThat(response.expiresIn()).isEqualTo(3600L);
            assertThat(response.userId()).isEqualTo("user-001");
            assertThat(response.username()).isEqualTo("admin");
        }
    }

    @Nested
    @DisplayName("默认值测试")
    class DefaultValueTests {

        @Test
        @DisplayName("默认 Token 类型应为 Bearer")
        void defaultTokenTypeShouldBeBearer() {
            LoginResponse response = LoginResponse.builder()
                    .accessToken("access-token-123")
                    .expiresIn(3600L)
                    .userId("user-001")
                    .username("admin")
                    .build();

            assertThat(response.tokenType()).isEqualTo("Bearer");
        }

        @Test
        @DisplayName("应允许覆盖默认 Token 类型")
        void shouldAllowOverrideTokenType() {
            LoginResponse response = LoginResponse.builder()
                    .accessToken("access-token-123")
                    .tokenType("MAC")
                    .expiresIn(3600L)
                    .userId("user-001")
                    .username("admin")
                    .build();

            assertThat(response.tokenType()).isEqualTo("MAC");
        }
    }

    @Nested
    @DisplayName("可选字段测试")
    class OptionalFieldTests {

        @Test
        @DisplayName("refreshToken 应为 null")
        void refreshTokenShouldBeNull() {
            LoginResponse response = LoginResponse.builder()
                    .accessToken("access-token-123")
                    .expiresIn(3600L)
                    .userId("user-001")
                    .username("admin")
                    .build();

            assertThat(response.refreshToken()).isNull();
        }

        @Test
        @DisplayName("roles 应为 null")
        void rolesShouldBeNull() {
            LoginResponse response = LoginResponse.builder()
                    .accessToken("access-token-123")
                    .expiresIn(3600L)
                    .userId("user-001")
                    .username("admin")
                    .build();

            assertThat(response.roles()).isNull();
        }

        @Test
        @DisplayName("permissions 应为 null")
        void permissionsShouldBeNull() {
            LoginResponse response = LoginResponse.builder()
                    .accessToken("access-token-123")
                    .expiresIn(3600L)
                    .userId("user-001")
                    .username("admin")
                    .build();

            assertThat(response.permissions()).isNull();
        }

        @Test
        @DisplayName("tenantId 应为 null")
        void tenantIdShouldBeNull() {
            LoginResponse response = LoginResponse.builder()
                    .accessToken("access-token-123")
                    .expiresIn(3600L)
                    .userId("user-001")
                    .username("admin")
                    .build();

            assertThat(response.tenantId()).isNull();
        }

        @Test
        @DisplayName("应支持空角色列表")
        void shouldSupportEmptyRolesList() {
            LoginResponse response = LoginResponse.builder()
                    .accessToken("access-token-123")
                    .expiresIn(3600L)
                    .userId("user-001")
                    .username("admin")
                    .roles(List.of())
                    .build();

            assertThat(response.roles()).isEmpty();
        }

        @Test
        @DisplayName("应支持空权限列表")
        void shouldSupportEmptyPermissionsList() {
            LoginResponse response = LoginResponse.builder()
                    .accessToken("access-token-123")
                    .expiresIn(3600L)
                    .userId("user-001")
                    .username("admin")
                    .permissions(List.of())
                    .build();

            assertThat(response.permissions()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Record 特性测试")
    class RecordTests {

        @Test
        @DisplayName("应正确实现 equals")
        void shouldImplementEquals() {
            LoginResponse response1 = LoginResponse.builder()
                    .accessToken("access-token-123")
                    .expiresIn(3600L)
                    .userId("user-001")
                    .username("admin")
                    .build();

            LoginResponse response2 = LoginResponse.builder()
                    .accessToken("access-token-123")
                    .expiresIn(3600L)
                    .userId("user-001")
                    .username("admin")
                    .build();

            assertThat(response1).isEqualTo(response2);
        }

        @Test
        @DisplayName("应正确实现 hashCode")
        void shouldImplementHashCode() {
            LoginResponse response1 = LoginResponse.builder()
                    .accessToken("access-token-123")
                    .expiresIn(3600L)
                    .userId("user-001")
                    .username("admin")
                    .build();

            LoginResponse response2 = LoginResponse.builder()
                    .accessToken("access-token-123")
                    .expiresIn(3600L)
                    .userId("user-001")
                    .username("admin")
                    .build();

            assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
        }

        @Test
        @DisplayName("应正确实现 toString")
        void shouldImplementToString() {
            LoginResponse response = LoginResponse.builder()
                    .accessToken("access-token-123")
                    .expiresIn(3600L)
                    .userId("user-001")
                    .username("admin")
                    .build();

            String str = response.toString();

            assertThat(str).contains("LoginResponse");
            assertThat(str).contains("access-token-123");
            assertThat(str).contains("user-001");
            assertThat(str).contains("admin");
        }
    }
}
