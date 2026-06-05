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
package io.github.afgprojects.framework.data.core.context;

import io.github.afgprojects.framework.data.core.scope.TenantScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * TenantContextHolder 单元测试
 * <p>
 * 测试多租户上下文的设置、获取、清除、作用域和快照功能。
 */
class TenantContextHolderTest {

    private TenantContextHolder holder;

    @BeforeEach
    void setUp() {
        holder = new TenantContextHolder();
    }

    @AfterEach
    void tearDown() {
        holder.clear();
    }

    // ==================== 基本操作 ====================

    @Nested
    @DisplayName("基本操作")
    class BasicOperations {

        @Test
        @DisplayName("should set and get tenant id when setTenantId called")
        void shouldSetAndGetTenantId_whenSetTenantIdCalled() {
            holder.setTenantId("tenant-001");

            assertThat(holder.getTenantId()).isEqualTo("tenant-001");
        }

        @Test
        @DisplayName("should clear context when set null tenant id")
        void shouldClearContext_whenSetNullTenantId() {
            holder.setTenantId("tenant-001");
            holder.setTenantId(null);

            assertThat(holder.getTenantId()).isNull();
        }

        @Test
        @DisplayName("should clear context when clear called")
        void shouldClearContext_whenClearCalled() {
            holder.setTenantId("tenant-001");
            holder.clear();

            assertThat(holder.getTenantId()).isNull();
        }

        @Test
        @DisplayName("should return null when no tenant set")
        void shouldReturnNull_whenNoTenantSet() {
            assertThat(holder.getTenantId()).isNull();
        }
    }

    // ==================== 作用域操作 ====================

    @Nested
    @DisplayName("作用域操作")
    class ScopeOperations {

        @Test
        @DisplayName("should create scope when call scope")
        void shouldCreateScope_whenCallScope() {
            TenantScope scope = holder.scope("tenant-002");

            assertThat(scope).isNotNull();
            assertThat(scope.getTenantId()).isEqualTo("tenant-002");
        }

        @Test
        @DisplayName("should restore previous context when scope closed")
        void shouldRestorePreviousContext_whenScopeClosed() {
            holder.setTenantId("tenant-001");

            try (TenantScope scope = holder.scope("tenant-002")) {
                assertThat(holder.getTenantId()).isEqualTo("tenant-002");
            }

            assertThat(holder.getTenantId()).isEqualTo("tenant-001");
        }

        @Test
        @DisplayName("should clear context when scope closed and no previous")
        void shouldClearContext_whenScopeClosedAndNoPrevious() {
            try (TenantScope scope = holder.scope("tenant-002")) {
                assertThat(holder.getTenantId()).isEqualTo("tenant-002");
            }

            assertThat(holder.getTenantId()).isNull();
        }

        @Test
        @DisplayName("should handle nested scopes when outer and inner set")
        void shouldHandleNestedScopes_whenOuterAndInnerSet() {
            holder.setTenantId("tenant-001");

            try (TenantScope scope1 = holder.scope("tenant-002")) {
                assertThat(holder.getTenantId()).isEqualTo("tenant-002");

                try (TenantScope scope2 = holder.scope("tenant-003")) {
                    assertThat(holder.getTenantId()).isEqualTo("tenant-003");
                }

                assertThat(holder.getTenantId()).isEqualTo("tenant-002");
            }

            assertThat(holder.getTenantId()).isEqualTo("tenant-001");
        }

        @Test
        @DisplayName("should handle scope with null tenant id")
        void shouldHandleScopeWithNullTenantId() {
            holder.setTenantId("tenant-001");

            try (TenantScope scope = holder.scope(null)) {
                assertThat(holder.getTenantId()).isNull();
            }

            assertThat(holder.getTenantId()).isEqualTo("tenant-001");
        }
    }

    // ==================== 快照操作 ====================

    @Nested
    @DisplayName("快照操作")
    class SnapshotOperations {

        @Test
        @DisplayName("should create snapshot when tenant set")
        void shouldCreateSnapshot_whenTenantSet() {
            holder.setTenantId("tenant-001");
            TenantContextHolder.TenantContextSnapshot snapshot = holder.snapshot();

            assertThat(snapshot).isNotNull();
            assertThat(snapshot.isValid()).isTrue();
            assertThat(snapshot.tenantId()).isEqualTo("tenant-001");
        }

        @Test
        @DisplayName("should return null snapshot when no tenant set")
        void shouldReturnNullSnapshot_whenNoTenantSet() {
            TenantContextHolder.TenantContextSnapshot snapshot = holder.snapshot();

            assertThat(snapshot).isNull();
        }

        @Test
        @DisplayName("should restore from snapshot when call restore")
        void shouldRestoreFromSnapshot_whenCallRestore() {
            holder.setTenantId("tenant-001");
            TenantContextHolder.TenantContextSnapshot snapshot = holder.snapshot();

            holder.setTenantId("tenant-002");
            holder.restore(snapshot);

            assertThat(holder.getTenantId()).isEqualTo("tenant-001");
        }

        @Test
        @DisplayName("should clear context when restore null snapshot")
        void shouldClearContext_whenRestoreNullSnapshot() {
            holder.setTenantId("tenant-001");
            holder.restore(null);

            assertThat(holder.getTenantId()).isNull();
        }

        @Test
        @DisplayName("should run in snapshot context when call runWithSnapshot")
        void shouldRunInSnapshotContext_whenCallRunWithSnapshot() {
            holder.setTenantId("tenant-001");
            TenantContextHolder.TenantContextSnapshot snapshot = holder.snapshot();

            holder.setTenantId("tenant-002");

            StringBuilder capturedTenant = new StringBuilder();
            holder.runWithSnapshot(snapshot, () -> {
                capturedTenant.append(holder.getTenantId());
            });

            assertThat(capturedTenant.toString()).isEqualTo("tenant-001");
        }

        @Test
        @DisplayName("should restore previous context after runWithSnapshot")
        void shouldRestorePreviousContext_afterRunWithSnapshot() {
            holder.setTenantId("tenant-001");
            TenantContextHolder.TenantContextSnapshot snapshot = holder.snapshot();

            holder.setTenantId("tenant-002");
            holder.runWithSnapshot(snapshot, () -> {
                // 执行一些操作
            });

            assertThat(holder.getTenantId()).isEqualTo("tenant-002");
        }

        @Test
        @DisplayName("should handle runWithSnapshot with null snapshot")
        void shouldHandleRunWithSnapshotWithNullSnapshot() {
            holder.setTenantId("tenant-001");

            holder.runWithSnapshot(null, () -> {
                assertThat(holder.getTenantId()).isNull();
            });

            assertThat(holder.getTenantId()).isEqualTo("tenant-001");
        }
    }

    // ==================== 边界情况 ====================

    @Nested
    @DisplayName("边界情况")
    class EdgeCases {

        @Test
        @DisplayName("should handle empty string tenant id")
        void shouldHandleEmptyStringTenantId() {
            holder.setTenantId("");

            assertThat(holder.getTenantId()).isEmpty();
        }

        @Test
        @DisplayName("should handle tenant id with special characters")
        void shouldHandleTenantIdWithSpecialCharacters() {
            String specialTenantId = "tenant-001_测试";
            holder.setTenantId(specialTenantId);

            assertThat(holder.getTenantId()).isEqualTo(specialTenantId);
        }

        @Test
        @DisplayName("should handle multiple set operations")
        void shouldHandleMultipleSetOperations() {
            holder.setTenantId("tenant-001");
            holder.setTenantId("tenant-002");
            holder.setTenantId("tenant-003");

            assertThat(holder.getTenantId()).isEqualTo("tenant-003");
        }

        @Test
        @DisplayName("should handle clear when already cleared")
        void shouldHandleClearWhenAlreadyCleared() {
            holder.clear();
            holder.clear();

            assertThat(holder.getTenantId()).isNull();
        }
    }
}
