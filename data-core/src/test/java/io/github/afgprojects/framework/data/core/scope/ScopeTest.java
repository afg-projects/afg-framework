package io.github.afgprojects.framework.data.core.scope;

import io.github.afgprojects.framework.data.core.context.TenantContextHolder;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScopeTest {

    @Test
    void shouldCreateDataScope() {
        DataScope scope = DataScope.of("sys_user", "dept_id", DataScopeType.DEPT_AND_CHILD);

        assertThat(scope.table()).isEqualTo("sys_user");
        assertThat(scope.column()).isEqualTo("dept_id");
        assertThat(scope.scopeType()).isEqualTo(DataScopeType.DEPT_AND_CHILD);
    }

    @Test
    void shouldCreateDataScopeWithBuilder() {
        DataScope scope = DataScope.builder()
            .table("sys_user")
            .column("dept_id")
            .scopeType(DataScopeType.DEPT)
            .aliasPrefix("u")
            .build();

        assertThat(scope.table()).isEqualTo("sys_user");
        assertThat(scope.aliasPrefix()).isEqualTo("u");
    }

    @Test
    void shouldManageTenantContext() {
        TenantContextHolder holder = new TenantContextHolder();

        holder.setTenantId("tenant-001");
        assertThat(holder.getTenantId()).isEqualTo("tenant-001");

        holder.clear();
        assertThat(holder.getTenantId()).isNull();
    }

    @Test
    void shouldUseTenantScopeWithTryWithResources() {
        TenantContextHolder holder = new TenantContextHolder();

        try (TenantScope scope = holder.scope("tenant-002")) {
            assertThat(holder.getTenantId()).isEqualTo("tenant-002");
        }

        assertThat(holder.getTenantId()).isNull();
    }

    @Test
    void shouldRestorePreviousTenantOnClose() {
        TenantContextHolder holder = new TenantContextHolder();
        holder.setTenantId("tenant-001");

        try (TenantScope scope = holder.scope("tenant-002")) {
            assertThat(holder.getTenantId()).isEqualTo("tenant-002");
        }

        assertThat(holder.getTenantId()).isEqualTo("tenant-001");
    }
}