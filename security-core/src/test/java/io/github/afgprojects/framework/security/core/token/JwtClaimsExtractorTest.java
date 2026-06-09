package io.github.afgprojects.framework.security.core.token;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link JwtClaimsExtractor} 单元测试。
 *
 * <p>测试 JWT Claims 提取逻辑和 Bearer Token 解析功能。
 * 使用 {@link MockHttpServletRequest} 代替 Mockito mock。
 *
 * @since 1.0.0
 */
@DisplayName("JwtClaimsExtractor 测试")
class JwtClaimsExtractorTest {

    // ==================== 默认配置测试 ====================

    @Nested
    @DisplayName("默认配置提取")
    class DefaultConfigTests {

        @Test
        @DisplayName("应从 sub claim 提取用户 ID")
        void shouldExtractUserIdFromSubClaim() {
            JwtClaimsExtractor extractor = new JwtClaimsExtractor();
            Map<String, Object> claims = Map.of("sub", "user-123");

            assertThat(extractor.extractUserId(claims)).isEqualTo("user-123");
        }

        @Test
        @DisplayName("应从 preferred_username claim 提取用户名")
        void shouldExtractUsernameFromPreferredUsernameClaim() {
            JwtClaimsExtractor extractor = new JwtClaimsExtractor();
            Map<String, Object> claims = Map.of("preferred_username", "john.doe");

            assertThat(extractor.extractUsername(claims)).isEqualTo("john.doe");
        }

        @Test
        @DisplayName("应从 roles claim 提取角色集合")
        void shouldExtractRolesFromRolesClaim() {
            JwtClaimsExtractor extractor = new JwtClaimsExtractor();
            Map<String, Object> claims = Map.of("roles", List.of("ADMIN", "USER"));

            assertThat(extractor.extractRoles(claims)).containsExactlyInAnyOrder("ADMIN", "USER");
        }

        @Test
        @DisplayName("应从 permissions claim 提取权限集合")
        void shouldExtractPermissionsFromPermissionsClaim() {
            JwtClaimsExtractor extractor = new JwtClaimsExtractor();
            Map<String, Object> claims = Map.of("permissions", List.of("user:read", "user:write"));

            assertThat(extractor.extractPermissions(claims)).containsExactlyInAnyOrder("user:read", "user:write");
        }

        @Test
        @DisplayName("应从 tenant_id claim 提取租户 ID")
        void shouldExtractTenantIdFromTenantIdClaim() {
            JwtClaimsExtractor extractor = new JwtClaimsExtractor();
            Map<String, Object> claims = Map.of("tenant_id", "tenant-001");

            assertThat(extractor.extractTenantId(claims)).isEqualTo("tenant-001");
        }

        @Test
        @DisplayName("应从 token_type claim 提取 Token 类型")
        void shouldExtractTokenTypeFromTokenTypeClaim() {
            JwtClaimsExtractor extractor = new JwtClaimsExtractor();
            Map<String, Object> claims = Map.of("token_type", "access");

            assertThat(extractor.extractTokenType(claims)).isEqualTo("access");
        }

        @Test
        @DisplayName("应从 iss claim 提取 Issuer")
        void shouldExtractIssuerFromIssClaim() {
            JwtClaimsExtractor extractor = new JwtClaimsExtractor();
            Map<String, Object> claims = Map.of("iss", "https://auth.example.com");

            assertThat(extractor.extractIssuer(claims)).isEqualTo("https://auth.example.com");
        }
    }

    // ==================== 自定义配置测试 ====================

    @Nested
    @DisplayName("自定义配置提取")
    class CustomConfigTests {

        @Test
        @DisplayName("应使用自定义 claim 名称提取字段")
        void shouldExtractFieldsWithCustomClaimNames() {
            JwtClaimsConfig config = new JwtClaimsConfig();
            config.setUserIdClaim("user_id");
            config.setUsernameClaim("name");
            config.setRolesClaim("custom_roles");
            config.setPermissionsClaim("custom_perms");
            config.setTenantIdClaim("custom_tenant");

            JwtClaimsExtractor extractor = new JwtClaimsExtractor(config);
            Map<String, Object> claims = Map.of(
                    "user_id", "u-001",
                    "name", "alice",
                    "custom_roles", List.of("MANAGER"),
                    "custom_perms", List.of("doc:read"),
                    "custom_tenant", "t-001"
            );

            assertThat(extractor.extractUserId(claims)).isEqualTo("u-001");
            assertThat(extractor.extractUsername(claims)).isEqualTo("alice");
            assertThat(extractor.extractRoles(claims)).containsExactly("MANAGER");
            assertThat(extractor.extractPermissions(claims)).containsExactly("doc:read");
            assertThat(extractor.extractTenantId(claims)).isEqualTo("t-001");
        }
    }

    // ==================== Null/缺失值测试 ====================

    @Nested
    @DisplayName("缺失值处理")
    class MissingValueTests {

        @Test
        @DisplayName("claim 缺失时应返回 null（字符串字段）")
        void shouldReturnNullWhenUserIdClaimMissing() {
            JwtClaimsExtractor extractor = new JwtClaimsExtractor();
            Map<String, Object> claims = Map.of();

            assertThat(extractor.extractUserId(claims)).isNull();
            assertThat(extractor.extractUsername(claims)).isNull();
            assertThat(extractor.extractTenantId(claims)).isNull();
            assertThat(extractor.extractTokenType(claims)).isNull();
            assertThat(extractor.extractIssuer(claims)).isNull();
        }

        @Test
        @DisplayName("claim 缺失时应返回空集合（集合字段）")
        void shouldReturnEmptySetWhenCollectionClaimMissing() {
            JwtClaimsExtractor extractor = new JwtClaimsExtractor();
            Map<String, Object> claims = Map.of();

            assertThat(extractor.extractRoles(claims)).isEmpty();
            assertThat(extractor.extractPermissions(claims)).isEmpty();
        }
    }

    // ==================== 类型处理测试 ====================

    @Nested
    @DisplayName("值类型处理")
    class ValueTypeTests {

        @Test
        @DisplayName("整数值应转为字符串")
        void shouldHandleIntegerClaimValueAsString() {
            JwtClaimsExtractor extractor = new JwtClaimsExtractor();
            Map<String, Object> claims = Map.of("sub", 12345);

            assertThat(extractor.extractUserId(claims)).isEqualTo("12345");
        }

        @Test
        @DisplayName("逗号分隔字符串应转为集合")
        void shouldHandleCommaSeparatedStringForRoles() {
            JwtClaimsExtractor extractor = new JwtClaimsExtractor();
            Map<String, Object> claims = Map.of("roles", "ADMIN,USER");

            assertThat(extractor.extractRoles(claims)).containsExactlyInAnyOrder("ADMIN", "USER");
        }

        @Test
        @DisplayName("List 值应转为集合")
        void shouldHandleListForPermissions() {
            JwtClaimsExtractor extractor = new JwtClaimsExtractor();
            Map<String, Object> claims = Map.of("permissions", List.of("user:read", "user:write", "user:delete"));

            assertThat(extractor.extractPermissions(claims)).containsExactlyInAnyOrder("user:read", "user:write", "user:delete");
        }

        @Test
        @DisplayName("空 List 应转为空集合")
        void shouldHandleEmptyListForRoles() {
            JwtClaimsExtractor extractor = new JwtClaimsExtractor();
            Map<String, Object> claims = Map.of("roles", List.of());

            assertThat(extractor.extractRoles(claims)).isEmpty();
        }
    }

    // ==================== Bearer Token 提取测试 ====================

    @Nested
    @DisplayName("Bearer Token 提取")
    class BearerTokenTests {

        @Test
        @DisplayName("应从有效的 Authorization 头提取 Bearer Token")
        void shouldExtractBearerTokenFromValidHeader() {
            String token = JwtClaimsExtractor.extractBearerToken("Bearer abc123");

            assertThat(token).isEqualTo("abc123");
        }

        @Test
        @DisplayName("非 Bearer 格式应返回 null")
        void shouldReturnNullWhenNoBearerPrefix() {
            String token = JwtClaimsExtractor.extractBearerToken("Basic abc123");

            assertThat(token).isNull();
        }

        @Test
        @DisplayName("null 头应返回 null")
        void shouldReturnNullWhenNullHeader() {
            String token = JwtClaimsExtractor.extractBearerToken((String) null);

            assertThat(token).isNull();
        }

        @Test
        @DisplayName("空字符串头应返回 null")
        void shouldReturnNullWhenEmptyHeader() {
            String token = JwtClaimsExtractor.extractBearerToken("");

            assertThat(token).isNull();
        }

        @Test
        @DisplayName("应从 HttpServletRequest 提取 Bearer Token")
        void shouldExtractBearerTokenFromHttpServletRequest() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer xyz789");

            String token = JwtClaimsExtractor.extractBearerToken(request);

            assertThat(token).isEqualTo("xyz789");
        }

        @Test
        @DisplayName("无 Authorization 头应返回 null")
        void shouldReturnNullWhenRequestHasNoAuthorization() {
            MockHttpServletRequest request = new MockHttpServletRequest();

            String token = JwtClaimsExtractor.extractBearerToken(request);

            assertThat(token).isNull();
        }
    }

    // ==================== 构造函数测试 ====================

    @Nested
    @DisplayName("构造函数")
    class ConstructorTests {

        @Test
        @DisplayName("null 配置应使用默认值")
        void shouldUseDefaultConfigWhenNullProvided() {
            JwtClaimsExtractor extractor = new JwtClaimsExtractor(null);

            assertThat(extractor.getClaimsConfig().getUserIdClaim()).isEqualTo("sub");
            assertThat(extractor.getClaimsConfig().getTenantIdClaim()).isEqualTo("tenant_id");
        }

        @Test
        @DisplayName("应返回使用的配置")
        void shouldReturnClaimsConfig() {
            JwtClaimsConfig config = new JwtClaimsConfig();
            config.setTenantIdClaim("custom_tenant");
            JwtClaimsExtractor extractor = new JwtClaimsExtractor(config);

            assertThat(extractor.getClaimsConfig().getTenantIdClaim()).isEqualTo("custom_tenant");
        }
    }

    // ==================== 综合场景测试 ====================

    @Test
    @DisplayName("应从完整 claims Map 提取所有字段")
    void shouldExtractAllFieldsFromCompleteClaimsMap() {
        JwtClaimsExtractor extractor = new JwtClaimsExtractor();
        Map<String, Object> claims = Map.of(
                "sub", "user-001",
                "preferred_username", "john",
                "roles", List.of("ADMIN", "USER"),
                "permissions", List.of("user:read", "user:write"),
                "tenant_id", "tenant-001",
                "token_type", "access",
                "iss", "https://auth.example.com"
        );

        assertThat(extractor.extractUserId(claims)).isEqualTo("user-001");
        assertThat(extractor.extractUsername(claims)).isEqualTo("john");
        assertThat(extractor.extractRoles(claims)).containsExactlyInAnyOrder("ADMIN", "USER");
        assertThat(extractor.extractPermissions(claims)).containsExactlyInAnyOrder("user:read", "user:write");
        assertThat(extractor.extractTenantId(claims)).isEqualTo("tenant-001");
        assertThat(extractor.extractTokenType(claims)).isEqualTo("access");
        assertThat(extractor.extractIssuer(claims)).isEqualTo("https://auth.example.com");
    }
}