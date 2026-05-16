package io.github.afgprojects.framework.security.core.tenant;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TenantTest {

    @Test
    void shouldGetBasicProperties() {
        Tenant tenant = createTestTenant("tenant-001", "测试租户", TenantStatus.ACTIVE, null);

        assertThat(tenant.getTenantId()).isEqualTo("tenant-001");
        assertThat(tenant.getTenantName()).isEqualTo("测试租户");
        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.ACTIVE);
    }

    @Test
    void shouldGetTenantCode() {
        Tenant tenant = createTestTenant("tenant-001", "测试租户", TenantStatus.ACTIVE, null);

        // 默认返回 tenantId
        assertThat(tenant.getTenantCode()).isEqualTo("tenant-001");
    }

    @Test
    void shouldSupportCustomTenantCode() {
        Tenant tenant = new TestTenant("tenant-001", "acme", "测试租户", TenantStatus.ACTIVE, null, Map.of());

        assertThat(tenant.getTenantCode()).isEqualTo("acme");
    }

    @Test
    void activeTenantShouldBeValid() {
        Tenant activeTenant = createTestTenant("tenant-001", "活跃租户", TenantStatus.ACTIVE, null);
        Tenant suspendedTenant = createTestTenant("tenant-002", "暂停租户", TenantStatus.SUSPENDED, null);
        Tenant disabledTenant = createTestTenant("tenant-003", "禁用租户", TenantStatus.DISABLED, null);

        assertThat(activeTenant.isActive()).isTrue();
        assertThat(suspendedTenant.isActive()).isFalse();
        assertThat(disabledTenant.isActive()).isFalse();
    }

    @Test
    void expiredTenantShouldNotBeActive() {
        // 过期租户
        Instant pastTime = Instant.now().minus(1, ChronoUnit.HOURS);
        Tenant expiredTenant = createTestTenant("tenant-001", "过期租户", TenantStatus.ACTIVE, pastTime);

        assertThat(expiredTenant.isActive()).isFalse();
    }

    @Test
    void tenantExpiringInFutureShouldBeActive() {
        // 未来过期的租户
        Instant futureTime = Instant.now().plus(1, ChronoUnit.DAYS);
        Tenant tenant = createTestTenant("tenant-001", "活跃租户", TenantStatus.ACTIVE, futureTime);

        assertThat(tenant.isActive()).isTrue();
    }

    @Test
    void shouldSupportAttributes() {
        Map<String, Object> attributes = Map.of("type", "enterprise", "quota", 1000);
        Tenant tenant = new TestTenant("tenant-001", null, "测试租户", TenantStatus.ACTIVE, null, attributes);

        assertThat(tenant.getAttributes())
            .containsEntry("type", "enterprise")
            .containsEntry("quota", 1000);
    }

    @Test
    void shouldReturnEmptyAttributesByDefault() {
        Tenant tenant = createTestTenant("tenant-001", "测试租户", TenantStatus.ACTIVE, null);

        assertThat(tenant.getAttributes()).isEmpty();
    }

    private Tenant createTestTenant(String id, String name, TenantStatus status, Instant expiresAt) {
        return new TestTenant(id, null, name, status, expiresAt, Map.of());
    }

    /**
     * Test implementation of Tenant interface.
     */
    private static class TestTenant implements Tenant {
        private final String tenantId;
        private final String tenantCode;
        private final String tenantName;
        private final TenantStatus status;
        private final Instant expiresAt;
        private final Map<String, Object> attributes;

        TestTenant(String tenantId, String tenantCode, String tenantName, TenantStatus status,
                   Instant expiresAt, Map<String, Object> attributes) {
            this.tenantId = tenantId;
            this.tenantCode = tenantCode;
            this.tenantName = tenantName;
            this.status = status;
            this.expiresAt = expiresAt;
            this.attributes = attributes;
        }

        @Override
        public @NonNull String getTenantId() {
            return tenantId;
        }

        @Override
        public @Nullable String getTenantCode() {
            // 如果未设置 tenantCode，返回 tenantId（符合 spec 默认行为）
            return tenantCode != null ? tenantCode : getTenantId();
        }

        @Override
        public @Nullable String getTenantName() {
            return tenantName;
        }

        @Override
        public @NonNull TenantStatus getStatus() {
            return status;
        }

        @Override
        public @Nullable Instant getExpiresAt() {
            return expiresAt;
        }

        @Override
        public @NonNull Map<String, Object> getAttributes() {
            return attributes;
        }
    }
}
