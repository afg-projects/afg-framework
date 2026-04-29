package io.github.afgprojects.framework.data.sql.scope;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DataScopeContextProviders 测试
 */
class DataScopeContextProvidersTest {

    @Test
    @DisplayName("empty() 返回 null")
    void testEmpty() {
        DataScopeContextProvider provider = DataScopeContextProviders.empty();
        assertThat(provider.provide()).isNull();
    }

    @Test
    @DisplayName("fromSupplier() 正确工作")
    void testFromSupplier() {
        DataScopeContextProvider provider = DataScopeContextProviders.fromSupplier(
                () -> DataScopeUserContext.builder().userId(123L).build()
        );

        DataScopeUserContext context = provider.provide();
        assertThat(context).isNotNull();
        assertThat(context.getUserId()).isEqualTo(123L);
    }

    @Test
    @DisplayName("fixed() 始终返回相同的上下文")
    void testFixed() {
        DataScopeUserContext context = DataScopeUserContext.builder()
                .userId(123L)
                .deptId(456L)
                .build();

        DataScopeContextProvider provider = DataScopeContextProviders.fixed(context);

        assertThat(provider.provide()).isSameAs(context);
        assertThat(provider.provide()).isSameAs(context);
    }

    @Test
    @DisplayName("admin() 创建拥有全部权限的上下文")
    void testAdmin() {
        DataScopeContextProvider provider = DataScopeContextProviders.admin(123L);
        DataScopeUserContext context = provider.provide();

        assertThat(context.getUserId()).isEqualTo(123L);
        assertThat(context.isAllDataPermission()).isTrue();
    }

    @Test
    @DisplayName("user() 创建普通用户上下文")
    void testUser() {
        DataScopeContextProvider provider = DataScopeContextProviders.user(123L, 456L);
        DataScopeUserContext context = provider.provide();

        assertThat(context.getUserId()).isEqualTo(123L);
        assertThat(context.getDeptId()).isEqualTo(456L);
        assertThat(context.isAllDataPermission()).isFalse();
    }

    @Test
    @DisplayName("userWithDepts() 创建带部门集合的上下文")
    void testUserWithDepts() {
        Set<Long> deptIds = Set.of(1L, 2L, 3L);
        DataScopeContextProvider provider = DataScopeContextProviders.userWithDepts(123L, 456L, deptIds);
        DataScopeUserContext context = provider.provide();

        assertThat(context.getUserId()).isEqualTo(123L);
        assertThat(context.getDeptId()).isEqualTo(456L);
        assertThat(context.getAccessibleDeptIds()).containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    @DisplayName("userWithTenant() 创建带租户的上下文")
    void testUserWithTenant() {
        DataScopeContextProvider provider = DataScopeContextProviders.userWithTenant(123L, 456L, 789L);
        DataScopeUserContext context = provider.provide();

        assertThat(context.getUserId()).isEqualTo(123L);
        assertThat(context.getDeptId()).isEqualTo(456L);
        assertThat(context.getTenantId()).isEqualTo(789L);
    }

    @Test
    @DisplayName("full() 创建完整上下文")
    void testFull() {
        Set<Long> deptIds = Set.of(1L, 2L);
        DataScopeContextProvider provider = DataScopeContextProviders.full(
                123L, 456L, deptIds, 789L, true
        );
        DataScopeUserContext context = provider.provide();

        assertThat(context.getUserId()).isEqualTo(123L);
        assertThat(context.getDeptId()).isEqualTo(456L);
        assertThat(context.getAccessibleDeptIds()).containsExactlyInAnyOrder(1L, 2L);
        assertThat(context.getTenantId()).isEqualTo(789L);
        assertThat(context.isAllDataPermission()).isTrue();
    }

    @Test
    @DisplayName("fromContextHolder() 正确工作")
    void testFromContextHolder() {
        DataScopeContextProviders.ContextHolder holder = new DataScopeContextProviders.ContextHolder() {
            @Override
            public Long getUserId() {
                return 123L;
            }

            @Override
            public Long getDeptId() {
                return 456L;
            }

            @Override
            public Set<Long> getAccessibleDeptIds() {
                return Set.of(1L, 2L);
            }

            @Override
            public Long getTenantId() {
                return 789L;
            }
        };

        DataScopeContextProvider provider = DataScopeContextProviders.fromContextHolder(holder);
        DataScopeUserContext context = provider.provide();

        assertThat(context.getUserId()).isEqualTo(123L);
        assertThat(context.getDeptId()).isEqualTo(456L);
        assertThat(context.getAccessibleDeptIds()).containsExactlyInAnyOrder(1L, 2L);
        assertThat(context.getTenantId()).isEqualTo(789L);
    }

    @Test
    @DisplayName("fromContextHolder() 返回 null 当没有有效信息")
    void testFromContextHolderReturnsNull() {
        DataScopeContextProviders.ContextHolder holder = new DataScopeContextProviders.ContextHolder() {
            @Override
            public Long getUserId() {
                return null;
            }
        };

        DataScopeContextProvider provider = DataScopeContextProviders.fromContextHolder(holder);
        assertThat(provider.provide()).isNull();
    }
}