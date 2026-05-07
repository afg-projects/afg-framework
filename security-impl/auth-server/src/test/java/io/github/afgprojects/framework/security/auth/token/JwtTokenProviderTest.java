package io.github.afgprojects.framework.security.auth.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.nimbusds.jwt.JWTClaimsSet;

/**
 * JwtTokenProvider 测试
 */
@DisplayName("JwtTokenProvider 测试")
class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(
                "test-signing-key-must-be-at-least-256-bits-long",
                "https://auth.example.com",
                Duration.ofHours(2));
    }

    @Nested
    @DisplayName("Token 生成测试")
    class TokenGenerationTests {

        @Test
        @DisplayName("应该生成有效的 Access Token")
        void shouldGenerateValidAccessToken() {
            // given
            String subject = "user-123";
            List<String> roles = List.of("ADMIN", "USER");
            List<String> permissions = List.of("read:user", "write:user");
            Map<String, Object> additionalClaims = Map.of("client_id", "test-client");

            // when
            String token = tokenProvider.generateAccessToken(subject, roles, permissions, additionalClaims);

            // then
            assertThat(token).isNotBlank();
            assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts
        }

        @Test
        @DisplayName("应该生成有效的 Refresh Token")
        void shouldGenerateValidRefreshToken() {
            // given
            String subject = "user-456";

            // when
            String token = tokenProvider.generateRefreshToken(subject);

            // then
            assertThat(token).isNotBlank();
            assertThat(token.split("\\.")).hasSize(3);
        }

        @Test
        @DisplayName("生成的 Token 应该包含正确的 subject")
        void shouldContainCorrectSubject() throws Exception {
            // given
            String subject = "test-user";

            // when
            String token = tokenProvider.generateAccessToken(subject, List.of(), List.of(), Map.of());
            JWTClaimsSet claims = tokenProvider.parseToken(token);

            // then
            assertThat(claims.getSubject()).isEqualTo(subject);
        }

        @Test
        @DisplayName("生成的 Token 应该包含正确的 issuer")
        void shouldContainCorrectIssuer() throws Exception {
            // when
            String token = tokenProvider.generateAccessToken("user", List.of(), List.of(), Map.of());
            JWTClaimsSet claims = tokenProvider.parseToken(token);

            // then
            assertThat(claims.getIssuer().toString()).isEqualTo("https://auth.example.com");
        }

        @Test
        @DisplayName("生成的 Access Token 应该包含角色")
        void shouldContainRolesInAccessToken() throws Exception {
            // given
            List<String> roles = List.of("ADMIN", "MANAGER");

            // when
            String token = tokenProvider.generateAccessToken("user", roles, List.of(), Map.of());
            JWTClaimsSet claims = tokenProvider.parseToken(token);

            // then
            @SuppressWarnings("unchecked")
            List<String> tokenRoles = (List<String>) claims.getClaim("roles");
            assertThat(tokenRoles).containsExactlyElementsOf(roles);
        }

        @Test
        @DisplayName("生成的 Access Token 应该包含权限")
        void shouldContainPermissionsInAccessToken() throws Exception {
            // given
            List<String> permissions = List.of("read:resource", "write:resource");

            // when
            String token = tokenProvider.generateAccessToken("user", List.of(), permissions, Map.of());
            JWTClaimsSet claims = tokenProvider.parseToken(token);

            // then
            @SuppressWarnings("unchecked")
            List<String> tokenPermissions = (List<String>) claims.getClaim("permissions");
            assertThat(tokenPermissions).containsExactlyElementsOf(permissions);
        }
    }

    @Nested
    @DisplayName("Token 验证测试")
    class TokenValidationTests {

        @Test
        @DisplayName("应该验证有效的 Token")
        void shouldValidateValidToken() {
            // given
            String token = tokenProvider.generateAccessToken("user", List.of(), List.of(), Map.of());

            // when
            boolean isValid = tokenProvider.validateToken(token);

            // then
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("应该拒绝无效的 Token")
        void shouldRejectInvalidToken() {
            // given
            String invalidToken = "invalid.token.here";

            // when
            boolean isValid = tokenProvider.validateToken(invalidToken);

            // then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("应该拒绝过期的 Token")
        void shouldRejectExpiredToken() {
            // given - 创建一个已过期的 token provider
            JwtTokenProvider expiredProvider = new JwtTokenProvider(
                    "test-signing-key-must-be-at-least-256-bits-long",
                    "https://auth.example.com",
                    Duration.ofMillis(-1)); // 负值表示已过期

            String token = expiredProvider.generateAccessToken("user", List.of(), List.of(), Map.of());

            // when
            boolean isValid = expiredProvider.validateToken(token);

            // then
            assertThat(isValid).isFalse();
        }
    }

    @Nested
    @DisplayName("Token 解析测试")
    class TokenParsingTests {

        @Test
        @DisplayName("应该解析 Token 获取 claims")
        void shouldParseTokenAndGetClaims() throws Exception {
            // given
            String subject = "parse-test-user";
            Map<String, Object> additionalClaims = Map.of(
                    "custom_claim", "custom_value",
                    "numeric_claim", 123);

            // when
            String token = tokenProvider.generateAccessToken(subject, List.of(), List.of(), additionalClaims);
            JWTClaimsSet claims = tokenProvider.parseToken(token);

            // then
            assertThat(claims.getSubject()).isEqualTo(subject);
            assertThat(claims.getStringClaim("custom_claim")).isEqualTo("custom_value");
            assertThat(claims.getIntegerClaim("numeric_claim")).isEqualTo(123);
        }

        @Test
        @DisplayName("应该抛出异常当 Token 无效")
        void shouldThrowExceptionWhenTokenInvalid() {
            // given
            String invalidToken = "not.a.valid.jwt";

            // when & then
            assertThatThrownBy(() -> tokenProvider.parseToken(invalidToken))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("应该获取 Token 过期时间")
        void shouldGetTokenExpirationTime() throws Exception {
            // given
            Instant beforeGeneration = Instant.now();

            // when
            String token = tokenProvider.generateAccessToken("user", List.of(), List.of(), Map.of());
            JWTClaimsSet claims = tokenProvider.parseToken(token);
            Instant expirationTime = claims.getExpirationTime().toInstant();

            // then
            assertThat(expirationTime).isAfter(beforeGeneration);
        }
    }

    @Nested
    @DisplayName("Subject 提取测试")
    class SubjectExtractionTests {

        @Test
        @DisplayName("应该从 Token 中提取 subject")
        void shouldExtractSubjectFromToken() throws Exception {
            // given
            String expectedSubject = "subject-test-user";
            String token = tokenProvider.generateAccessToken(expectedSubject, List.of(), List.of(), Map.of());

            // when
            String subject = tokenProvider.extractSubject(token);

            // then
            assertThat(subject).isEqualTo(expectedSubject);
        }

        @Test
        @DisplayName("应该从 Refresh Token 中提取 subject")
        void shouldExtractSubjectFromRefreshToken() throws Exception {
            // given
            String expectedSubject = "refresh-test-user";
            String token = tokenProvider.generateRefreshToken(expectedSubject);

            // when
            String subject = tokenProvider.extractSubject(token);

            // then
            assertThat(subject).isEqualTo(expectedSubject);
        }
    }
}