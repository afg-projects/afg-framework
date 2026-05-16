package io.github.afgprojects.framework.security.auth.login;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.afgprojects.framework.security.auth.login.config.TokenProperties;
import io.github.afgprojects.framework.security.core.storage.AfgRefreshTokenStorage;
import io.github.afgprojects.framework.security.core.storage.AfgTokenBlacklist;

/**
 * JwtTokenService 测试。
 *
 * <p>测试 JWT Token 服务的生成、验证、提取和撤销功能。
 */
@DisplayName("JwtTokenService 测试")
@ExtendWith(MockitoExtension.class)
class JwtTokenServiceTest {

    private static final String SIGNING_KEY = "test-signing-key-must-be-at-least-256-bits-long-for-hs256";
    private static final String ISSUER = "https://auth.example.com";
    private static final Duration ACCESS_TOKEN_TTL = Duration.ofHours(2);
    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(7);

    @Mock
    private AfgRefreshTokenStorage refreshTokenStorage;

    @Mock
    private AfgTokenBlacklist tokenBlacklist;

    private JwtTokenService tokenService;

    @BeforeEach
    void setUp() {
        TokenProperties properties = new TokenProperties();
        properties.setSigningKey(SIGNING_KEY);
        properties.setIssuer(ISSUER);
        properties.setAccessTokenTtl(ACCESS_TOKEN_TTL);
        properties.setRefreshTokenTtl(REFRESH_TOKEN_TTL);
        properties.setIncludeRoles(true);
        properties.setIncludePermissions(true);

        tokenService = new JwtTokenService(properties, refreshTokenStorage, tokenBlacklist);
    }

    @Nested
    @DisplayName("Access Token 生成测试")
    class AccessTokenGenerationTests {

        @Test
        @DisplayName("应该生成有效的 Access Token")
        void shouldGenerateValidAccessToken() {
            // given
            String userId = "user-123";
            String username = "testuser";
            Set<String> roles = Set.of("ADMIN", "USER");
            Set<String> permissions = Set.of("read:user", "write:user");
            String tenantId = "tenant-001";

            // when
            String token = tokenService.generateAccessToken(userId, username, roles, permissions, tenantId);

            // then
            assertThat(token).isNotBlank();
            assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts
        }

        @Test
        @DisplayName("生成的 Access Token 应该包含正确的用户信息")
        void shouldContainCorrectUserInfoInAccessToken() {
            // given
            String userId = "user-456";
            String username = "johndoe";
            Set<String> roles = Set.of("MANAGER");
            Set<String> permissions = Set.of("read:report", "write:report");
            String tenantId = "tenant-002";

            // when
            String token = tokenService.generateAccessToken(userId, username, roles, permissions, tenantId);

            // then
            assertThat(tokenService.extractUserId(token)).isEqualTo(userId);
            assertThat(tokenService.extractUsername(token)).isEqualTo(username);
            assertThat(tokenService.extractRoles(token)).containsExactlyInAnyOrderElementsOf(roles);
            assertThat(tokenService.extractPermissions(token)).containsExactlyInAnyOrderElementsOf(permissions);
            assertThat(tokenService.extractTenantId(token)).isEqualTo(tenantId);
        }

        @Test
        @DisplayName("应该支持无租户的 Access Token")
        void shouldSupportAccessTokenWithoutTenant() {
            // given
            String userId = "user-789";
            String username = "singleTenantUser";
            Set<String> roles = Set.of("USER");
            Set<String> permissions = Set.of("read:profile");

            // when
            String token = tokenService.generateAccessToken(userId, username, roles, permissions, null);

            // then
            assertThat(token).isNotBlank();
            assertThat(tokenService.extractUserId(token)).isEqualTo(userId);
            assertThat(tokenService.extractTenantId(token)).isNull();
        }
    }

    @Nested
    @DisplayName("Access Token 验证测试")
    class AccessTokenValidationTests {

        @Test
        @DisplayName("应该验证有效的 Access Token")
        void shouldValidateValidAccessToken() {
            // given
            String token = tokenService.generateAccessToken(
                    "user-123", "testuser", Set.of("USER"), Set.of("read:user"), null);

            // when
            boolean isValid = tokenService.validateAccessToken(token);

            // then
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("应该拒绝无效的 Access Token")
        void shouldRejectInvalidAccessToken() {
            // given
            String invalidToken = "invalid.token.here";

            // when
            boolean isValid = tokenService.validateAccessToken(invalidToken);

            // then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("应该拒绝过期的 Access Token")
        void shouldRejectExpiredAccessToken() {
            // given - 创建一个使用已过期 TTL 的 token service
            TokenProperties expiredProperties = new TokenProperties();
            expiredProperties.setSigningKey(SIGNING_KEY);
            expiredProperties.setIssuer(ISSUER);
            expiredProperties.setAccessTokenTtl(Duration.ofMillis(-1));
            expiredProperties.setRefreshTokenTtl(REFRESH_TOKEN_TTL);

            JwtTokenService expiredService = new JwtTokenService(
                    expiredProperties, refreshTokenStorage, tokenBlacklist);

            String token = expiredService.generateAccessToken(
                    "user-123", "testuser", Set.of("USER"), Set.of("read:user"), null);

            // when
            boolean isValid = expiredService.validateAccessToken(token);

            // then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("应该拒绝黑名单中的 Access Token")
        void shouldRejectBlacklistedAccessToken() {
            // given
            String token = tokenService.generateAccessToken(
                    "user-123", "testuser", Set.of("USER"), Set.of("read:user"), null);
            String tokenHash = sha256(token);

            when(tokenBlacklist.isBlacklisted(tokenHash)).thenReturn(true);

            // when
            boolean isValid = tokenService.validateAccessToken(token);

            // then
            assertThat(isValid).isFalse();
            verify(tokenBlacklist).isBlacklisted(tokenHash);
        }

        @Test
        @DisplayName("应该拒绝签名错误的 Access Token")
        void shouldRejectWrongSignatureAccessToken() {
            // given - 使用不同的签名密钥生成 token
            TokenProperties otherProperties = new TokenProperties();
            otherProperties.setSigningKey("different-signing-key-at-least-256-bits-long-for-hs256");
            otherProperties.setIssuer(ISSUER);
            otherProperties.setAccessTokenTtl(ACCESS_TOKEN_TTL);
            otherProperties.setRefreshTokenTtl(REFRESH_TOKEN_TTL);

            JwtTokenService otherService = new JwtTokenService(
                    otherProperties, refreshTokenStorage, tokenBlacklist);

            String token = otherService.generateAccessToken(
                    "user-123", "testuser", Set.of("USER"), Set.of("read:user"), null);

            // when
            boolean isValid = tokenService.validateAccessToken(token);

            // then
            assertThat(isValid).isFalse();
        }
    }

    @Nested
    @DisplayName("Refresh Token 生成测试")
    class RefreshTokenGenerationTests {

        @Test
        @DisplayName("应该生成有效的 Refresh Token")
        void shouldGenerateValidRefreshToken() {
            // given
            String userId = "user-123";
            String tenantId = "tenant-001";

            // when
            String refreshToken = tokenService.generateRefreshToken(userId, tenantId);

            // then
            assertThat(refreshToken).isNotBlank();
            assertThat(refreshToken.split("\\.")).hasSize(3);
        }

        @Test
        @DisplayName("生成的 Refresh Token 应该保存到存储")
        void shouldSaveRefreshTokenToStorage() {
            // given
            String userId = "user-456";
            String tenantId = "tenant-002";

            // when
            String refreshToken = tokenService.generateRefreshToken(userId, tenantId);

            // then
            verify(refreshTokenStorage).save(
                    anyString(), // tokenId
                    anyString(), // tokenHash
                    eq(userId),
                    eq(tenantId),
                    any(),
                    any(),
                    any(LocalDateTime.class));
        }

        @Test
        @DisplayName("应该支持无租户的 Refresh Token")
        void shouldSupportRefreshTokenWithoutTenant() {
            // given
            String userId = "user-789";

            // when
            String refreshToken = tokenService.generateRefreshToken(userId, null);

            // then
            assertThat(refreshToken).isNotBlank();
            verify(refreshTokenStorage).save(
                    anyString(),
                    anyString(),
                    eq(userId),
                    eq(null),
                    any(),
                    any(),
                    any(LocalDateTime.class));
        }
    }

    @Nested
    @DisplayName("Refresh Token 验证测试")
    class RefreshTokenValidationTests {

        @Test
        @DisplayName("应该验证有效的 Refresh Token")
        void shouldValidateValidRefreshToken() {
            // given
            String userId = "user-123";
            String tenantId = "tenant-001";
            String refreshToken = tokenService.generateRefreshToken(userId, tenantId);
            String tokenHash = sha256(refreshToken);

            when(refreshTokenStorage.findByTokenHash(tokenHash))
                    .thenReturn(Optional.of(new AfgRefreshTokenStorage.RefreshTokenInfo(
                            "token-id-123",
                            tokenHash,
                            userId,
                            tenantId,
                            null,
                            null,
                            LocalDateTime.now().plusDays(7),
                            LocalDateTime.now())));

            // when
            boolean isValid = tokenService.validateRefreshToken(refreshToken);

            // then
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("应该拒绝无效的 Refresh Token")
        void shouldRejectInvalidRefreshToken() {
            // given
            String invalidToken = "invalid.refresh.token";

            // when
            boolean isValid = tokenService.validateRefreshToken(invalidToken);

            // then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("应该拒绝存储中不存在的 Refresh Token")
        void shouldRejectNonExistentRefreshToken() {
            // given
            String userId = "user-123";
            String refreshToken = tokenService.generateRefreshToken(userId, null);
            String tokenHash = sha256(refreshToken);

            when(refreshTokenStorage.findByTokenHash(tokenHash))
                    .thenReturn(Optional.empty());

            // when
            boolean isValid = tokenService.validateRefreshToken(refreshToken);

            // then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("应该拒绝过期的 Refresh Token")
        void shouldRejectExpiredRefreshToken() {
            // given - 创建一个使用已过期 TTL 的 token service
            TokenProperties expiredProperties = new TokenProperties();
            expiredProperties.setSigningKey(SIGNING_KEY);
            expiredProperties.setIssuer(ISSUER);
            expiredProperties.setAccessTokenTtl(ACCESS_TOKEN_TTL);
            expiredProperties.setRefreshTokenTtl(Duration.ofMillis(-1));

            JwtTokenService expiredService = new JwtTokenService(
                    expiredProperties, refreshTokenStorage, tokenBlacklist);

            String refreshToken = expiredService.generateRefreshToken("user-123", null);

            // when
            boolean isValid = expiredService.validateRefreshToken(refreshToken);

            // then
            assertThat(isValid).isFalse();
        }
    }

    @Nested
    @DisplayName("Token 黑名单测试")
    class TokenBlacklistTests {

        @Test
        @DisplayName("应该将 Token 加入黑名单")
        void shouldInvalidateToken() {
            // given
            String token = tokenService.generateAccessToken(
                    "user-123", "testuser", Set.of("USER"), Set.of("read:user"), null);
            String tokenHash = sha256(token);

            // when
            tokenService.invalidateToken(token);

            // then
            verify(tokenBlacklist).addToBlacklist(
                    eq(tokenHash),
                    eq("user-123"),
                    eq("revoked"),
                    any(Duration.class));
        }

        @Test
        @DisplayName("黑名单中的 Token 验证应该失败")
        void blacklistedTokenValidationShouldFail() {
            // given
            String token = tokenService.generateAccessToken(
                    "user-123", "testuser", Set.of("USER"), Set.of("read:user"), null);
            String tokenHash = sha256(token);

            when(tokenBlacklist.isBlacklisted(tokenHash)).thenReturn(true);

            // when
            boolean isValid = tokenService.validateAccessToken(token);

            // then
            assertThat(isValid).isFalse();
        }
    }

    @Nested
    @DisplayName("用户所有 Token 失效测试")
    class InvalidateAllTokensTests {

        @Test
        @DisplayName("应该使用户所有 Token 失效")
        void shouldInvalidateAllUserTokens() {
            // given
            String userId = "user-123";

            // when
            tokenService.invalidateAllTokens(userId);

            // then
            verify(tokenBlacklist).blacklistAllUserTokens(eq(userId), any(Duration.class));
            verify(refreshTokenStorage).deleteByUserId(userId);
        }
    }

    @Nested
    @DisplayName("TTL 测试")
    class TtlTests {

        @Test
        @DisplayName("应该返回正确的 Access Token TTL")
        void shouldReturnCorrectAccessTokenTtl() {
            // when
            long ttl = tokenService.getAccessTokenTtl();

            // then
            assertThat(ttl).isEqualTo(ACCESS_TOKEN_TTL.getSeconds());
        }

        @Test
        @DisplayName("应该返回正确的 Refresh Token TTL")
        void shouldReturnCorrectRefreshTokenTtl() {
            // when
            long ttl = tokenService.getRefreshTokenTtl();

            // then
            assertThat(ttl).isEqualTo(REFRESH_TOKEN_TTL.getSeconds());
        }
    }

    @Nested
    @DisplayName("用户信息提取测试")
    class UserInfoExtractionTests {

        @Test
        @DisplayName("应该从 Access Token 中提取用户 ID")
        void shouldExtractUserIdFromAccessToken() {
            // given
            String userId = "user-123";
            String token = tokenService.generateAccessToken(
                    userId, "testuser", Set.of("USER"), Set.of("read:user"), null);

            // when
            String extractedUserId = tokenService.extractUserId(token);

            // then
            assertThat(extractedUserId).isEqualTo(userId);
        }

        @Test
        @DisplayName("应该从 Access Token 中提取用户名")
        void shouldExtractUsernameFromAccessToken() {
            // given
            String username = "johndoe";
            String token = tokenService.generateAccessToken(
                    "user-123", username, Set.of("USER"), Set.of("read:user"), null);

            // when
            String extractedUsername = tokenService.extractUsername(token);

            // then
            assertThat(extractedUsername).isEqualTo(username);
        }

        @Test
        @DisplayName("应该从 Access Token 中提取角色")
        void shouldExtractRolesFromAccessToken() {
            // given
            Set<String> roles = Set.of("ADMIN", "MANAGER", "USER");
            String token = tokenService.generateAccessToken(
                    "user-123", "testuser", roles, Set.of("read:user"), null);

            // when
            Set<String> extractedRoles = tokenService.extractRoles(token);

            // then
            assertThat(extractedRoles).containsExactlyInAnyOrderElementsOf(roles);
        }

        @Test
        @DisplayName("应该从 Access Token 中提取权限")
        void shouldExtractPermissionsFromAccessToken() {
            // given
            Set<String> permissions = Set.of("read:user", "write:user", "delete:user");
            String token = tokenService.generateAccessToken(
                    "user-123", "testuser", Set.of("USER"), permissions, null);

            // when
            Set<String> extractedPermissions = tokenService.extractPermissions(token);

            // then
            assertThat(extractedPermissions).containsExactlyInAnyOrderElementsOf(permissions);
        }

        @Test
        @DisplayName("应该从 Access Token 中提取租户 ID")
        void shouldExtractTenantIdFromAccessToken() {
            // given
            String tenantId = "tenant-001";
            String token = tokenService.generateAccessToken(
                    "user-123", "testuser", Set.of("USER"), Set.of("read:user"), tenantId);

            // when
            String extractedTenantId = tokenService.extractTenantId(token);

            // then
            assertThat(extractedTenantId).isEqualTo(tenantId);
        }

        @Test
        @DisplayName("无效 Token 应该返回 null 用户 ID")
        void invalidTokenShouldReturnNullUserId() {
            // given
            String invalidToken = "invalid.token";

            // when
            String userId = tokenService.extractUserId(invalidToken);

            // then
            assertThat(userId).isNull();
        }

        @Test
        @DisplayName("无效 Token 应该返回空角色集合")
        void invalidTokenShouldReturnEmptyRoles() {
            // given
            String invalidToken = "invalid.token";

            // when
            Set<String> roles = tokenService.extractRoles(invalidToken);

            // then
            assertThat(roles).isEmpty();
        }

        @Test
        @DisplayName("无效 Token 应该返回空权限集合")
        void invalidTokenShouldReturnEmptyPermissions() {
            // given
            String invalidToken = "invalid.token";

            // when
            Set<String> permissions = tokenService.extractPermissions(invalidToken);

            // then
            assertThat(permissions).isEmpty();
        }
    }

    @Nested
    @DisplayName("配置测试")
    class ConfigurationTests {

        @Test
        @DisplayName("应该拒绝过短的签名密钥")
        void shouldRejectShortSigningKey() {
            // given
            TokenProperties properties = new TokenProperties();
            properties.setSigningKey("short-key"); // Less than 32 characters
            properties.setIssuer(ISSUER);

            // when & then
            assertThatThrownBy(() -> new JwtTokenService(properties, refreshTokenStorage, tokenBlacklist))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Signing key must be at least 256 bits");
        }

        @Test
        @DisplayName("应该拒绝 null 签名密钥")
        void shouldRejectNullSigningKey() {
            // given
            TokenProperties properties = new TokenProperties();
            properties.setSigningKey(null);
            properties.setIssuer(ISSUER);

            // when & then
            assertThatThrownBy(() -> new JwtTokenService(properties, refreshTokenStorage, tokenBlacklist))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Signing key must be at least 256 bits");
        }
    }

    /**
     * 计算字符串的 SHA-256 哈希值
     */
    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute SHA-256 hash", e);
        }
    }
}