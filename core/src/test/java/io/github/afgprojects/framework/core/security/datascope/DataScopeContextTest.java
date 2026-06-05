package io.github.afgprojects.framework.core.security.datascope;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DataScopeContext")
class DataScopeContextTest {

    @Nested
    @DisplayName("empty")
    class Empty {

        @Test
        @DisplayName("should create empty context")
        void shouldCreateEmptyContext() {
            DataScopeContext context = DataScopeContext.empty();

            assertThat(context.getUserId()).isNull();
            assertThat(context.getDeptId()).isNull();
            assertThat(context.getAccessibleDeptIds()).isEmpty();
            assertThat(context.isAllDataPermission()).isFalse();
            assertThat(context.isIgnoreDataScope()).isFalse();
        }
    }

    @Nested
    @DisplayName("allPermission")
    class AllPermission {

        @Test
        @DisplayName("should create context with all data permission")
        void shouldCreateContextWithAllDataPermission() {
            DataScopeContext context = DataScopeContext.allPermission(1L);

            assertThat(context.getUserId()).isEqualTo(1L);
            assertThat(context.isAllDataPermission()).isTrue();
        }
    }

    @Nested
    @DisplayName("hasDeptPermission")
    class HasDeptPermission {

        @Test
        @DisplayName("should return true when allDataPermission is true")
        void shouldReturnTrue_whenAllDataPermissionIsTrue() {
            DataScopeContext context = DataScopeContext.allPermission(1L);

            assertThat(context.hasDeptPermission(999L)).isTrue();
        }

        @Test
        @DisplayName("should return true when ignoreDataScope is true")
        void shouldReturnTrue_whenIgnoreDataScopeIsTrue() {
            DataScopeContext context = DataScopeContext.builder()
                    .userId(1L)
                    .ignoreDataScope(true)
                    .build();

            assertThat(context.hasDeptPermission(999L)).isTrue();
        }

        @Test
        @DisplayName("should return true when deptId matches")
        void shouldReturnTrue_whenDeptIdMatches() {
            DataScopeContext context = DataScopeContext.builder()
                    .userId(1L)
                    .deptId(10L)
                    .build();

            assertThat(context.hasDeptPermission(10L)).isTrue();
        }

        @Test
        @DisplayName("should return true when deptId in accessibleDeptIds")
        void shouldReturnTrue_whenDeptIdInAccessibleDeptIds() {
            DataScopeContext context = DataScopeContext.builder()
                    .userId(1L)
                    .deptId(10L)
                    .accessibleDeptIds(Set.of(10L, 20L, 30L))
                    .build();

            assertThat(context.hasDeptPermission(20L)).isTrue();
            assertThat(context.hasDeptPermission(30L)).isTrue();
        }

        @Test
        @DisplayName("should return false when no permission")
        void shouldReturnFalse_whenNoPermission() {
            DataScopeContext context = DataScopeContext.builder()
                    .userId(1L)
                    .deptId(10L)
                    .build();

            assertThat(context.hasDeptPermission(999L)).isFalse();
        }
    }

    @Nested
    @DisplayName("isSelfData")
    class IsSelfData {

        @Test
        @DisplayName("should return true when allDataPermission is true")
        void shouldReturnTrue_whenAllDataPermissionIsTrue() {
            DataScopeContext context = DataScopeContext.allPermission(1L);

            assertThat(context.isSelfData(999L)).isTrue();
        }

        @Test
        @DisplayName("should return true when ignoreDataScope is true")
        void shouldReturnTrue_whenIgnoreDataScopeIsTrue() {
            DataScopeContext context = DataScopeContext.builder()
                    .ignoreDataScope(true)
                    .build();

            assertThat(context.isSelfData(999L)).isTrue();
        }

        @Test
        @DisplayName("should return true when userId matches")
        void shouldReturnTrue_whenUserIdMatches() {
            DataScopeContext context = DataScopeContext.builder()
                    .userId(1L)
                    .build();

            assertThat(context.isSelfData(1L)).isTrue();
        }

        @Test
        @DisplayName("should return false when userId does not match")
        void shouldReturnFalse_whenUserIdDoesNotMatch() {
            DataScopeContext context = DataScopeContext.builder()
                    .userId(1L)
                    .build();

            assertThat(context.isSelfData(2L)).isFalse();
        }

        @Test
        @DisplayName("should return false when userId is null")
        void shouldReturnFalse_whenUserIdIsNull() {
            DataScopeContext context = DataScopeContext.empty();

            assertThat(context.isSelfData(1L)).isFalse();
        }
    }

    @Nested
    @DisplayName("accessibleDeptIds")
    class AccessibleDeptIds {

        @Test
        @DisplayName("should add accessible dept id")
        void shouldAddAccessibleDeptId() {
            DataScopeContext context = DataScopeContext.empty();
            context.addAccessibleDeptId(10L);

            assertThat(context.getAccessibleDeptIds()).contains(10L);
        }

        @Test
        @DisplayName("should add multiple accessible dept ids")
        void shouldAddMultipleAccessibleDeptIds() {
            DataScopeContext context = DataScopeContext.empty();
            context.addAccessibleDeptIds(Set.of(10L, 20L, 30L));

            assertThat(context.getAccessibleDeptIds()).containsExactlyInAnyOrder(10L, 20L, 30L);
        }

        @Test
        @DisplayName("should return unmodifiable set")
        void shouldReturnUnmodifiableSet() {
            DataScopeContext context = DataScopeContext.empty();
            context.addAccessibleDeptId(10L);

            Set<Long> deptIds = context.getAccessibleDeptIds();
            assertThatThrownBy(() -> deptIds.add(99L))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
