package io.github.afgprojects.framework.core.security.datascope;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * DataScopeContext 测试
 */
@DisplayName("DataScopeContext 测试")
class DataScopeContextTest {

    @Nested
    @DisplayName("构建器测试")
    class BuilderTests {

        @Test
        @DisplayName("应该构建完整的上下文")
        void shouldBuildFullContext() {
            // given
            Set<Long> deptIds = new HashSet<>();
            deptIds.add(1L);
            deptIds.add(2L);

            // when
            DataScopeContext context = DataScopeContext.builder()
                    .userId(100L)
                    .deptId(10L)
                    .accessibleDeptIds(deptIds)
                    .customCondition("status = 1")
                    .allDataPermission(false)
                    .ignoreDataScope(false)
                    .build();

            // then
            assertThat(context.getUserId()).isEqualTo(100L);
            assertThat(context.getDeptId()).isEqualTo(10L);
            assertThat(context.getAccessibleDeptIds()).containsExactlyInAnyOrder(1L, 2L);
            assertThat(context.getCustomCondition()).isEqualTo("status = 1");
            assertThat(context.isAllDataPermission()).isFalse();
            assertThat(context.isIgnoreDataScope()).isFalse();
        }

        @Test
        @DisplayName("应该构建空上下文")
        void shouldBuildEmptyContext() {
            // when
            DataScopeContext context = DataScopeContext.empty();

            // then
            assertThat(context.getUserId()).isNull();
            assertThat(context.getDeptId()).isNull();
            assertThat(context.getAccessibleDeptIds()).isEmpty();
            assertThat(context.isAllDataPermission()).isFalse();
            assertThat(context.isIgnoreDataScope()).isFalse();
        }

        @Test
        @DisplayName("应该构建管理员上下文")
        void shouldBuildAllPermissionContext() {
            // when
            DataScopeContext context = DataScopeContext.allPermission(100L);

            // then
            assertThat(context.getUserId()).isEqualTo(100L);
            assertThat(context.isAllDataPermission()).isTrue();
        }
    }

    @Nested
    @DisplayName("部门权限测试")
    class DeptPermissionTests {

        private DataScopeContext context;

        @BeforeEach
        void setUp() {
            Set<Long> deptIds = new HashSet<>();
            deptIds.add(20L);
            deptIds.add(30L);

            context = DataScopeContext.builder()
                    .userId(100L)
                    .deptId(10L)
                    .accessibleDeptIds(deptIds)
                    .build();
        }

        @Test
        @DisplayName("应该判断本部门有权限")
        void shouldHavePermissionForOwnDept() {
            assertThat(context.hasDeptPermission(10L)).isTrue();
        }

        @Test
        @DisplayName("应该判断子部门有权限")
        void shouldHavePermissionForChildDept() {
            assertThat(context.hasDeptPermission(20L)).isTrue();
            assertThat(context.hasDeptPermission(30L)).isTrue();
        }

        @Test
        @DisplayName("应该判断其他部门无权限")
        void shouldNotHavePermissionForOtherDept() {
            assertThat(context.hasDeptPermission(99L)).isFalse();
        }
    }

    @Nested
    @DisplayName("本人数据测试")
    class SelfDataTests {

        private DataScopeContext context;

        @BeforeEach
        void setUp() {
            context = DataScopeContext.builder()
                    .userId(100L)
                    .build();
        }

        @Test
        @DisplayName("应该判断本人的数据")
        void shouldBeSelfData() {
            assertThat(context.isSelfData(100L)).isTrue();
        }

        @Test
        @DisplayName("应该判断不是本人的数据")
        void shouldNotBeSelfData() {
            assertThat(context.isSelfData(200L)).isFalse();
        }
    }

    @Nested
    @DisplayName("全部权限测试")
    class AllPermissionTests {

        private DataScopeContext context;

        @BeforeEach
        void setUp() {
            context = DataScopeContext.allPermission(100L);
        }

        @Test
        @DisplayName("全部权限应该可以访问任何部门")
        void shouldAccessAnyDeptWithAllPermission() {
            assertThat(context.hasDeptPermission(1L)).isTrue();
            assertThat(context.hasDeptPermission(99L)).isTrue();
        }

        @Test
        @DisplayName("全部权限应该可以访问任何人的数据")
        void shouldAccessAnyDataWithAllPermission() {
            assertThat(context.isSelfData(1L)).isTrue();
            assertThat(context.isSelfData(99L)).isTrue();
        }
    }

    @Nested
    @DisplayName("忽略权限测试")
    class IgnoreScopeTests {

        @Test
        @DisplayName("忽略权限应该可以访问任何部门")
        void shouldAccessAnyDeptWhenIgnored() {
            // given
            DataScopeContext context = DataScopeContext.builder()
                    .userId(100L)
                    .deptId(10L)
                    .ignoreDataScope(true)
                    .build();

            // then
            assertThat(context.hasDeptPermission(1L)).isTrue();
            assertThat(context.hasDeptPermission(99L)).isTrue();
        }
    }

    @Nested
    @DisplayName("部门集合操作测试")
    class DeptSetOperationTests {

        private DataScopeContext context;

        @BeforeEach
        void setUp() {
            context = DataScopeContext.builder()
                    .userId(100L)
                    .deptId(10L)
                    .build();
        }

        @Test
        @DisplayName("应该添加单个部门")
        void shouldAddSingleDept() {
            // when
            context.addAccessibleDeptId(20L);

            // then
            assertThat(context.getAccessibleDeptIds()).contains(20L);
        }

        @Test
        @DisplayName("应该添加多个部门")
        void shouldAddMultipleDepts() {
            // given
            Set<Long> deptIds = new HashSet<>();
            deptIds.add(20L);
            deptIds.add(30L);

            // when
            context.addAccessibleDeptIds(deptIds);

            // then
            assertThat(context.getAccessibleDeptIds()).containsExactlyInAnyOrder(20L, 30L);
        }

        @Test
        @DisplayName("应该返回不可修改的部门集合")
        void shouldReturnUnmodifiableSet() {
            // given
            context.addAccessibleDeptId(20L);
            Set<Long> deptIds = context.getAccessibleDeptIds();

            // when/then
            assertThatCode(() -> deptIds.add(99L))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}