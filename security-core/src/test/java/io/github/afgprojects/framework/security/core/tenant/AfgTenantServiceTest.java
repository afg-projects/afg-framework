package io.github.afgprojects.framework.security.core.tenant;

import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;

class AfgTenantServiceTest {

    @Test
    void shouldFindTenantById() {
        AfgTenantService tenantService = new TestTenantService();
        Optional<Tenant> tenant = tenantService.findById("tenant-001");

        assertThat(tenant).isPresent();
        assertThat(tenant.get().getId()).isEqualTo("tenant-001");
        assertThat(tenant.get().getName()).isEqualTo("测试租户");
    }

    @Test
    void shouldReturnEmptyWhenTenantNotFound() {
        AfgTenantService tenantService = new TestTenantService();
        Optional<Tenant> tenant = tenantService.findById("non-existent");

        assertThat(tenant).isEmpty();
    }

    @Test
    void shouldFindTenantByDomain() {
        AfgTenantService tenantService = new TestTenantService();
        Optional<Tenant> tenant = tenantService.findByDomain("tenant-001.example.com");

        assertThat(tenant).isPresent();
        assertThat(tenant.get().getId()).isEqualTo("tenant-001");
    }

    @Test
    void shouldReturnEmptyWhenDomainNotFound() {
        AfgTenantService tenantService = new TestTenantService();
        Optional<Tenant> tenant = tenantService.findByDomain("non-existent.example.com");

        assertThat(tenant).isEmpty();
    }

    /**
     * Test implementation of AfgTenantService.
     */
    private static class TestTenantService implements AfgTenantService {

        @Override
        public Optional<Tenant> findById(String tenantId) {
            if ("tenant-001".equals(tenantId)) {
                return Optional.of(new TestTenant(
                    "tenant-001",
                    "测试租户",
                    TenantStatus.ACTIVE,
                    "tenant-001.example.com",
                    null
                ));
            }
            return Optional.empty();
        }

        @Override
        public Optional<Tenant> findByDomain(String domain) {
            if ("tenant-001.example.com".equals(domain)) {
                return Optional.of(new TestTenant(
                    "tenant-001",
                    "测试租户",
                    TenantStatus.ACTIVE,
                    "tenant-001.example.com",
                    null
                ));
            }
            return Optional.empty();
        }
    }

    /**
     * Test implementation of Tenant.
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
        public Optional<String> getDomain() {
            return Optional.ofNullable(domain);
        }

        @Override
        public Optional<String> getContactEmail() {
            return Optional.ofNullable(contactEmail);
        }
    }
}
