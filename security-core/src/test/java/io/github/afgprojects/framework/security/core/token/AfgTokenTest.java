package io.github.afgprojects.framework.security.core.token;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AfgToken 接口默认方法测试
 */
@DisplayName("AfgToken 接口默认方法测试")
class AfgTokenTest {

    @Nested
    @DisplayName("getTokenType 默认方法")
    class GetTokenTypeTests {

        @Test
        @DisplayName("默认应返回 access")
        void shouldReturnAccessByDefault() {
            AfgToken token = createToken("token-123", Instant.now(), Instant.now().plusSeconds(7200));

            assertThat(token.getTokenType()).isEqualTo("access");
        }
    }

    @Nested
    @DisplayName("isExpired 默认方法")
    class IsExpiredTests {

        @Test
        @DisplayName("过期时间在未来应判断为未过期")
        void shouldNotBeExpiredWhenExpiresAtIsInFuture() {
            AfgToken token = createToken("token-123", Instant.now(), Instant.now().plusSeconds(7200));

            assertThat(token.isExpired()).isFalse();
        }

        @Test
        @DisplayName("过期时间在过去应判断为已过期")
        void shouldBeExpiredWhenExpiresAtIsInPast() {
            AfgToken token = createToken("token-123", Instant.now().minusSeconds(7210), Instant.now().minusSeconds(10));

            assertThat(token.isExpired()).isTrue();
        }

        @Test
        @DisplayName("过期时间为 null 应判断为未过期")
        void shouldNotBeExpiredWhenExpiresAtIsNull() {
            AfgToken token = createToken("token-123", Instant.now(), null);

            assertThat(token.isExpired()).isFalse();
        }
    }

    @Nested
    @DisplayName("getClaims 默认方法")
    class GetClaimsTests {

        @Test
        @DisplayName("默认应返回空 Map")
        void shouldReturnEmptyMapByDefault() {
            AfgToken token = createToken("token-123", Instant.now(), Instant.now().plusSeconds(7200));

            assertThat(token.getClaims()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getClaim 默认方法")
    class GetClaimTests {

        @Test
        @DisplayName("默认应返回 null")
        void shouldReturnNullByDefault() {
            AfgToken token = createToken("token-123", Instant.now(), Instant.now().plusSeconds(7200));

            assertThat(token.getClaim("sub")).isNull();
        }

        @Test
        @DisplayName("有自定义 claims 时应返回对应值")
        void shouldReturnClaimValueWhenCustomClaimsPresent() {
            AfgToken token = new AfgToken() {
                @Override
                public String getTokenValue() {
                    return "token-123";
                }

                @Override
                public Instant getIssuedAt() {
                    return Instant.now();
                }

                @Override
                public Instant getExpiresAt() {
                    return Instant.now().plusSeconds(7200);
                }

                @Override
                public Map<String, Object> getClaims() {
                    return Map.of("sub", "user-001", "tenant_id", "tenant-001");
                }
            };

            assertThat(token.getClaim("sub")).isEqualTo("user-001");
            assertThat(token.getClaim("tenant_id")).isEqualTo("tenant-001");
            assertThat(token.getClaim("nonexistent")).isNull();
        }
    }

    /**
     * 创建 AfgToken 实例用于测试 default 方法
     */
    private AfgToken createToken(String tokenValue, Instant issuedAt, Instant expiresAt) {
        return new AfgToken() {
            @Override
            public String getTokenValue() {
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
}