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

import io.github.afgprojects.framework.data.core.dialect.Dialect;
import io.github.afgprojects.framework.data.core.dialect.PostgreSQLDialect;
import io.github.afgprojects.framework.data.core.scope.DataScope;
import io.github.afgprojects.framework.data.core.scope.DataScopeType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * DataScopeSqlBuilder 单元测试
 * <p>
 * 测试数据权限 SQL 构建器的各种 scopeType 生成逻辑。
 */
class DataScopeSqlBuilderTest {

    private final Dialect dialect = new PostgreSQLDialect();

    // ==================== 空 list ====================

    @Nested
    @DisplayName("空 list")
    class EmptyList {

        @Test
        @DisplayName("should return null when dataScopes is empty")
        void shouldReturnNull_whenDataScopesIsEmpty() {
            DataScopeUserContext context = DataScopeUserContext.builder()
                .userId(1L)
                .build();
            DataScopeSqlBuilder builder = new DataScopeSqlBuilder(() -> context);

            DataScopeSqlBuilder.SqlResult result = builder.buildSql(List.of(), dialect);

            assertThat(result).isNull();
        }
    }

    // ==================== admin 用户 ====================

    @Nested
    @DisplayName("admin 用户")
    class AdminUser {

        @Test
        @DisplayName("should return null when user has allDataPermission")
        void shouldReturnNull_whenUserHasAllDataPermission() {
            DataScopeUserContext context = DataScopeUserContext.builder()
                .userId(1L)
                .allDataPermission(true)
                .build();
            DataScopeSqlBuilder builder = new DataScopeSqlBuilder(() -> context);
            List<DataScope> scopes = List.of(DataScope.of("sys_user", "user_id", DataScopeType.SELF));

            DataScopeSqlBuilder.SqlResult result = builder.buildSql(scopes, dialect);

            assertThat(result).isNull();
        }
    }

    // ==================== ALL 类型 ====================

    @Nested
    @DisplayName("ALL 类型")
    class AllType {

        @Test
        @DisplayName("should return null when scopeType is ALL")
        void shouldReturnNull_whenScopeTypeIsAll() {
            DataScopeUserContext context = DataScopeUserContext.builder()
                .userId(1L)
                .build();
            DataScopeSqlBuilder builder = new DataScopeSqlBuilder(() -> context);
            List<DataScope> scopes = List.of(DataScope.of("sys_user", "user_id", DataScopeType.ALL));

            DataScopeSqlBuilder.SqlResult result = builder.buildSql(scopes, dialect);

            assertThat(result).isNull();
        }
    }

    // ==================== SELF 类型 ====================

    @Nested
    @DisplayName("SELF 类型")
    class SelfType {

        @Test
        @DisplayName("should generate column = ? when scopeType is SELF")
        void shouldGenerateColumnEqualsPlaceholder_whenScopeTypeIsSelf() {
            DataScopeUserContext context = DataScopeUserContext.builder()
                .userId(123L)
                .build();
            DataScopeSqlBuilder builder = new DataScopeSqlBuilder(() -> context);
            List<DataScope> scopes = List.of(DataScope.of("sys_user", "user_id", DataScopeType.SELF));

            DataScopeSqlBuilder.SqlResult result = builder.buildSql(scopes, dialect);

            assertThat(result).isNotNull();
            assertThat(result.sql()).contains("= ?");
            assertThat(result.parameters()).containsExactly(123L);
        }

        @Test
        @DisplayName("should use Long MIN_VALUE when userId is null for SELF type")
        void shouldUseLongMinValue_whenUserIdIsNullForSelfType() {
            DataScopeUserContext context = DataScopeUserContext.builder().build();
            DataScopeSqlBuilder builder = new DataScopeSqlBuilder(() -> context);
            List<DataScope> scopes = List.of(DataScope.of("sys_user", "user_id", DataScopeType.SELF));

            DataScopeSqlBuilder.SqlResult result = builder.buildSql(scopes, dialect);

            assertThat(result).isNotNull();
            assertThat(result.sql()).contains(String.valueOf(Long.MIN_VALUE));
            assertThat(result.parameters()).isEmpty();
        }
    }

    // ==================== DEPT 类型 ====================

    @Nested
    @DisplayName("DEPT 类型")
    class DeptType {

        @Test
        @DisplayName("should generate column = ? when scopeType is DEPT")
        void shouldGenerateColumnEqualsPlaceholder_whenScopeTypeIsDept() {
            DataScopeUserContext context = DataScopeUserContext.builder()
                .deptId(456L)
                .build();
            DataScopeSqlBuilder builder = new DataScopeSqlBuilder(() -> context);
            List<DataScope> scopes = List.of(DataScope.of("sys_user", "dept_id", DataScopeType.DEPT));

            DataScopeSqlBuilder.SqlResult result = builder.buildSql(scopes, dialect);

            assertThat(result).isNotNull();
            assertThat(result.sql()).contains("= ?");
            assertThat(result.parameters()).containsExactly(456L);
        }

        @Test
        @DisplayName("should use Long MIN_VALUE when deptId is null for DEPT type")
        void shouldUseLongMinValue_whenDeptIdIsNullForDeptType() {
            DataScopeUserContext context = DataScopeUserContext.builder().build();
            DataScopeSqlBuilder builder = new DataScopeSqlBuilder(() -> context);
            List<DataScope> scopes = List.of(DataScope.of("sys_user", "dept_id", DataScopeType.DEPT));

            DataScopeSqlBuilder.SqlResult result = builder.buildSql(scopes, dialect);

            assertThat(result).isNotNull();
            assertThat(result.sql()).contains(String.valueOf(Long.MIN_VALUE));
            assertThat(result.parameters()).isEmpty();
        }
    }

    // ==================== DEPT_AND_CHILD 类型 ====================

    @Nested
    @DisplayName("DEPT_AND_CHILD 类型")
    class DeptAndChildType {

        @Test
        @DisplayName("should generate column = ? when single deptId for DEPT_AND_CHILD")
        void shouldGenerateColumnEqualsPlaceholder_whenSingleDeptIdForDeptAndChild() {
            DataScopeUserContext context = DataScopeUserContext.builder()
                .deptId(10L)
                .build();
            DataScopeSqlBuilder builder = new DataScopeSqlBuilder(() -> context);
            List<DataScope> scopes = List.of(DataScope.of("sys_user", "dept_id", DataScopeType.DEPT_AND_CHILD));

            DataScopeSqlBuilder.SqlResult result = builder.buildSql(scopes, dialect);

            assertThat(result).isNotNull();
            assertThat(result.sql()).contains("= ?");
            assertThat(result.parameters()).containsExactly(10L);
        }

        @Test
        @DisplayName("should generate column IN (?, ?) when multiple deptIds for DEPT_AND_CHILD")
        void shouldGenerateColumnInPlaceholders_whenMultipleDeptIdsForDeptAndChild() {
            DataScopeUserContext context = DataScopeUserContext.builder()
                .deptId(10L)
                .accessibleDeptIds(Set.of(20L, 30L))
                .build();
            DataScopeSqlBuilder builder = new DataScopeSqlBuilder(() -> context);
            List<DataScope> scopes = List.of(DataScope.of("sys_user", "dept_id", DataScopeType.DEPT_AND_CHILD));

            DataScopeSqlBuilder.SqlResult result = builder.buildSql(scopes, dialect);

            assertThat(result).isNotNull();
            assertThat(result.sql()).contains("IN (");
            assertThat(result.parameters()).hasSize(3); // deptId + accessibleDeptIds
            assertThat(result.parameters()).containsExactlyInAnyOrder(10L, 20L, 30L);
        }

        @Test
        @DisplayName("should use Long MIN_VALUE when no deptIds for DEPT_AND_CHILD")
        void shouldUseLongMinValue_whenNoDeptIdsForDeptAndChild() {
            DataScopeUserContext context = DataScopeUserContext.builder().build();
            DataScopeSqlBuilder builder = new DataScopeSqlBuilder(() -> context);
            List<DataScope> scopes = List.of(DataScope.of("sys_user", "dept_id", DataScopeType.DEPT_AND_CHILD));

            DataScopeSqlBuilder.SqlResult result = builder.buildSql(scopes, dialect);

            assertThat(result).isNotNull();
            assertThat(result.sql()).contains(String.valueOf(Long.MIN_VALUE));
            assertThat(result.parameters()).isEmpty();
        }
    }

    // ==================== CUSTOM 类型 ====================

    @Nested
    @DisplayName("CUSTOM 类型")
    class CustomType {

        @Test
        @DisplayName("should resolve placeholders when scopeType is CUSTOM")
        void shouldResolvePlaceholders_whenScopeTypeIsCustom() {
            DataScopeUserContext context = DataScopeUserContext.builder()
                .userId(123L)
                .build();
            DataScopeSqlBuilder builder = new DataScopeSqlBuilder(() -> context);
            List<DataScope> scopes = List.of(
                new DataScope("sys_user", "user_id", DataScopeType.CUSTOM,
                    "user_id = #{currentUserId}", null));

            DataScopeSqlBuilder.SqlResult result = builder.buildSql(scopes, dialect);

            assertThat(result).isNotNull();
            assertThat(result.sql()).contains("user_id = 123");
        }

        @Test
        @DisplayName("should return null when customCondition is null for CUSTOM type")
        void shouldReturnNull_whenCustomConditionIsNullForCustomType() {
            DataScopeUserContext context = DataScopeUserContext.builder()
                .userId(123L)
                .build();
            DataScopeSqlBuilder builder = new DataScopeSqlBuilder(() -> context);
            List<DataScope> scopes = List.of(DataScope.of("sys_user", "user_id", DataScopeType.CUSTOM));

            DataScopeSqlBuilder.SqlResult result = builder.buildSql(scopes, dialect);

            assertThat(result).isNull();
        }
    }

    // ==================== 多个 scope AND 组合 ====================

    @Nested
    @DisplayName("多个 scope AND 组合")
    class MultipleScopesAndCombination {

        @Test
        @DisplayName("should combine multiple scopes with AND when multiple scopes provided")
        void shouldCombineMultipleScopesWithAnd_whenMultipleScopesProvided() {
            DataScopeUserContext context = DataScopeUserContext.builder()
                .userId(123L)
                .deptId(456L)
                .build();
            DataScopeSqlBuilder builder = new DataScopeSqlBuilder(() -> context);
            List<DataScope> scopes = List.of(
                DataScope.of("sys_user", "user_id", DataScopeType.SELF),
                DataScope.of("sys_user", "dept_id", DataScopeType.DEPT));

            DataScopeSqlBuilder.SqlResult result = builder.buildSql(scopes, dialect);

            assertThat(result).isNotNull();
            assertThat(result.sql()).contains("AND");
            assertThat(result.parameters()).containsExactly(123L, 456L);
        }

        @Test
        @DisplayName("should skip ALL type scope when combining multiple scopes")
        void shouldSkipAllTypeScope_whenCombiningMultipleScopes() {
            DataScopeUserContext context = DataScopeUserContext.builder()
                .userId(123L)
                .build();
            DataScopeSqlBuilder builder = new DataScopeSqlBuilder(() -> context);
            List<DataScope> scopes = List.of(
                DataScope.of("sys_user", "user_id", DataScopeType.ALL),
                DataScope.of("sys_user", "dept_id", DataScopeType.SELF));

            DataScopeSqlBuilder.SqlResult result = builder.buildSql(scopes, dialect);

            assertThat(result).isNotNull();
            // Only SELF scope should be in the result, ALL is skipped
            assertThat(result.parameters()).hasSize(1);
        }
    }

    // ==================== aliasPrefix ====================

    @Nested
    @DisplayName("aliasPrefix")
    class AliasPrefix {

        @Test
        @DisplayName("should generate prefix.column format when aliasPrefix set")
        void shouldGeneratePrefixColumnFormat_whenAliasPrefixSet() {
            DataScopeUserContext context = DataScopeUserContext.builder()
                .userId(123L)
                .build();
            DataScopeSqlBuilder builder = new DataScopeSqlBuilder(() -> context);
            List<DataScope> scopes = List.of(
                DataScope.builder()
                    .table("sys_user")
                    .column("user_id")
                    .scopeType(DataScopeType.SELF)
                    .aliasPrefix("u")
                    .build());

            DataScopeSqlBuilder.SqlResult result = builder.buildSql(scopes, dialect);

            assertThat(result).isNotNull();
            // PostgreSQL quotes identifiers with double quotes
            assertThat(result.sql()).contains(".\"user_id\"");
            assertThat(result.sql()).contains("\"u\".");
        }

        @Test
        @DisplayName("should not add prefix when aliasPrefix is null")
        void shouldNotAddPrefix_whenAliasPrefixIsNull() {
            DataScopeUserContext context = DataScopeUserContext.builder()
                .userId(123L)
                .build();
            DataScopeSqlBuilder builder = new DataScopeSqlBuilder(() -> context);
            List<DataScope> scopes = List.of(DataScope.of("sys_user", "user_id", DataScopeType.SELF));

            DataScopeSqlBuilder.SqlResult result = builder.buildSql(scopes, dialect);

            assertThat(result).isNotNull();
            assertThat(result.sql()).doesNotContain(".");
        }
    }
}
