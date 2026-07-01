package io.github.afgprojects.framework.security.resource.jwt;

import io.github.afgprojects.framework.security.core.token.JwtClaimsConfig;
import io.github.afgprojects.framework.security.core.token.JwtClaimsExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JwtAfgAuthenticationConverter 单元测试
 *
 * <p>验证 Jwt 到 AfgAuthentication 的适配转换：从 permissions/roles claim 还原 authorities，
 * 并覆盖空 claim 不抛异常的边界。
 *
 * @since 1.1.0
 */
@DisplayName("JwtAfgAuthenticationConverter 测试")
class JwtAfgAuthenticationConverterTest {

    private JwtAfgAuthenticationConverter converter;

    @BeforeEach
    void setUp() {
        // 使用默认 claim 名称（与 DefaultTokenService 签发的 JWT 一致）
        JwtClaimsExtractor extractor = new JwtClaimsExtractor(new JwtClaimsConfig());
        converter = new JwtAfgAuthenticationConverter(new JwtAuthenticationConverter(extractor));
    }

    private Jwt buildJwt(Map<String, Object> claims) {
        return Jwt.withTokenValue("token-value")
            .header("alg", "RS256")
            .claims(c -> c.putAll(claims))
            // Jwt 要求有 issuer/subject/issuedAt/expiry 等基础字段才能正常构造
            .issuer("https://afg.test")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();
    }

    private Set<String> authorityNames(AbstractAuthenticationToken token) {
        return token.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toSet());
    }

    @Nested
    @DisplayName("权限与角色还原")
    class AuthoritiesRestoration {

        @Test
        @DisplayName("应从 permissions claim 还原权限码，并给 roles 加 ROLE_ 前缀")
        void shouldRestorePermissionsAndRolesFromClaims() {
            Jwt jwt = buildJwt(Map.of(
                "sub", "1",
                "preferred_username", "admin",
                "tenant_id", "default",
                "roles", List.of("admin"),
                "permissions", List.of("system:user:list", "system:role:list", "system:menu:list")
            ));

            AbstractAuthenticationToken token = converter.convert(jwt);

            assertThat(token).isInstanceOf(JwtAuthenticationToken.class);
            assertThat(authorityNames(token)).containsExactlyInAnyOrder(
                "system:user:list", "system:role:list", "system:menu:list", "ROLE_admin");
        }

        @Test
        @DisplayName("应以 username 作为认证令牌名称")
        void shouldUseUsernameAsPrincipalName() {
            Jwt jwt = buildJwt(Map.of(
                "sub", "42",
                "preferred_username", "alice",
                "tenant_id", "tenant-x",
                "roles", List.of("operator"),
                "permissions", List.of("system:user:list")
            ));

            AbstractAuthenticationToken token = converter.convert(jwt);

            assertThat(token.getName()).isEqualTo("alice");
            assertThat(authorityNames(token)).contains("system:user:list", "ROLE_operator");
        }
    }

    @Nested
    @DisplayName("边界场景")
    class EdgeCases {

        @Test
        @DisplayName("无 permissions/roles claim 时返回空 authorities 且不抛异常")
        void shouldReturnEmptyAuthoritiesWhenNoPermissionClaims() {
            Jwt jwt = buildJwt(Map.of(
                "sub", "1",
                "preferred_username", "noperm"
            ));

            AbstractAuthenticationToken token = converter.convert(jwt);

            assertThat(token).isInstanceOf(JwtAuthenticationToken.class);
            assertThat(token.getAuthorities()).isEmpty();
        }

        @Test
        @DisplayName("permissions 为空列表时返回空 authorities")
        void shouldReturnEmptyAuthoritiesWhenPermissionsClaimIsEmpty() {
            Jwt jwt = buildJwt(Map.of(
                "sub", "1",
                "preferred_username", "empty",
                "roles", List.of(),
                "permissions", List.of()
            ));

            AbstractAuthenticationToken token = converter.convert(jwt);

            assertThat(token).isInstanceOf(JwtAuthenticationToken.class);
            assertThat(token.getAuthorities()).isEmpty();
        }
    }
}
