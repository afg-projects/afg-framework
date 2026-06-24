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
            DataScopeContext context = DataScopeContext.allPermission("1");

            assertThat(context.getUserId()).isEqualTo("1");
            assertThat(context.isAllDataPermission()).isTrue();
        }
    }

    @Nested
    @DisplayName("hasDeptPermission")
    class HasDeptPermission {

        @Test
        @DisplayName("should return true when allDataPermission is true")
        void shouldReturnTrue_whenAllDataPermissionIsTrue() {
            DataScopeContext context = DataScopeContext.allPermission("1");

            assertThat(context.hasDeptPermission("999")).isTrue();
        }

        @Test
        @DisplayName("should return true when ignoreDataScope is true")
        void shouldReturnTrue_whenIgnoreDataScopeIsTrue() {
            DataScopeContext context = DataScopeContext.builder()
                    .userId("1")
                    .ignoreDataScope(true)
                    .build();

            assertThat(context.hasDeptPermission("999")).isTrue();
        }

        @Test
        @DisplayName("should return true when deptId matches")
        void shouldReturnTrue_whenDeptIdMatches() {
            DataScopeContext context = DataScopeContext.builder()
                    .userId("1")
                    .deptId("10")
                    .build();

            assertThat(context.hasDeptPermission("10")).isTrue();
        }

        @Test
        @DisplayName("should return true when deptId in accessibleDeptIds")
        void shouldReturnTrue_whenDeptIdInAccessibleDeptIds() {
            DataScopeContext context = DataScopeContext.builder()
                    .userId("1")
                    .deptId("10")
                    .accessibleDeptIds(Set.of("10", "20", "30"))
                    .build();

            assertThat(context.hasDeptPermission("20")).isTrue();
            assertThat(context.hasDeptPermission("30")).isTrue();
        }

        @Test
        @DisplayName("should return false when no permission")
        void shouldReturnFalse_whenNoPermission() {
            DataScopeContext context = DataScopeContext.builder()
                    .userId("1")
                    .deptId("10")
                    .build();

            assertThat(context.hasDeptPermission("999")).isFalse();
        }
    }

    @Nested
    @DisplayName("isSelfData")
    class IsSelfData {

        @Test
        @DisplayName("should return true when allDataPermission is true")
        void shouldReturnTrue_whenAllDataPermissionIsTrue() {
            DataScopeContext context = DataScopeContext.allPermission("1");

            assertThat(context.isSelfData("999")).isTrue();
        }

        @Test
        @DisplayName("should return true when ignoreDataScope is true")
        void shouldReturnTrue_whenIgnoreDataScopeIsTrue() {
            DataScopeContext context = DataScopeContext.builder()
                    .ignoreDataScope(true)
                    .build();

            assertThat(context.isSelfData("999")).isTrue();
        }

        @Test
        @DisplayName("should return true when userId matches")
        void shouldReturnTrue_whenUserIdMatches() {
            DataScopeContext context = DataScopeContext.builder()
                    .userId("1")
                    .build();

            assertThat(context.isSelfData("1")).isTrue();
        }

        @Test
        @DisplayName("should return false when userId does not match")
        void shouldReturnFalse_whenUserIdDoesNotMatch() {
            DataScopeContext context = DataScopeContext.builder()
                    .userId("1")
                    .build();

            assertThat(context.isSelfData("2")).isFalse();
        }

        @Test
        @DisplayName("should return false when userId is null")
        void shouldReturnFalse_whenUserIdIsNull() {
            DataScopeContext context = DataScopeContext.empty();

            assertThat(context.isSelfData("1")).isFalse();
        }
    }

    @Nested
    @DisplayName("accessibleDeptIds")
    class AccessibleDeptIds {

        @Test
        @DisplayName("should add accessible dept id")
        void shouldAddAccessibleDeptId() {
            DataScopeContext context = DataScopeContext.empty();
            context.addAccessibleDeptId("10");

            assertThat(context.getAccessibleDeptIds()).contains("10");
        }

        @Test
        @DisplayName("should add multiple accessible dept ids")
        void shouldAddMultipleAccessibleDeptIds() {
            DataScopeContext context = DataScopeContext.empty();
            context.addAccessibleDeptIds(Set.of("10", "20", "30"));

            assertThat(context.getAccessibleDeptIds()).containsExactlyInAnyOrder("10", "20", "30");
        }

        @Test
        @DisplayName("should return unmodifiable set")
        void shouldReturnUnmodifiableSet() {
            DataScopeContext context = DataScopeContext.empty();
            context.addAccessibleDeptId("10");

            Set<String> deptIds = context.getAccessibleDeptIds();
            assertThatThrownBy(() -> deptIds.add("99"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
