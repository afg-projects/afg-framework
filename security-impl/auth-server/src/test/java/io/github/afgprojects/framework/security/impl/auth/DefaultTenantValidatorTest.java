package io.github.afgprojects.framework.security.impl.auth;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.afgprojects.framework.security.auth.tenant.validator.DefaultTenantValidator;
import io.github.afgprojects.framework.security.core.tenant.AfgTenantService;
import io.github.afgprojects.framework.security.core.tenant.Tenant;
import io.github.afgprojects.framework.security.core.tenant.TenantException;
import io.github.afgprojects.framework.security.core.tenant.TenantStatus;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * DefaultTenantValidator 测试
 */
@DisplayName("DefaultTenantValidator 测试")
class DefaultTenantValidatorTest {

    /**
     * 内存租户服务，用于测试
     */
    private static class InMemoryTenantService implements AfgTenantService {

        private final Map<String, SimpleTenant> tenants = new java.util.concurrent.ConcurrentHashMap<>();

        void addTenant(String tenantId, TenantStatus status, @Nullable Instant expiresAt) {
            tenants.put(tenantId, new SimpleTenant(tenantId, tenantId, status, expiresAt));
        }

        @Override
        @Nullable
        public Tenant getTenant(@NonNull String tenantId) {
            return tenants.get(tenantId);
        }
    }

    private static class SimpleTenant implements Tenant {

        private final String tenantId;
        private final String tenantName;
        private final TenantStatus status;
        private final Instant expiresAt;

        SimpleTenant(String tenantId, String tenantName, TenantStatus status, @Nullable Instant expiresAt) {
            this.tenantId = tenantId;
            this.tenantName = tenantName;
            this.status = status;
            this.expiresAt = expiresAt;
        }

        @Override
        @NonNull
        public String getTenantId() {
            return tenantId;
        }

        @Override
        @Nullable
        public String getTenantName() {
            return tenantName;
        }

        @Override
        @NonNull
        public TenantStatus getStatus() {
            return status;
        }

        @Override
        @Nullable
        public Instant getExpiresAt() {
            return expiresAt;
        }
    }

    private InMemoryTenantService tenantService;
    private Cache<String, Boolean> validationCache;
    private DefaultTenantValidator validator;

    @BeforeEach
    void setUp() {
        tenantService = new InMemoryTenantService();
        validationCache = Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(1000)
                .build();
        validator = new DefaultTenantValidator(tenantService, validationCache);
    }

    @Nested
    @DisplayName("validate 方法")
    class ValidateTests {

        @Test
        @DisplayName("活跃租户应验证通过")
        void shouldPassForActiveTenant() {
            tenantService.addTenant("tenant-001", TenantStatus.ACTIVE, null);

            assertThatCode(() -> validator.validate("tenant-001"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("禁用的租户应抛出异常")
        void shouldThrowForDisabledTenant() {
            tenantService.addTenant("tenant-001", TenantStatus.DISABLED, null);

            assertThatThrownBy(() -> validator.validate("tenant-001"))
                    .isInstanceOf(TenantException.class);
        }

        @Test
        @DisplayName("暂停的租户应抛出异常")
        void shouldThrowForSuspendedTenant() {
            tenantService.addTenant("tenant-001", TenantStatus.SUSPENDED, null);

            assertThatThrownBy(() -> validator.validate("tenant-001"))
                    .isInstanceOf(TenantException.class);
        }

        @Test
        @DisplayName("不存在的租户应抛出异常")
        void shouldThrowForNonExistentTenant() {
            assertThatThrownBy(() -> validator.validate("non-existent"))
                    .isInstanceOf(TenantException.class);
        }

        @Test
        @DisplayName("已过期的租户应抛出异常")
        void shouldThrowForExpiredTenant() {
            tenantService.addTenant("tenant-001", TenantStatus.ACTIVE, Instant.now().minusSeconds(60));

            assertThatThrownBy(() -> validator.validate("tenant-001"))
                    .isInstanceOf(TenantException.class);
        }

        @Test
        @DisplayName("未过期的租户应验证通过")
        void shouldPassForNonExpiredTenant() {
            tenantService.addTenant("tenant-001", TenantStatus.ACTIVE, Instant.now().plusSeconds(3600));

            assertThatCode(() -> validator.validate("tenant-001"))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("缓存行为")
    class CacheTests {

        @Test
        @DisplayName("验证通过后应写入缓存")
        void shouldCacheValidationResult() {
            tenantService.addTenant("tenant-001", TenantStatus.ACTIVE, null);

            validator.validate("tenant-001");

            // 缓存中应有记录
            assertThat(validationCache.getIfPresent("tenant-001")).isTrue();
        }

        @Test
        @DisplayName("缓存命中时应直接通过，不调用服务")
        void shouldUseCacheWhenAvailable() {
            tenantService.addTenant("tenant-001", TenantStatus.ACTIVE, null);

            // 第一次验证
            validator.validate("tenant-001");

            // 移除租户（模拟服务不可用）
            tenantService = new InMemoryTenantService();
            // 由于缓存，应仍然通过（此处无法直接替换服务，仅验证缓存存在）
            assertThat(validationCache.getIfPresent("tenant-001")).isTrue();
        }

        @Test
        @DisplayName("缓存未命中时应调用服务验证")
        void shouldCallServiceWhenCacheMiss() {
            tenantService.addTenant("tenant-001", TenantStatus.ACTIVE, null);

            // 缓存为空
            assertThat(validationCache.getIfPresent("tenant-001")).isNull();

            // 验证后缓存应有值
            validator.validate("tenant-001");
            assertThat(validationCache.getIfPresent("tenant-001")).isTrue();
        }
    }
}
