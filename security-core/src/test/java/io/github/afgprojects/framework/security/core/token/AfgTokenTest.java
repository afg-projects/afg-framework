package io.github.afgprojects.framework.security.core.token;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.security.core.authentication.AfgAuthentication;
import io.github.afgprojects.framework.security.core.authentication.AfgUserDetails;

class AfgTokenTest {

    @Nested
    @DisplayName("AfgToken 接口测试")
    class AfgTokenInterfaceTests {

        @Test
        @DisplayName("应获取 Token 值")
        void shouldGetTokenValue() {
            AfgToken token = createTestToken("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...");

            assertThat(token.getTokenValue()).isEqualTo("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...");
        }

        @Test
        @DisplayName("应获取 Token 类型")
        void shouldGetTokenType() {
            AfgToken token = createTestToken("token-value");

            assertThat(token.getTokenType()).isEqualTo("access");
        }

        @Test
        @DisplayName("应获取签发时间")
        void shouldGetIssuedAt() {
            Instant issuedAt = Instant.now().minus(1, ChronoUnit.HOURS);
            AfgToken token = createTestTokenWithTimes("token-value", issuedAt, Instant.now().plus(1, ChronoUnit.HOURS));

            assertThat(token.getIssuedAt()).isNotNull();
        }

        @Test
        @DisplayName("应获取过期时间")
        void shouldGetExpiresAt() {
            Instant expiresAt = Instant.now().plus(1, ChronoUnit.HOURS);
            AfgToken token = createTestTokenWithTimes("token-value", Instant.now(), expiresAt);

            assertThat(token.getExpiresAt()).isNotNull();
        }

        @Test
        @DisplayName("应判断 Token 是否过期")
        void shouldCheckExpired() {
            // 未过期
            AfgToken validToken = createTestTokenWithTimes(
                    "token-value",
                    Instant.now().minus(1, ChronoUnit.HOURS),
                    Instant.now().plus(1, ChronoUnit.HOURS));
            assertThat(validToken.isExpired()).isFalse();

            // 已过期
            AfgToken expiredToken = createTestTokenWithTimes(
                    "token-value",
                    Instant.now().minus(2, ChronoUnit.HOURS),
                    Instant.now().minus(1, ChronoUnit.HOURS));
            assertThat(expiredToken.isExpired()).isTrue();
        }

        @Test
        @DisplayName("应获取 Claims")
        void shouldGetClaims() {
            AfgToken token = createTestTokenWithClaims("token-value", Map.of("sub", "user-001", "role", "admin"));

            assertThat(token.getClaims()).containsEntry("sub", "user-001");
            assertThat(token.getClaims()).containsEntry("role", "admin");
        }

        @Test
        @DisplayName("应获取指定 Claim")
        void shouldGetClaim() {
            AfgToken token = createTestTokenWithClaims("token-value", Map.of("sub", "user-001"));

            assertThat(token.getClaim("sub")).isEqualTo("user-001");
        }
    }

    @Nested
    @DisplayName("AfgTokenProvider 接口测试")
    class AfgTokenProviderInterfaceTests {

        @Test
        @DisplayName("应生成 Access Token")
        void shouldGenerateAccessToken() {
            AfgTokenProvider provider = createTestTokenProvider();
            AfgAuthentication auth = createTestAuthentication("user-001", "testuser");

            AfgToken token = provider.generateAccessToken(auth);

            assertThat(token).isNotNull();
            assertThat(token.getTokenType()).isEqualTo("access");
        }

        @Test
        @DisplayName("应生成 Refresh Token")
        void shouldGenerateRefreshToken() {
            AfgTokenProvider provider = createTestTokenProvider();
            AfgAuthentication auth = createTestAuthentication("user-001", "testuser");

            AfgToken token = provider.generateRefreshToken(auth);

            assertThat(token).isNotNull();
            assertThat(token.getTokenType()).isEqualTo("refresh");
        }

        @Test
        @DisplayName("应验证 Token")
        void shouldValidateToken() {
            AfgTokenProvider provider = createTestTokenProvider();

            AfgAuthentication auth = provider.validateToken("valid-token");

            assertThat(auth).isNotNull();
            assertThat(auth.getUserId()).isEqualTo("user-001");
        }

        @Test
        @DisplayName("验证无效 Token 应抛出异常")
        void shouldThrowExceptionForInvalidToken() {
            AfgTokenProvider provider = createTestTokenProvider();

            org.junit.jupiter.api.Assertions.assertThrows(
                    TokenValidationException.class, () -> provider.validateToken("invalid-token"));
        }

        @Test
        @DisplayName("应判断 Token 是否有效")
        void shouldCheckValidToken() {
            AfgTokenProvider provider = createTestTokenProvider();

            assertThat(provider.isValidToken("valid-token")).isTrue();
            assertThat(provider.isValidToken("invalid-token")).isFalse();
        }

        @Test
        @DisplayName("应撤销 Token")
        void shouldInvalidateToken() {
            AfgTokenProvider provider = createTestTokenProvider();

            provider.invalidateToken("valid-token");

            assertThat(provider.isValidToken("valid-token")).isFalse();
        }
    }

    @Nested
    @DisplayName("TokenValidationException 测试")
    class TokenValidationExceptionTests {

        @Test
        @DisplayName("应创建 Token 已过期异常")
        void shouldCreateExpiredException() {
            TokenValidationException ex = TokenValidationException.expired();

            assertThat(ex.getMessage()).contains("expired");
        }

        @Test
        @DisplayName("应创建 Token 无效异常")
        void shouldCreateInvalidException() {
            TokenValidationException ex = TokenValidationException.invalid();

            assertThat(ex.getMessage()).contains("invalid");
        }

        @Test
        @DisplayName("应创建 Token 签名无效异常")
        void shouldCreateInvalidSignatureException() {
            TokenValidationException ex = TokenValidationException.invalidSignature();

            assertThat(ex.getMessage()).contains("signature");
        }

        @Test
        @DisplayName("应创建 Token 已撤销异常")
        void shouldCreateRevokedException() {
            TokenValidationException ex = TokenValidationException.revoked();

            assertThat(ex.getMessage()).contains("revoked");
        }

        @Test
        @DisplayName("应使用消息和原因构造异常")
        void shouldCreateWithMessageAndCause() {
            RuntimeException cause = new RuntimeException("Original error");
            TokenValidationException ex = new TokenValidationException("Token error", cause);

            assertThat(ex.getMessage()).isEqualTo("Token error");
            assertThat(ex.getCause()).isSameAs(cause);
        }
    }

    // ========== 测试辅助方法 ==========

    private AfgToken createTestToken(String tokenValue) {
        return new AfgToken() {
            @Override
            public @NonNull String getTokenValue() {
                return tokenValue;
            }

            @Override
            public Instant getIssuedAt() {
                return Instant.now().minus(1, ChronoUnit.HOURS);
            }

            @Override
            public Instant getExpiresAt() {
                return Instant.now().plus(1, ChronoUnit.HOURS);
            }
        };
    }

    private AfgToken createTestTokenWithTimes(String tokenValue, Instant issuedAt, Instant expiresAt) {
        return new AfgToken() {
            @Override
            public @NonNull String getTokenValue() {
                return tokenValue;
            }

            @Override
            public Instant getIssuedAt() {
                return issuedAt;
            }

            @Override
            public Instant getExpiresAt() {
                return expiresAt;
            }
        };
    }

    private AfgToken createTestTokenWithClaims(String tokenValue, Map<String, Object> claims) {
        return new AfgToken() {
            @Override
            public @NonNull String getTokenValue() {
                return tokenValue;
            }

            @Override
            public Instant getIssuedAt() {
                return Instant.now();
            }

            @Override
            public Instant getExpiresAt() {
                return Instant.now().plus(1, ChronoUnit.HOURS);
            }

            @Override
            public Map<String, Object> getClaims() {
                return claims;
            }
        };
    }

    private AfgTokenProvider createTestTokenProvider() {
        return new AfgTokenProvider() {
            private final java.util.Set<String> invalidatedTokens = java.util.concurrent.ConcurrentHashMap.newKeySet();

            @Override
            public @NonNull AfgToken generateAccessToken(@NonNull AfgAuthentication authentication) {
                return new AfgToken() {
                    @Override
                    public @NonNull String getTokenValue() {
                        return "access-token-" + authentication.getUserId();
                    }

                    @Override
                    public Instant getIssuedAt() {
                        return Instant.now();
                    }

                    @Override
                    public Instant getExpiresAt() {
                        return Instant.now().plus(1, ChronoUnit.HOURS);
                    }

                    @Override
                    public @NonNull String getTokenType() {
                        return "access";
                    }
                };
            }

            @Override
            public @NonNull AfgToken generateRefreshToken(@NonNull AfgAuthentication authentication) {
                return new AfgToken() {
                    @Override
                    public @NonNull String getTokenValue() {
                        return "refresh-token-" + authentication.getUserId();
                    }

                    @Override
                    public Instant getIssuedAt() {
                        return Instant.now();
                    }

                    @Override
                    public Instant getExpiresAt() {
                        return Instant.now().plus(7, ChronoUnit.DAYS);
                    }

                    @Override
                    public @NonNull String getTokenType() {
                        return "refresh";
                    }
                };
            }

            @Override
            public @NonNull AfgAuthentication validateToken(@NonNull String tokenValue) {
                if (!"valid-token".equals(tokenValue) || invalidatedTokens.contains(tokenValue)) {
                    throw TokenValidationException.invalid();
                }
                return createTestAuthentication("user-001", "testuser");
            }

            @Override
            public void invalidateToken(@NonNull String tokenValue) {
                invalidatedTokens.add(tokenValue);
            }

            @Override
            public boolean isValidToken(@NonNull String tokenValue) {
                return "valid-token".equals(tokenValue) && !invalidatedTokens.contains(tokenValue);
            }
        };
    }

    private AfgAuthentication createTestAuthentication(String userId, String username) {
        return new AfgAuthentication() {
            @Override
            public @NonNull AfgUserDetails getUserDetails() {
                return new AfgUserDetails() {
                    @Override
                    public @NonNull String getUserId() {
                        return userId;
                    }

                    @Override
                    public @NonNull String getUsername() {
                        return username;
                    }

                    @Override
                    public @Nullable String getTenantId() {
                        return "tenant-001";
                    }

                    @Override
                    public java.util.@NonNull Set<String> getRoles() {
                        return java.util.Set.of("ROLE_USER");
                    }

                    @Override
                    public java.util.@NonNull Collection<? extends org.springframework.security.core.GrantedAuthority>
                            getAuthorities() {
                        return java.util.Set.of(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority("read"));
                    }

                    @Override
                    public String getPassword() {
                        return "$2a$10$encrypted";
                    }
                };
            }

            @Override
            public @NonNull Collection<? extends org.springframework.security.core.GrantedAuthority> getAuthorities() {
                return getUserDetails().getAuthorities();
            }

            @Override
            public @Nullable Object getCredentials() {
                return null;
            }

            @Override
            public @Nullable Object getDetails() {
                return null;
            }
        };
    }
}
