package io.github.afgprojects.framework.security.core.tenant;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class TenantTest {

    @Test
    void shouldGetBasicProperties() {
        Tenant tenant = createTestTenant("tenant-001", "测试租户", TenantStatus.ACTIVE);

        assertThat(tenant.getId()).isEqualTo("tenant-001");
        assertThat(tenant.getName()).isEqualTo("测试租户");
        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.ACTIVE);
    }

    @Test
    void activeTenantShouldBeValid() {
        Tenant activeTenant = createTestTenant("tenant-001", "活跃租户", TenantStatus.ACTIVE);
        Tenant suspendedTenant = createTestTenant("tenant-002", "暂停租户", TenantStatus.SUSPENDED);
        Tenant disabledTenant = createTestTenant("tenant-003", "禁用租户", TenantStatus.DISABLED);

        assertThat(activeTenant.isActive()).isTrue();
        assertThat(suspendedTenant.isActive()).isFalse();
        assertThat(disabledTenant.isActive()).isFalse();
    }

    @Test
    void shouldSupportOptionalProperties() {
        Tenant tenant = new TestTenant(
            "tenant-001",
            "测试租户",
            TenantStatus.ACTIVE,
            "tenant-001.example.com",
            "contact@example.com"
        );

        assertThat(tenant.getDomain()).isPresent().contains("tenant-001.example.com");
        assertThat(tenant.getContactEmail()).isPresent().contains("contact@example.com");
    }

    private Tenant createTestTenant(String id, String name, TenantStatus status) {
        return new TestTenant(id, name, status, null, null);
    }

    /**
     * Test implementation of Tenant interface.
     */
    private static class TestTenant implements Tenant {
        private final String id;
        private final String name;
        private final TenantStatus status;
        private final String domain;
        private final String contactEmail;

        TestTenant(String id, String name, TenantStatus status, String domain, String contactEmail) {
            this.id = id;
            this.name = name;
            this.status = status;
            this.domain = domain;
            this.contactEmail = contactEmail;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public TenantStatus getStatus() {
            return status;
        }

        @Override
        public java.util.Optional<String> getDomain() {
            return java.util.Optional.ofNullable(domain);
        }

        @Override
        public java.util.Optional<String> getContactEmail() {
            return java.util.Optional.ofNullable(contactEmail);
        }
    }
}
