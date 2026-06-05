/*
 * Copyright 2024 AFG Projects.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.afgprojects.framework.data.sql.scope;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * DataScopeUserContext 单元测试
 * <p>
 * 测试数据权限用户上下文的创建、Builder 模式和不可修改集合。
 */
class DataScopeUserContextTest {

    // ==================== Builder 创建 ====================

    @Nested
    @DisplayName("Builder 创建")
    class BuilderCreation {

        @Test
        @DisplayName("should create context with all fields when builder used")
        void shouldCreateContextWithAllFields_whenBuilderUsed() {
            DataScopeUserContext context = DataScopeUserContext.builder()
                .userId(123L)
                .deptId(456L)
                .accessibleDeptIds(Set.of(10L, 20L, 30L))
                .tenantId(789L)
                .allDataPermission(true)
                .build();

            assertThat(context.getUserId()).isEqualTo(123L);
            assertThat(context.getDeptId()).isEqualTo(456L);
            assertThat(context.getAccessibleDeptIds()).containsExactlyInAnyOrder(10L, 20L, 30L);
            assertThat(context.getTenantId()).isEqualTo(789L);
            assertThat(context.isAllDataPermission()).isTrue();
        }

        @Test
        @DisplayName("should return null userId when not set via builder")
        void shouldReturnNullUserId_whenNotSetViaBuilder() {
            DataScopeUserContext context = DataScopeUserContext.builder().build();

            assertThat(context.getUserId()).isNull();
        }

        @Test
        @DisplayName("should return null deptId when not set via builder")
        void shouldReturnNullDeptId_whenNotSetViaBuilder() {
            DataScopeUserContext context = DataScopeUserContext.builder().build();

            assertThat(context.getDeptId()).isNull();
        }

        @Test
        @DisplayName("should return null tenantId when not set via builder")
        void shouldReturnNullTenantId_whenNotSetViaBuilder() {
            DataScopeUserContext context = DataScopeUserContext.builder().build();

            assertThat(context.getTenantId()).isNull();
        }

        @Test
        @DisplayName("should return empty set for accessibleDeptIds when default builder used")
        void shouldReturnEmptySetForAccessibleDeptIds_whenDefaultBuilderUsed() {
            DataScopeUserContext context = DataScopeUserContext.builder().build();

            assertThat(context.getAccessibleDeptIds()).isEmpty();
        }

        @Test
        @DisplayName("should return false for allDataPermission when default builder used")
        void shouldReturnFalseForAllDataPermission_whenDefaultBuilderUsed() {
            DataScopeUserContext context = DataScopeUserContext.builder().build();

            assertThat(context.isAllDataPermission()).isFalse();
        }
    }

    // ==================== empty() 工厂方法 ====================

    @Nested
    @DisplayName("empty() 工厂方法")
    class EmptyFactoryMethod {

        @Test
        @DisplayName("should return empty context when empty called")
        void shouldReturnEmptyContext_whenEmptyCalled() {
            DataScopeUserContext context = DataScopeUserContext.empty();

            assertThat(context.getUserId()).isNull();
            assertThat(context.getDeptId()).isNull();
            assertThat(context.getAccessibleDeptIds()).isEmpty();
            assertThat(context.getTenantId()).isNull();
            assertThat(context.isAllDataPermission()).isFalse();
        }
    }

    // ==================== getAccessibleDeptIds 不可修改 ====================

    @Nested
    @DisplayName("getAccessibleDeptIds 不可修改")
    class GetAccessibleDeptIdsImmutable {

        @Test
        @DisplayName("should return unmodifiable set when getAccessibleDeptIds called")
        void shouldReturnUnmodifiableSet_whenGetAccessibleDeptIdsCalled() {
            DataScopeUserContext context = DataScopeUserContext.builder()
                .accessibleDeptIds(Set.of(10L, 20L))
                .build();

            Set<Long> deptIds = context.getAccessibleDeptIds();

            assertThatThrownBy(() -> deptIds.add(30L))
                .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("should return unmodifiable empty set when no accessibleDeptIds set")
        void shouldReturnUnmodifiableEmptySet_whenNoAccessibleDeptIdsSet() {
            DataScopeUserContext context = DataScopeUserContext.builder().build();

            Set<Long> deptIds = context.getAccessibleDeptIds();

            assertThatThrownBy(() -> deptIds.add(1L))
                .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    // ==================== isAllDataPermission ====================

    @Nested
    @DisplayName("isAllDataPermission")
    class IsAllDataPermission {

        @Test
        @DisplayName("should return true when allDataPermission is set to true")
        void shouldReturnTrue_whenAllDataPermissionIsSetToTrue() {
            DataScopeUserContext context = DataScopeUserContext.builder()
                .allDataPermission(true)
                .build();

            assertThat(context.isAllDataPermission()).isTrue();
        }

        @Test
        @DisplayName("should return false when allDataPermission is set to false")
        void shouldReturnFalse_whenAllDataPermissionIsSetToFalse() {
            DataScopeUserContext context = DataScopeUserContext.builder()
                .allDataPermission(false)
                .build();

            assertThat(context.isAllDataPermission()).isFalse();
        }

        @Test
        @DisplayName("should return false when allDataPermission not explicitly set")
        void shouldReturnFalse_whenAllDataPermissionNotExplicitlySet() {
            DataScopeUserContext context = DataScopeUserContext.builder().build();

            assertThat(context.isAllDataPermission()).isFalse();
        }
    }
}
