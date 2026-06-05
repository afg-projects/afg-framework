package io.github.afgprojects.framework.security.core.tenant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tenant 接口默认方法测试
 */
@DisplayName("Tenant 接口默认方法测试")
class TenantTest {

    @Nested
    @DisplayName("getTenantCode 默认方法")
    class GetTenantCodeTests {

        @Test
        @DisplayName("默认应返回 tenantId")
        void shouldReturnTenantIdByDefault() {
            Tenant tenant = createTenant("tenant-001", null, TenantStatus.ACTIVE, null);

            assertThat(tenant.getTenantCode()).isEqualTo("tenant-001");
        }

        @Test
        @DisplayName("有自定义 tenantCode 时应返回 tenantCode")
        void shouldReturnCustomTenantCodeWhenSet() {
            Tenant tenant = createTenant("tenant-001", "acme", TenantStatus.ACTIVE, null);

            assertThat(tenant.getTenantCode()).isEqualTo("acme");
        }
    }

    @Nested
    @DisplayName("getAttributes 默认方法")
    class GetAttributesTests {

        @Test
        @DisplayName("默认应返回空 Map")
        void shouldReturnEmptyMapByDefault() {
            Tenant tenant = createTenant("tenant-001", null, TenantStatus.ACTIVE, null);

            assertThat(tenant.getAttributes()).isEmpty();
        }
    }

    @Nested
    @DisplayName("isActive 默认方法")
    class IsActiveTests {

        @Test
        @DisplayName("ACTIVE 状态且未过期应返回 true")
        void shouldReturnTrueWhenActiveAndNotExpired() {
            Tenant tenant = createTenant("tenant-001", null, TenantStatus.ACTIVE, null);

            assertThat(tenant.isActive()).isTrue();
        }

        @Test
        @DisplayName("ACTIVE 状态且过期时间在未来应返回 true")
        void shouldReturnTrueWhenActiveAndExpiresInFuture() {
            Tenant tenant = createTenant("tenant-001", null, TenantStatus.ACTIVE, Instant.now().plusSeconds(3600));

            assertThat(tenant.isActive()).isTrue();
        }

        @Test
        @DisplayName("ACTIVE 状态但已过期应返回 false")
        void shouldReturnFalseWhenActiveButExpired() {
            Tenant tenant = createTenant("tenant-001", null, TenantStatus.ACTIVE, Instant.now().minusSeconds(10));

            assertThat(tenant.isActive()).isFalse();
        }

        @Test
        @DisplayName("DISABLED 状态应返回 false")
        void shouldReturnFalseWhenDisabled() {
            Tenant tenant = createTenant("tenant-001", null, TenantStatus.DISABLED, null);

            assertThat(tenant.isActive()).isFalse();
        }

        @Test
        @DisplayName("SUSPENDED 状态应返回 false")
        void shouldReturnFalseWhenSuspended() {
            Tenant tenant = createTenant("tenant-001", null, TenantStatus.SUSPENDED, null);

            assertThat(tenant.isActive()).isFalse();
        }
    }

    /**
     * 创建 Tenant 实例用于测试 default 方法
     */
    private Tenant createTenant(String tenantId, String tenantCode, TenantStatus status, Instant expiresAt) {
        return new Tenant() {
            @Override
            public String getTenantId() {
                return tenantId;
            }

            @Override
            public String getTenantCode() {
                return tenantCode != null ? tenantCode : Tenant.super.getTenantCode();
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
                return expiresAt;
            }
        };
    }
}