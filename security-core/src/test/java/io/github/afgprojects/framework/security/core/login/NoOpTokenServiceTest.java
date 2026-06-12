package io.github.afgprojects.framework.security.core.login;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NoOpTokenService 测试
 */
@DisplayName("NoOpTokenService 测试")
class NoOpTokenServiceTest {

    private NoOpTokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new NoOpTokenService();
    }

    @Nested
    @DisplayName("令牌生成")
    class GenerateTests {

        @Test
        @DisplayName("generateAccessToken 应返回 noop 前缀的令牌")
        void shouldReturnNoopAccessToken() {
            String token = tokenService.generateAccessToken("user1", "admin", Set.of("ADMIN"), Set.of("read"), "tenant1");

            assertThat(token).isEqualTo("noop-access-token");
        }

        @Test
        @DisplayName("generateRefreshToken 应返回 noop 前缀的令牌")
        void shouldReturnNoopRefreshToken() {
            String token = tokenService.generateRefreshToken("user1", "tenant1");

            assertThat(token).isEqualTo("noop-refresh-token");
        }
    }

    @Nested
    @DisplayName("令牌验证")
    class ValidateTests {

        @Test
        @DisplayName("validateAccessToken 应返回 false")
        void shouldReturnFalseOnValidateAccessToken() {
            assertThat(tokenService.validateAccessToken("any-token")).isFalse();
        }

        @Test
        @DisplayName("validateRefreshToken 应返回 false")
        void shouldReturnFalseOnValidateRefreshToken() {
            assertThat(tokenService.validateRefreshToken("any-token")).isFalse();
        }
    }

    @Nested
    @DisplayName("令牌提取")
    class ExtractTests {

        @Test
        @DisplayName("extractUserId 应返回 null")
        void shouldReturnNullUserId() {
            assertThat(tokenService.extractUserId("any-token")).isNull();
        }

        @Test
        @DisplayName("extractUsername 应返回 null")
        void shouldReturnNullUsername() {
            assertThat(tokenService.extractUsername("any-token")).isNull();
        }

        @Test
        @DisplayName("extractRoles 应返回空集合")
        void shouldReturnEmptyRoles() {
            assertThat(tokenService.extractRoles("any-token")).isEmpty();
        }

        @Test
        @DisplayName("extractPermissions 应返回空集合")
        void shouldReturnEmptyPermissions() {
            assertThat(tokenService.extractPermissions("any-token")).isEmpty();
        }

        @Test
        @DisplayName("extractTenantId 应返回 null")
        void shouldReturnNullTenantId() {
            assertThat(tokenService.extractTenantId("any-token")).isNull();
        }
    }

    @Nested
    @DisplayName("令牌撤销")
    class InvalidateTests {

        @Test
        @DisplayName("invalidateToken 应不抛异常")
        void shouldNotThrowOnInvalidateToken() {
            tokenService.invalidateToken("any-token");
        }

        @Test
        @DisplayName("invalidateAllTokens 应不抛异常")
        void shouldNotThrowOnInvalidateAllTokens() {
            tokenService.invalidateAllTokens("user1");
        }
    }

    @Nested
    @DisplayName("TTL 查询")
    class TtlTests {

        @Test
        @DisplayName("getAccessTokenTtl 应返回 0")
        void shouldReturnZeroAccessTokenTtl() {
            assertThat(tokenService.getAccessTokenTtl()).isZero();
        }

        @Test
        @DisplayName("getRefreshTokenTtl 应返回 0")
        void shouldReturnZeroRefreshTokenTtl() {
            assertThat(tokenService.getRefreshTokenTtl()).isZero();
        }
    }
}
