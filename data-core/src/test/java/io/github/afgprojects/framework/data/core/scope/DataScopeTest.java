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
package io.github.afgprojects.framework.data.core.scope;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * DataScope 单元测试
 * <p>
 * 测试数据权限配置的创建、验证和 Builder 模式。
 */
class DataScopeTest {

    // ==================== of() 工厂方法 ====================

    @Nested
    @DisplayName("of() 工厂方法")
    class OfFactoryMethod {

        @Test
        @DisplayName("should create instance when of called with valid args")
        void shouldCreateInstance_whenOfCalledWithValidArgs() {
            DataScope scope = DataScope.of("sys_user", "user_id", DataScopeType.SELF);

            assertThat(scope.table()).isEqualTo("sys_user");
            assertThat(scope.column()).isEqualTo("user_id");
            assertThat(scope.scopeType()).isEqualTo(DataScopeType.SELF);
            assertThat(scope.customCondition()).isNull();
            assertThat(scope.aliasPrefix()).isNull();
        }

        @Test
        @DisplayName("should create ALL type instance when of called")
        void shouldCreateAllTypeInstance_whenOfCalled() {
            DataScope scope = DataScope.of("sys_user", "user_id", DataScopeType.ALL);

            assertThat(scope.scopeType()).isEqualTo(DataScopeType.ALL);
        }

        @Test
        @DisplayName("should create DEPT type instance when of called")
        void shouldCreateDeptTypeInstance_whenOfCalled() {
            DataScope scope = DataScope.of("sys_user", "dept_id", DataScopeType.DEPT);

            assertThat(scope.scopeType()).isEqualTo(DataScopeType.DEPT);
        }

        @Test
        @DisplayName("should create DEPT_AND_CHILD type instance when of called")
        void shouldCreateDeptAndChildTypeInstance_whenOfCalled() {
            DataScope scope = DataScope.of("sys_user", "dept_id", DataScopeType.DEPT_AND_CHILD);

            assertThat(scope.scopeType()).isEqualTo(DataScopeType.DEPT_AND_CHILD);
        }

        @Test
        @DisplayName("should create CUSTOM type instance when of called")
        void shouldCreateCustomTypeInstance_whenOfCalled() {
            DataScope scope = DataScope.of("sys_user", "user_id", DataScopeType.CUSTOM);

            assertThat(scope.scopeType()).isEqualTo(DataScopeType.CUSTOM);
            assertThat(scope.customCondition()).isNull();
        }
    }

    // ==================== Builder 模式 ====================

    @Nested
    @DisplayName("Builder 模式")
    class BuilderPattern {

        @Test
        @DisplayName("should create instance when builder used with all fields")
        void shouldCreateInstance_whenBuilderUsedWithAllFields() {
            DataScope scope = DataScope.builder()
                .table("sys_user")
                .column("user_id")
                .scopeType(DataScopeType.SELF)
                .aliasPrefix("u")
                .build();

            assertThat(scope.table()).isEqualTo("sys_user");
            assertThat(scope.column()).isEqualTo("user_id");
            assertThat(scope.scopeType()).isEqualTo(DataScopeType.SELF);
            assertThat(scope.aliasPrefix()).isEqualTo("u");
        }

        @Test
        @DisplayName("should create instance with customCondition when builder used")
        void shouldCreateInstanceWithCustomCondition_whenBuilderUsed() {
            DataScope scope = DataScope.builder()
                .table("sys_user")
                .column("user_id")
                .scopeType(DataScopeType.CUSTOM)
                .customCondition("dept_id = #{currentDeptId}")
                .build();

            assertThat(scope.customCondition()).isEqualTo("dept_id = #{currentDeptId}");
        }
    }

    // ==================== customCondition 验证 ====================

    @Nested
    @DisplayName("customCondition 验证")
    class CustomConditionValidation {

        @Test
        @DisplayName("should throw exception when customCondition set with non-CUSTOM scopeType")
        void shouldThrowException_whenCustomConditionSetWithNonCustomScopeType() {
            assertThatThrownBy(() -> new DataScope("sys_user", "user_id", DataScopeType.SELF,
                    "dept_id = 1", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("customCondition can only be set when scopeType is CUSTOM");
        }

        @Test
        @DisplayName("should throw exception when customCondition set with DEPT scopeType")
        void shouldThrowException_whenCustomConditionSetWithDeptScopeType() {
            assertThatThrownBy(() -> new DataScope("sys_user", "dept_id", DataScopeType.DEPT,
                    "dept_id = 1", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("customCondition can only be set when scopeType is CUSTOM");
        }

        @Test
        @DisplayName("should throw exception when customCondition set with ALL scopeType")
        void shouldThrowException_whenCustomConditionSetWithAllScopeType() {
            assertThatThrownBy(() -> new DataScope("sys_user", "user_id", DataScopeType.ALL,
                    "1 = 1", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("customCondition can only be set when scopeType is CUSTOM");
        }

        @Test
        @DisplayName("should throw exception when customCondition contains SQL injection")
        void shouldThrowException_whenCustomConditionContainsSqlInjection() {
            assertThatThrownBy(() -> new DataScope("sys_user", "user_id", DataScopeType.CUSTOM,
                    "1 = 1; DROP TABLE users", null))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should throw exception when customCondition contains UNION")
        void shouldThrowException_whenCustomConditionContainsUnion() {
            assertThatThrownBy(() -> new DataScope("sys_user", "user_id", DataScopeType.CUSTOM,
                    "1 = 1 UNION SELECT * FROM users", null))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should throw exception when customCondition contains SELECT subquery")
        void shouldThrowException_whenCustomConditionContainsSelectSubquery() {
            assertThatThrownBy(() -> new DataScope("sys_user", "user_id", DataScopeType.CUSTOM,
                    "id IN (SELECT id FROM other)", null))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should pass validation when customCondition is safe with placeholder")
        void shouldPassValidation_whenCustomConditionIsSafeWithPlaceholder() {
            assertThatCode(() -> new DataScope("sys_user", "user_id", DataScopeType.CUSTOM,
                    "dept_id = #{currentDeptId}", null))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should pass validation when customCondition is safe with AND operator")
        void shouldPassValidation_whenCustomConditionIsSafeWithAndOperator() {
            assertThatCode(() -> new DataScope("sys_user", "user_id", DataScopeType.CUSTOM,
                    "dept_id = #{currentDeptId} AND status = 1", null))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should pass validation when customCondition is safe with IN operator")
        void shouldPassValidation_whenCustomConditionIsSafeWithInOperator() {
            assertThatCode(() -> new DataScope("sys_user", "user_id", DataScopeType.CUSTOM,
                    "user_id IN (1, 2, 3)", null))
                .doesNotThrowAnyException();
        }
    }

    // ==================== 无 customCondition ====================

    @Nested
    @DisplayName("无 customCondition")
    class WithoutCustomCondition {

        @Test
        @DisplayName("should create instance when no customCondition with SELF type")
        void shouldCreateInstance_whenNoCustomConditionWithSelfType() {
            assertThatCode(() -> DataScope.of("sys_user", "user_id", DataScopeType.SELF))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should create instance when no customCondition with DEPT type")
        void shouldCreateInstance_whenNoCustomConditionWithDeptType() {
            assertThatCode(() -> DataScope.of("sys_user", "dept_id", DataScopeType.DEPT))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should create instance when no customCondition with DEPT_AND_CHILD type")
        void shouldCreateInstance_whenNoCustomConditionWithDeptAndChildType() {
            assertThatCode(() -> DataScope.of("sys_user", "dept_id", DataScopeType.DEPT_AND_CHILD))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should create instance when no customCondition with ALL type")
        void shouldCreateInstance_whenNoCustomConditionWithAllType() {
            assertThatCode(() -> DataScope.of("sys_user", "user_id", DataScopeType.ALL))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should create instance when no customCondition with CUSTOM type")
        void shouldCreateInstance_whenNoCustomConditionWithCustomType() {
            assertThatCode(() -> DataScope.of("sys_user", "user_id", DataScopeType.CUSTOM))
                .doesNotThrowAnyException();
        }
    }
}
