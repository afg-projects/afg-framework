package io.github.afgprojects.framework.security.core.tenant;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AfgTenantServiceTest {

    @Test
    void shouldGetTenantById() {
        AfgTenantService tenantService = new TestTenantService();
        Tenant tenant = tenantService.getTenant("tenant-001");

        assertThat(tenant).isNotNull();
        assertThat(tenant.getTenantId()).isEqualTo("tenant-001");
        assertThat(tenant.getTenantName()).isEqualTo("测试租户");
    }

    @Test
    void shouldReturnNullWhenTenantNotFound() {
        AfgTenantService tenantService = new TestTenantService();
        Tenant tenant = tenantService.getTenant("non-existent");

        assertThat(tenant).isNull();
    }

    @Test
    void shouldResolveTenantByDomain() {
        AfgTenantService tenantService = new TestTenantService();
        Tenant tenant = tenantService.resolveByDomain("tenant-001.example.com");

        assertThat(tenant).isNotNull();
        assertThat(tenant.getTenantId()).isEqualTo("tenant-001");
    }

    @Test
    void shouldReturnNullWhenDomainNotFound() {
        AfgTenantService tenantService = new TestTenantService();
        Tenant tenant = tenantService.resolveByDomain("non-existent.example.com");

        assertThat(tenant).isNull();
    }

    @Test
    void shouldCheckIfTenantIsActive() {
        AfgTenantService tenantService = new TestTenantService();

        // 活跃租户
        assertThat(tenantService.isTenantActive("tenant-001")).isTrue();

        // 不存在的租户
        assertThat(tenantService.isTenantActive("non-existent")).isFalse();

        // 已禁用的租户
        assertThat(tenantService.isTenantActive("tenant-disabled")).isFalse();

        // 已过期的租户
        assertThat(tenantService.isTenantActive("tenant-expired")).isFalse();
    }

    /**
     * Test implementation of AfgTenantService.
     */
    private static class TestTenantService implements AfgTenantService {

        @Override
        public @Nullable Tenant getTenant(@NonNull String tenantId) {
            switch (tenantId) {
                case "tenant-001":
                    return new TestTenant(
                        "tenant-001",
                        "acme",
                        "测试租户",
                        TenantStatus.ACTIVE,
                        null,
                        Map.of()
                    );
                case "tenant-disabled":
                    return new TestTenant(
                        "tenant-disabled",
                        null,
                        "禁用租户",
                        TenantStatus.DISABLED,
                        null,
                        Map.of()
                    );
                case "tenant-expired":
                    return new TestTenant(
                        "tenant-expired",
                        null,
                        "过期租户",
                        TenantStatus.ACTIVE,
                        Instant.now().minus(1, ChronoUnit.HOURS),
                        Map.of()
                    );
                default:
                    return null;
            }
        }

        @Override
        public @Nullable Tenant resolveByDomain(@NonNull String domain) {
            if ("tenant-001.example.com".equals(domain)) {
                return new TestTenant(
                    "tenant-001",
                    "acme",
                    "测试租户",
                    TenantStatus.ACTIVE,
                    null,
                    Map.of()
                );
            }
            return null;
        }
    }

    /**
     * Test implementation of Tenant.
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
            return tenantCode;
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
