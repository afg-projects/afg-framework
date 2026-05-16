package io.github.afgprojects.framework.security.auth.tenant.validator;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.github.afgprojects.framework.security.core.tenant.AfgTenantService;
import io.github.afgprojects.framework.security.core.tenant.Tenant;
import io.github.afgprojects.framework.security.core.tenant.TenantException;
import io.github.afgprojects.framework.security.core.tenant.TenantStatus;

/**
 * DefaultTenantValidator 测试。
 */
@ExtendWith(MockitoExtension.class)
class DefaultTenantValidatorTest {

    @Mock
    private AfgTenantService tenantService;

    private DefaultTenantValidator validator;

    @BeforeEach
    void setUp() {
        // 创建带短 TTL 的缓存用于测试
        Cache<String, Boolean> cache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(5))
                .maximumSize(1000)
                .build();

        validator = new DefaultTenantValidator(tenantService, cache);
    }

    @Nested
    @DisplayName("验证活跃租户")
    class ValidateActiveTenantTests {

        @Test
        @DisplayName("应通过活跃租户的验证")
        void shouldPassActiveTenant() {
            // Given
            String tenantId = "tenant-001";
            Tenant tenant = createActiveTenant(tenantId);
            when(tenantService.getTenant(tenantId)).thenReturn(tenant);

            // When & Then
            assertThatCode(() -> validator.validate(tenantId))
                    .doesNotThrowAnyException();

            verify(tenantService).getTenant(tenantId);
        }

        @Test
        @DisplayName("应缓存验证结果")
        void shouldCacheValidationResult() {
            // Given
            String tenantId = "tenant-001";
            Tenant tenant = createActiveTenant(tenantId);
            when(tenantService.getTenant(tenantId)).thenReturn(tenant);

            // When - 验证两次
            validator.validate(tenantId);
            validator.validate(tenantId);

            // Then - 只调用一次服务
            verify(tenantService, times(1)).getTenant(tenantId);
        }
    }

    @Nested
    @DisplayName("验证不存在的租户")
    class ValidateNonExistentTenantTests {

        @Test
        @DisplayName("应抛出租户不存在异常")
        void shouldThrowNotFoundException() {
            // Given
            String tenantId = "non-existent";
            when(tenantService.getTenant(tenantId)).thenReturn(null);

            // When & Then
            assertThatThrownBy(() -> validator.validate(tenantId))
                    .isInstanceOf(TenantException.class)
                    .hasMessageContaining("租户不存在");

            verify(tenantService).getTenant(tenantId);
        }

        @Test
        @DisplayName("应缓存不存在的结果")
        void shouldCacheNotFoundResult() {
            // Given
            String tenantId = "non-existent";
            when(tenantService.getTenant(tenantId)).thenReturn(null);

            // When - 第一次验证
            assertThatThrownBy(() -> validator.validate(tenantId))
                    .isInstanceOf(TenantException.class)
                    .hasMessageContaining("租户不存在");

            // When - 第二次验证（从缓存）
            assertThatThrownBy(() -> validator.validate(tenantId))
                    .isInstanceOf(TenantException.class)
                    .hasMessageContaining("租户已禁用"); // 缓存返回 false，抛出禁用异常

            // Then - 只调用一次服务
            verify(tenantService, times(1)).getTenant(tenantId);
        }
    }

    @Nested
    @DisplayName("验证已禁用的租户")
    class ValidateDisabledTenantTests {

        @Test
        @DisplayName("应抛出租户已禁用异常")
        void shouldThrowDisabledException() {
            // Given
            String tenantId = "tenant-001";
            Tenant tenant = createTenantWithStatus(tenantId, TenantStatus.DISABLED);
            when(tenantService.getTenant(tenantId)).thenReturn(tenant);

            // When & Then
            assertThatThrownBy(() -> validator.validate(tenantId))
                    .isInstanceOf(TenantException.class)
                    .hasMessageContaining("租户已禁用");

            verify(tenantService).getTenant(tenantId);
        }
    }

    @Nested
    @DisplayName("验证已暂停的租户")
    class ValidateSuspendedTenantTests {

        @Test
        @DisplayName("应抛出租户已禁用异常")
        void shouldThrowDisabledException() {
            // Given
            String tenantId = "tenant-001";
            Tenant tenant = createTenantWithStatus(tenantId, TenantStatus.SUSPENDED);
            when(tenantService.getTenant(tenantId)).thenReturn(tenant);

            // When & Then
            assertThatThrownBy(() -> validator.validate(tenantId))
                    .isInstanceOf(TenantException.class)
                    .hasMessageContaining("租户已禁用");

            verify(tenantService).getTenant(tenantId);
        }
    }

    @Nested
    @DisplayName("验证已过期的租户")
    class ValidateExpiredTenantTests {

        @Test
        @DisplayName("应抛出租户已过期异常")
        void shouldThrowExpiredException() {
            // Given
            String tenantId = "tenant-001";
            Tenant tenant = createExpiredTenant(tenantId);
            when(tenantService.getTenant(tenantId)).thenReturn(tenant);

            // When & Then
            assertThatThrownBy(() -> validator.validate(tenantId))
                    .isInstanceOf(TenantException.class)
                    .hasMessageContaining("租户已过期");

            verify(tenantService).getTenant(tenantId);
        }

        @Test
        @DisplayName("应通过即将过期但未过期的租户")
        void shouldPassTenantNotYetExpired() {
            // Given
            String tenantId = "tenant-001";
            Instant expiresAt = Instant.now().plus(1, ChronoUnit.HOURS);
            Tenant tenant = createTenantWithExpiration(tenantId, expiresAt);
            when(tenantService.getTenant(tenantId)).thenReturn(tenant);

            // When & Then
            assertThatCode(() -> validator.validate(tenantId))
                    .doesNotThrowAnyException();

            verify(tenantService).getTenant(tenantId);
        }
    }

    @Nested
    @DisplayName("缓存行为测试")
    class CacheBehaviorTests {

        @Test
        @DisplayName("缓存过期后应重新验证")
        void shouldRevalidateAfterCacheExpiry() {
            // Given - 使用极短的 TTL
            Cache<String, Boolean> shortTtlCache = Caffeine.newBuilder()
                    .expireAfterWrite(Duration.ofMillis(50))
                    .maximumSize(1000)
                    .build();

            DefaultTenantValidator shortTtlValidator = new DefaultTenantValidator(tenantService, shortTtlCache);

            String tenantId = "tenant-001";
            Tenant tenant = createActiveTenant(tenantId);
            when(tenantService.getTenant(tenantId)).thenReturn(tenant);

            // When - 第一次验证
            shortTtlValidator.validate(tenantId);

            // 等待缓存过期
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // 第二次验证
            shortTtlValidator.validate(tenantId);

            // Then - 应调用两次服务
            verify(tenantService, times(2)).getTenant(tenantId);
        }

        @Test
        @DisplayName("不同租户应分别缓存")
        void shouldCacheDifferentTenantsSeparately() {
            // Given
            String tenant1 = "tenant-001";
            String tenant2 = "tenant-002";
            Tenant tenant1Entity = createActiveTenant(tenant1);
            Tenant tenant2Entity = createActiveTenant(tenant2);

            when(tenantService.getTenant(tenant1)).thenReturn(tenant1Entity);
            when(tenantService.getTenant(tenant2)).thenReturn(tenant2Entity);

            // When
            validator.validate(tenant1);
            validator.validate(tenant2);
            validator.validate(tenant1);
            validator.validate(tenant2);

            // Then - 每个租户只调用一次
            verify(tenantService, times(1)).getTenant(tenant1);
            verify(tenantService, times(1)).getTenant(tenant2);
        }
    }

    // ========== 测试辅助方法 ==========

    private Tenant createActiveTenant(String tenantId) {
        return createTenantWithStatus(tenantId, TenantStatus.ACTIVE);
    }

    private Tenant createTenantWithStatus(String tenantId, TenantStatus status) {
        return new Tenant() {
            @Override
            public String getTenantId() {
                return tenantId;
            }

            @Override
            public String getTenantName() {
                return "Test Tenant";
            }

            @Override
            public TenantStatus getStatus() {
                return status;
            }

            @Override
            public Instant getExpiresAt() {
                return null; // 永不过期
            }
        };
    }

    private Tenant createExpiredTenant(String tenantId) {
        return new Tenant() {
            @Override
            public String getTenantId() {
                return tenantId;
            }

            @Override
            public String getTenantName() {
                return "Expired Tenant";
            }

            @Override
            public TenantStatus getStatus() {
                return TenantStatus.ACTIVE;
            }

            @Override
            public Instant getExpiresAt() {
                return Instant.now().minus(1, ChronoUnit.DAYS);
            }
        };
    }

    private Tenant createTenantWithExpiration(String tenantId, Instant expiresAt) {
        return new Tenant() {
            @Override
            public String getTenantId() {
                return tenantId;
            }

            @Override
            public String getTenantName() {
                return "Tenant with Expiration";
            }

            @Override
            public TenantStatus getStatus() {
                return TenantStatus.ACTIVE;
            }

            @Override
            public Instant getExpiresAt() {
                return expiresAt;
            }
        };
    }
}
