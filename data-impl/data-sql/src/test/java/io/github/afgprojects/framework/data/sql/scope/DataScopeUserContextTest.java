package io.github.afgprojects.framework.data.sql.scope;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * DataScopeUserContext 测试
 */
class DataScopeUserContextTest {

    @Test
    @DisplayName("创建空的上下文")
    void testEmptyContext() {
        DataScopeUserContext context = DataScopeUserContext.empty();

        assertThat(context.getUserId()).isNull();
        assertThat(context.getDeptId()).isNull();
        assertThat(context.getTenantId()).isNull();
        assertThat(context.getAccessibleDeptIds()).isEmpty();
        assertThat(context.isAllDataPermission()).isFalse();
    }

    @Test
    @DisplayName("使用 Builder 创建上下文")
    void testBuilder() {
        DataScopeUserContext context = DataScopeUserContext.builder()
                .userId(123L)
                .deptId(456L)
                .tenantId(789L)
                .accessibleDeptIds(Set.of(1L, 2L, 3L))
                .allDataPermission(true)
                .build();

        assertThat(context.getUserId()).isEqualTo(123L);
        assertThat(context.getDeptId()).isEqualTo(456L);
        assertThat(context.getTenantId()).isEqualTo(789L);
        assertThat(context.getAccessibleDeptIds()).containsExactlyInAnyOrder(1L, 2L, 3L);
        assertThat(context.isAllDataPermission()).isTrue();
    }

    @Test
    @DisplayName("部门ID集合不可修改")
    void testUnmodifiableDeptIds() {
        DataScopeUserContext context = DataScopeUserContext.builder()
                .accessibleDeptIds(Set.of(1L, 2L))
                .build();

        Set<Long> deptIds = context.getAccessibleDeptIds();
        assertThatThrownBy(() -> deptIds.add(3L))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("null 部门ID集合转为空集合")
    void testNullAccessibleDeptIds() {
        DataScopeUserContext context = DataScopeUserContext.builder()
                .accessibleDeptIds(null)
                .build();

        assertThat(context.getAccessibleDeptIds()).isNotNull();
        assertThat(context.getAccessibleDeptIds()).isEmpty();
    }
}