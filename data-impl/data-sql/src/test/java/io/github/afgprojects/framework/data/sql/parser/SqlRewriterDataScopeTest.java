package io.github.afgprojects.framework.data.sql.parser;

import io.github.afgprojects.framework.data.core.dialect.DatabaseType;
import io.github.afgprojects.framework.data.core.scope.DataScope;
import io.github.afgprojects.framework.data.core.scope.DataScopeType;
import io.github.afgprojects.framework.data.core.sql.SqlRewriteContext;
import io.github.afgprojects.framework.data.sql.scope.DataScopeContextProvider;
import io.github.afgprojects.framework.data.sql.scope.DataScopeUserContext;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SqlRewriter 数据权限测试
 */
@DisplayName("SqlRewriter DataScope Tests")
class SqlRewriterDataScopeTest {

    @Nested
    @DisplayName("DataScope Type Tests")
    class DataScopeTypeTests {

        @Test
        @DisplayName("SELF 类型权限 - 解析用户ID")
        void testSelfDataScope() throws Exception {
            // 准备上下文
            DataScopeUserContext userContext = DataScopeUserContext.builder()
                    .userId(123L)
                    .build();

            DataScopeContextProvider provider = () -> userContext;

            // 准备数据权限
            DataScope dataScope = DataScope.of("users", "create_by", DataScopeType.SELF);
            SqlRewriteContext context = createMockContext(List.of(dataScope));

            // 执行改写
            Statement statement = CCJSqlParserUtil.parse("SELECT * FROM users");
            SqlRewriter rewriter = new SqlRewriter(statement, context, provider);
            String result = rewriter.rewrite();

            // 验证
            assertThat(result).contains("create_by = 123");
        }

        @Test
        @DisplayName("DEPT 类型权限 - 解析部门ID")
        void testDeptDataScope() throws Exception {
            DataScopeUserContext userContext = DataScopeUserContext.builder()
                    .userId(123L)
                    .deptId(456L)
                    .build();

            DataScopeContextProvider provider = () -> userContext;

            DataScope dataScope = DataScope.of("users", "dept_id", DataScopeType.DEPT);
            SqlRewriteContext context = createMockContext(List.of(dataScope));

            Statement statement = CCJSqlParserUtil.parse("SELECT * FROM users");
            SqlRewriter rewriter = new SqlRewriter(statement, context, provider);
            String result = rewriter.rewrite();

            assertThat(result).contains("dept_id = 456");
        }

        @Test
        @DisplayName("DEPT_AND_CHILD 类型权限 - 解析部门ID列表")
        void testDeptAndChildDataScope() throws Exception {
            DataScopeUserContext userContext = DataScopeUserContext.builder()
                    .userId(123L)
                    .deptId(456L)
                    .accessibleDeptIds(Set.of(100L, 200L, 300L))
                    .build();

            DataScopeContextProvider provider = () -> userContext;

            DataScope dataScope = DataScope.of("users", "dept_id", DataScopeType.DEPT_AND_CHILD);
            SqlRewriteContext context = createMockContext(List.of(dataScope));

            Statement statement = CCJSqlParserUtil.parse("SELECT * FROM users");
            SqlRewriter rewriter = new SqlRewriter(statement, context, provider);
            String result = rewriter.rewrite();

            // 应该生成 IN 条件
            assertThat(result).contains("dept_id IN");
            assertThat(result).contains("100");
            assertThat(result).contains("200");
            assertThat(result).contains("300");
        }

        @Test
        @DisplayName("ALL 类型权限 - 不添加条件")
        void testAllDataScope() throws Exception {
            DataScopeUserContext userContext = DataScopeUserContext.builder()
                    .userId(123L)
                    .build();

            DataScopeContextProvider provider = () -> userContext;

            DataScope dataScope = DataScope.of("users", "dept_id", DataScopeType.ALL);
            SqlRewriteContext context = createMockContext(List.of(dataScope));

            Statement statement = CCJSqlParserUtil.parse("SELECT * FROM users");
            SqlRewriter rewriter = new SqlRewriter(statement, context, provider);
            String result = rewriter.rewrite();

            // ALL 类型不添加任何条件
            assertThat(result).isEqualTo("SELECT * FROM users");
        }

        @Test
        @DisplayName("CUSTOM 类型权限 - 解析占位符")
        void testCustomDataScope() throws Exception {
            DataScopeUserContext userContext = DataScopeUserContext.builder()
                    .userId(123L)
                    .deptId(456L)
                    .build();

            DataScopeContextProvider provider = () -> userContext;

            DataScope dataScope = new DataScope(
                    "users", "dept_id", DataScopeType.CUSTOM,
                    "user_id = #{currentUserId} AND dept_id = #{currentDeptId}",
                    null
            );
            SqlRewriteContext context = createMockContext(List.of(dataScope));

            Statement statement = CCJSqlParserUtil.parse("SELECT * FROM users");
            SqlRewriter rewriter = new SqlRewriter(statement, context, provider);
            String result = rewriter.rewrite();

            assertThat(result).contains("user_id = 123");
            assertThat(result).contains("dept_id = 456");
        }
    }

    @Nested
    @DisplayName("Statement Type Tests")
    class StatementTypeTests {

        @Test
        @DisplayName("UPDATE 语句添加数据权限条件")
        void testUpdateStatement() throws Exception {
            DataScopeUserContext userContext = DataScopeUserContext.builder()
                    .userId(123L)
                    .build();

            DataScopeContextProvider provider = () -> userContext;

            DataScope dataScope = DataScope.of("users", "create_by", DataScopeType.SELF);
            SqlRewriteContext context = createMockContext(List.of(dataScope));

            Statement statement = CCJSqlParserUtil.parse("UPDATE users SET name = 'test'");
            SqlRewriter rewriter = new SqlRewriter(statement, context, provider);
            String result = rewriter.rewrite();

            assertThat(result).contains("WHERE");
            assertThat(result).contains("create_by = 123");
        }

        @Test
        @DisplayName("DELETE 语句添加数据权限条件")
        void testDeleteStatement() throws Exception {
            DataScopeUserContext userContext = DataScopeUserContext.builder()
                    .userId(123L)
                    .build();

            DataScopeContextProvider provider = () -> userContext;

            DataScope dataScope = DataScope.of("users", "create_by", DataScopeType.SELF);
            SqlRewriteContext context = createMockContext(List.of(dataScope));

            Statement statement = CCJSqlParserUtil.parse("DELETE FROM users");
            SqlRewriter rewriter = new SqlRewriter(statement, context, provider);
            String result = rewriter.rewrite();

            assertThat(result).contains("WHERE");
            assertThat(result).contains("create_by = 123");
        }

        @Test
        @DisplayName("SELECT with existing WHERE clause")
        void testSelectWithExistingWhere() throws Exception {
            DataScopeUserContext userContext = DataScopeUserContext.builder()
                    .userId(123L)
                    .build();

            DataScopeContextProvider provider = () -> userContext;

            DataScope dataScope = DataScope.of("users", "create_by", DataScopeType.SELF);
            SqlRewriteContext context = createMockContext(List.of(dataScope));

            Statement statement = CCJSqlParserUtil.parse("SELECT * FROM users WHERE status = 1");
            SqlRewriter rewriter = new SqlRewriter(statement, context, provider);
            String result = rewriter.rewrite();

            assertThat(result).contains("status = 1");
            assertThat(result).contains("create_by = 123");
            assertThat(result).contains("AND");
        }
    }

    @Nested
    @DisplayName("Special Cases Tests")
    class SpecialCasesTests {

        @Test
        @DisplayName("用户拥有全部数据权限 - 不添加条件")
        void testAllDataPermission() throws Exception {
            DataScopeUserContext userContext = DataScopeUserContext.builder()
                    .userId(123L)
                    .allDataPermission(true)
                    .build();

            DataScopeContextProvider provider = () -> userContext;

            DataScope dataScope = DataScope.of("users", "dept_id", DataScopeType.DEPT);
            SqlRewriteContext context = createMockContext(List.of(dataScope));

            Statement statement = CCJSqlParserUtil.parse("SELECT * FROM users");
            SqlRewriter rewriter = new SqlRewriter(statement, context, provider);
            String result = rewriter.rewrite();

            // 用户拥有全部权限，不添加条件
            assertThat(result).isEqualTo("SELECT * FROM users");
        }

        @Test
        @DisplayName("无 DataScopeProvider 时保持占位符")
        void testNoProvider() throws Exception {
            DataScope dataScope = DataScope.of("users", "create_by", DataScopeType.SELF);
            SqlRewriteContext context = createMockContext(List.of(dataScope));

            Statement statement = CCJSqlParserUtil.parse("SELECT * FROM users");
            SqlRewriter rewriter = new SqlRewriter(statement, context, null);
            String result = rewriter.rewrite();

            // 没有提供者时，使用占位符
            assertThat(result).contains("create_by = '#{currentUserId}'");
        }

        @Test
        @DisplayName("多个数据权限条件")
        void testMultipleDataScopes() throws Exception {
            DataScopeUserContext userContext = DataScopeUserContext.builder()
                    .userId(123L)
                    .deptId(456L)
                    .build();

            DataScopeContextProvider provider = () -> userContext;

            DataScope scope1 = DataScope.of("users", "create_by", DataScopeType.SELF);
            DataScope scope2 = DataScope.of("users", "dept_id", DataScopeType.DEPT);
            SqlRewriteContext context = createMockContext(List.of(scope1, scope2));

            Statement statement = CCJSqlParserUtil.parse("SELECT * FROM users");
            SqlRewriter rewriter = new SqlRewriter(statement, context, provider);
            String result = rewriter.rewrite();

            assertThat(result).contains("create_by = 123");
            assertThat(result).contains("dept_id = 456");
            assertThat(result).contains("AND");
        }

        @Test
        @DisplayName("表别名支持")
        void testTableAlias() throws Exception {
            DataScopeUserContext userContext = DataScopeUserContext.builder()
                    .userId(123L)
                    .build();

            DataScopeContextProvider provider = () -> userContext;

            DataScope dataScope = DataScope.of("users", "create_by", DataScopeType.SELF);
            SqlRewriteContext context = new MockSqlRewriteContext(List.of(dataScope)) {
                @Override
                public @Nullable String getTableAlias(@NonNull String tableName) {
                    if ("users".equals(tableName)) {
                        return "u";
                    }
                    return null;
                }
            };

            Statement statement = CCJSqlParserUtil.parse("SELECT * FROM users u");
            SqlRewriter rewriter = new SqlRewriter(statement, context, provider);
            String result = rewriter.rewrite();

            assertThat(result).contains("u.create_by = 123");
        }

        @Test
        @DisplayName("空数据范围列表")
        void testEmptyDataScopes() throws Exception {
            DataScopeUserContext userContext = DataScopeUserContext.builder()
                    .userId(123L)
                    .build();

            DataScopeContextProvider provider = () -> userContext;
            SqlRewriteContext context = createMockContext(Collections.emptyList());

            Statement statement = CCJSqlParserUtil.parse("SELECT * FROM users");
            SqlRewriter rewriter = new SqlRewriter(statement, context, provider);
            String result = rewriter.rewrite();

            assertThat(result).isEqualTo("SELECT * FROM users");
        }

        @Test
        @DisplayName("DEPT_AND_CHILD with empty accessibleDeptIds uses deptId")
        void testDeptAndChildWithEmptyDeptIds() throws Exception {
            DataScopeUserContext userContext = DataScopeUserContext.builder()
                    .userId(123L)
                    .deptId(456L)
                    .accessibleDeptIds(Set.of())
                    .build();

            DataScopeContextProvider provider = () -> userContext;

            DataScope dataScope = DataScope.of("users", "dept_id", DataScopeType.DEPT_AND_CHILD);
            SqlRewriteContext context = createMockContext(List.of(dataScope));

            Statement statement = CCJSqlParserUtil.parse("SELECT * FROM users");
            SqlRewriter rewriter = new SqlRewriter(statement, context, provider);
            String result = rewriter.rewrite();

            // 应该使用 deptId 作为单值条件
            assertThat(result).contains("dept_id = 456");
        }

        @Test
        @DisplayName("SELF with null userId uses MIN_VALUE")
        void testSelfWithNullUserId() throws Exception {
            DataScopeUserContext userContext = DataScopeUserContext.builder().build();
            DataScopeContextProvider provider = () -> userContext;

            DataScope dataScope = DataScope.of("users", "create_by", DataScopeType.SELF);
            SqlRewriteContext context = createMockContext(List.of(dataScope));

            Statement statement = CCJSqlParserUtil.parse("SELECT * FROM users");
            SqlRewriter rewriter = new SqlRewriter(statement, context, provider);
            String result = rewriter.rewrite();

            // 应该使用 Long.MIN_VALUE 作为不可能匹配的值（带引号）
            assertThat(result).contains("create_by = '" + Long.MIN_VALUE + "'");
        }

        @Test
        @DisplayName("DEPT with null deptId uses MIN_VALUE")
        void testDeptWithNullDeptId() throws Exception {
            DataScopeUserContext userContext = DataScopeUserContext.builder()
                    .userId(123L)
                    .build();
            DataScopeContextProvider provider = () -> userContext;

            DataScope dataScope = DataScope.of("users", "dept_id", DataScopeType.DEPT);
            SqlRewriteContext context = createMockContext(List.of(dataScope));

            Statement statement = CCJSqlParserUtil.parse("SELECT * FROM users");
            SqlRewriter rewriter = new SqlRewriter(statement, context, provider);
            String result = rewriter.rewrite();

            assertThat(result).contains("dept_id = '" + Long.MIN_VALUE + "'");
        }
    }

    @Nested
    @DisplayName("Tenant and SoftDelete Tests")
    class TenantAndSoftDeleteTests {

        @Test
        @DisplayName("租户过滤")
        void testTenantFilter() throws Exception {
            DataScopeUserContext userContext = DataScopeUserContext.builder()
                    .userId(123L)
                    .tenantId(999L)
                    .build();

            DataScopeContextProvider provider = () -> userContext;

            SqlRewriteContext context = new MockSqlRewriteContext(Collections.emptyList()) {
                @Override
                public @Nullable String getTenantId() {
                    return "999";
                }

                @Override
                public boolean isIgnoreTenant() {
                    return false;
                }
            };

            Statement statement = CCJSqlParserUtil.parse("SELECT * FROM users");
            SqlRewriter rewriter = new SqlRewriter(statement, context, provider);
            String result = rewriter.rewrite();

            assertThat(result).contains("tenant_id = '999'");
        }

        @Test
        @DisplayName("软删除过滤")
        void testSoftDeleteFilter() throws Exception {
            DataScopeUserContext userContext = DataScopeUserContext.builder()
                    .userId(123L)
                    .build();

            DataScopeContextProvider provider = () -> userContext;

            SqlRewriteContext context = new MockSqlRewriteContext(Collections.emptyList()) {
                @Override
                public boolean isSoftDeleteFilter() {
                    return true;
                }

                @Override
                public @Nullable String getSoftDeleteColumn() {
                    return "deleted";
                }

                @Override
                public @Nullable String getDeletedValue() {
                    return "1";
                }
            };

            Statement statement = CCJSqlParserUtil.parse("SELECT * FROM users");
            SqlRewriter rewriter = new SqlRewriter(statement, context, provider);
            String result = rewriter.rewrite();

            // JSqlParser 使用 <> 表示不等于
            assertThat(result).contains("deleted <> '1'");
        }

        @Test
        @DisplayName("租户过滤和软删除组合")
        void testTenantAndSoftDeleteCombined() throws Exception {
            DataScopeUserContext userContext = DataScopeUserContext.builder()
                    .userId(123L)
                    .tenantId(999L)
                    .build();

            DataScopeContextProvider provider = () -> userContext;

            SqlRewriteContext context = new MockSqlRewriteContext(Collections.emptyList()) {
                @Override
                public @Nullable String getTenantId() {
                    return "999";
                }

                @Override
                public boolean isIgnoreTenant() {
                    return false;
                }

                @Override
                public boolean isSoftDeleteFilter() {
                    return true;
                }

                @Override
                public @Nullable String getSoftDeleteColumn() {
                    return "deleted";
                }

                @Override
                public @Nullable String getDeletedValue() {
                    return "1";
                }
            };

            Statement statement = CCJSqlParserUtil.parse("SELECT * FROM users");
            SqlRewriter rewriter = new SqlRewriter(statement, context, provider);
            String result = rewriter.rewrite();

            assertThat(result).contains("tenant_id = '999'");
            assertThat(result).contains("deleted <> '1'");
            assertThat(result).contains("AND");
        }

        @Test
        @DisplayName("忽略租户过滤")
        void testIgnoreTenant() throws Exception {
            DataScopeUserContext userContext = DataScopeUserContext.builder()
                    .userId(123L)
                    .tenantId(999L)
                    .build();

            DataScopeContextProvider provider = () -> userContext;

            SqlRewriteContext context = new MockSqlRewriteContext(Collections.emptyList()) {
                @Override
                public @Nullable String getTenantId() {
                    return "999";
                }

                @Override
                public boolean isIgnoreTenant() {
                    return true; // 忽略租户
                }
            };

            Statement statement = CCJSqlParserUtil.parse("SELECT * FROM users");
            SqlRewriter rewriter = new SqlRewriter(statement, context, provider);
            String result = rewriter.rewrite();

            assertThat(result).doesNotContain("tenant_id");
            assertThat(result).isEqualTo("SELECT * FROM users");
        }
    }

    @Nested
    @DisplayName("Constructor Without Provider Tests")
    class ConstructorWithoutProviderTests {

        @Test
        @DisplayName("使用无 Provider 构造函数 - SELF 类型使用占位符")
        void testConstructorWithoutProvider() throws Exception {
            DataScope dataScope = DataScope.of("users", "create_by", DataScopeType.SELF);
            SqlRewriteContext context = createMockContext(List.of(dataScope));

            Statement statement = CCJSqlParserUtil.parse("SELECT * FROM users");
            // 使用无 Provider 的构造函数
            SqlRewriter rewriter = new SqlRewriter(statement, context);
            String result = rewriter.rewrite();

            // 应该使用占位符
            assertThat(result).contains("create_by = '#{currentUserId}'");
        }

        @Test
        @DisplayName("使用无 Provider 构造函数 - DEPT_AND_CHILD 类型使用占位符")
        void testConstructorWithoutProviderDeptAndChild() throws Exception {
            DataScope dataScope = DataScope.of("users", "dept_id", DataScopeType.DEPT_AND_CHILD);
            SqlRewriteContext context = createMockContext(List.of(dataScope));

            Statement statement = CCJSqlParserUtil.parse("SELECT * FROM users");
            SqlRewriter rewriter = new SqlRewriter(statement, context);
            String result = rewriter.rewrite();

            // 应该使用 IN 条件和占位符
            assertThat(result).contains("dept_id IN");
            assertThat(result).contains("#{currentUserDeptAndChildIds}");
        }

        @Test
        @DisplayName("使用无 Provider 构造函数 - DEPT 类型使用占位符")
        void testConstructorWithoutProviderDept() throws Exception {
            DataScope dataScope = DataScope.of("users", "dept_id", DataScopeType.DEPT);
            SqlRewriteContext context = createMockContext(List.of(dataScope));

            Statement statement = CCJSqlParserUtil.parse("SELECT * FROM users");
            SqlRewriter rewriter = new SqlRewriter(statement, context);
            String result = rewriter.rewrite();

            assertThat(result).contains("dept_id = '#{currentDeptId}'");
        }

        @Test
        @DisplayName("使用无 Provider 构造函数 - ALL 类型不添加条件")
        void testConstructorWithoutProviderAll() throws Exception {
            DataScope dataScope = DataScope.of("users", "dept_id", DataScopeType.ALL);
            SqlRewriteContext context = createMockContext(List.of(dataScope));

            Statement statement = CCJSqlParserUtil.parse("SELECT * FROM users");
            SqlRewriter rewriter = new SqlRewriter(statement, context);
            String result = rewriter.rewrite();

            // ALL 类型不添加任何条件
            assertThat(result).isEqualTo("SELECT * FROM users");
        }
    }

    @Nested
    @DisplayName("Custom Condition Tests")
    class CustomConditionTests {

        @Test
        @DisplayName("CUSTOM 类型 - 无自定义条件返回 null")
        void testCustomConditionNull() throws Exception {
            DataScopeUserContext userContext = DataScopeUserContext.builder()
                    .userId(123L)
                    .build();

            DataScopeContextProvider provider = () -> userContext;

            DataScope dataScope = new DataScope("users", "dept_id", DataScopeType.CUSTOM, null, null);
            SqlRewriteContext context = createMockContext(List.of(dataScope));

            Statement statement = CCJSqlParserUtil.parse("SELECT * FROM users");
            SqlRewriter rewriter = new SqlRewriter(statement, context, provider);
            String result = rewriter.rewrite();

            // 无自定义条件，不添加任何过滤
            assertThat(result).isEqualTo("SELECT * FROM users");
        }

        @Test
        @DisplayName("CUSTOM 类型 - 空自定义条件返回 null")
        void testCustomConditionEmpty() throws Exception {
            DataScopeUserContext userContext = DataScopeUserContext.builder()
                    .userId(123L)
                    .build();

            DataScopeContextProvider provider = () -> userContext;

            DataScope dataScope = new DataScope("users", "dept_id", DataScopeType.CUSTOM, "", null);
            SqlRewriteContext context = createMockContext(List.of(dataScope));

            Statement statement = CCJSqlParserUtil.parse("SELECT * FROM users");
            SqlRewriter rewriter = new SqlRewriter(statement, context, provider);
            String result = rewriter.rewrite();

            assertThat(result).isEqualTo("SELECT * FROM users");
        }

        @Test
        @DisplayName("CUSTOM 类型 - 复杂条件表达式")
        void testCustomConditionComplex() throws Exception {
            DataScopeUserContext userContext = DataScopeUserContext.builder()
                    .userId(123L)
                    .deptId(456L)
                    .build();

            DataScopeContextProvider provider = () -> userContext;

            DataScope dataScope = new DataScope(
                    "users", "dept_id", DataScopeType.CUSTOM,
                    "(user_id = #{currentUserId} OR dept_id = #{currentDeptId})",
                    null
            );
            SqlRewriteContext context = createMockContext(List.of(dataScope));

            Statement statement = CCJSqlParserUtil.parse("SELECT * FROM users");
            SqlRewriter rewriter = new SqlRewriter(statement, context, provider);
            String result = rewriter.rewrite();

            assertThat(result).contains("user_id = 123");
            assertThat(result).contains("dept_id = 456");
        }
    }

    @Nested
    @DisplayName("Ignore DataScope Tests")
    class IgnoreDataScopeTests {

        @Test
        @DisplayName("忽略数据权限过滤")
        void testIgnoreDataScope() throws Exception {
            DataScopeUserContext userContext = DataScopeUserContext.builder()
                    .userId(123L)
                    .build();

            DataScopeContextProvider provider = () -> userContext;

            DataScope dataScope = DataScope.of("users", "create_by", DataScopeType.SELF);
            SqlRewriteContext context = new MockSqlRewriteContext(List.of(dataScope)) {
                @Override
                public boolean isIgnoreDataScope() {
                    return true; // 忽略数据权限
                }
            };

            Statement statement = CCJSqlParserUtil.parse("SELECT * FROM users");
            SqlRewriter rewriter = new SqlRewriter(statement, context, provider);
            String result = rewriter.rewrite();

            assertThat(result).doesNotContain("create_by");
            assertThat(result).isEqualTo("SELECT * FROM users");
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("DEPT_AND_CHILD with single deptId in list")
        void testDeptAndChildWithSingleDeptId() throws Exception {
            DataScopeUserContext userContext = DataScopeUserContext.builder()
                    .userId(123L)
                    .deptId(456L)
                    .accessibleDeptIds(Set.of(100L))
                    .build();

            DataScopeContextProvider provider = () -> userContext;

            DataScope dataScope = DataScope.of("users", "dept_id", DataScopeType.DEPT_AND_CHILD);
            SqlRewriteContext context = createMockContext(List.of(dataScope));

            Statement statement = CCJSqlParserUtil.parse("SELECT * FROM users");
            SqlRewriter rewriter = new SqlRewriter(statement, context, provider);
            String result = rewriter.rewrite();

            // 单个部门ID也应该使用 IN 条件
            assertThat(result).contains("dept_id IN");
            assertThat(result).contains("100");
        }

        @Test
        @DisplayName("SELECT with complex WHERE clause")
        void testSelectWithComplexWhere() throws Exception {
            DataScopeUserContext userContext = DataScopeUserContext.builder()
                    .userId(123L)
                    .build();

            DataScopeContextProvider provider = () -> userContext;

            DataScope dataScope = DataScope.of("users", "create_by", DataScopeType.SELF);
            SqlRewriteContext context = createMockContext(List.of(dataScope));

            Statement statement = CCJSqlParserUtil.parse("SELECT * FROM users WHERE status = 1 AND type = 'admin'");
            SqlRewriter rewriter = new SqlRewriter(statement, context, provider);
            String result = rewriter.rewrite();

            assertThat(result).contains("status = 1");
            assertThat(result).contains("type = 'admin'");
            assertThat(result).contains("create_by = 123");
            assertThat(result).contains("AND");
        }

        @Test
        @DisplayName("UPDATE with existing WHERE and data scope")
        void testUpdateWithExistingWhereAndDataScope() throws Exception {
            DataScopeUserContext userContext = DataScopeUserContext.builder()
                    .userId(123L)
                    .build();

            DataScopeContextProvider provider = () -> userContext;

            DataScope dataScope = DataScope.of("users", "create_by", DataScopeType.SELF);
            SqlRewriteContext context = createMockContext(List.of(dataScope));

            Statement statement = CCJSqlParserUtil.parse("UPDATE users SET name = 'test' WHERE id = 1");
            SqlRewriter rewriter = new SqlRewriter(statement, context, provider);
            String result = rewriter.rewrite();

            assertThat(result).contains("id = 1");
            assertThat(result).contains("create_by = 123");
            assertThat(result).contains("AND");
        }

        @Test
        @DisplayName("DELETE with existing WHERE and data scope")
        void testDeleteWithExistingWhereAndDataScope() throws Exception {
            DataScopeUserContext userContext = DataScopeUserContext.builder()
                    .userId(123L)
                    .build();

            DataScopeContextProvider provider = () -> userContext;

            DataScope dataScope = DataScope.of("users", "create_by", DataScopeType.SELF);
            SqlRewriteContext context = createMockContext(List.of(dataScope));

            Statement statement = CCJSqlParserUtil.parse("DELETE FROM users WHERE id = 1");
            SqlRewriter rewriter = new SqlRewriter(statement, context, provider);
            String result = rewriter.rewrite();

            assertThat(result).contains("id = 1");
            assertThat(result).contains("create_by = 123");
            assertThat(result).contains("AND");
        }
    }

    /**
     * 创建模拟的 SqlRewriteContext
     */
    private SqlRewriteContext createMockContext(List<DataScope> dataScopes) {
        return new MockSqlRewriteContext(dataScopes);
    }

    /**
     * 模拟的 SqlRewriteContext 实现
     */
    private static class MockSqlRewriteContext implements SqlRewriteContext {
        private final List<DataScope> dataScopes;

        MockSqlRewriteContext(List<DataScope> dataScopes) {
            this.dataScopes = dataScopes;
        }

        @Override
        public @NonNull DatabaseType getDatabaseType() {
            return DatabaseType.MYSQL;
        }

        @Override
        public @NonNull List<DataScope> getDataScopes() {
            return dataScopes;
        }

        @Override
        public @Nullable String getTenantId() {
            return null;
        }

        @Override
        public boolean isSoftDeleteFilter() {
            return false;
        }

        @Override
        public @Nullable String getSoftDeleteColumn() {
            return null;
        }

        @Override
        public @Nullable String getDeletedValue() {
            return null;
        }

        @Override
        public boolean isIgnoreDataScope() {
            return false;
        }

        @Override
        public boolean isIgnoreTenant() {
            return true;
        }

        @Override
        public @Nullable String getTableAlias(@NonNull String tableName) {
            return null;
        }
    }

    @Nested
    @DisplayName("SqlRewriter Edge Cases Tests")
    class SqlRewriterEdgeCasesTests {

        @Test
        @DisplayName("DEPT_AND_CHILD with null accessibleDeptIds and null deptId uses MIN_VALUE")
        void testDeptAndChildWithBothNull() throws Exception {
            DataScopeUserContext userContext = DataScopeUserContext.builder().build();
            DataScopeContextProvider provider = () -> userContext;

            DataScope dataScope = DataScope.of("users", "dept_id", DataScopeType.DEPT_AND_CHILD);
            SqlRewriteContext context = createMockContext(List.of(dataScope));

            Statement statement = CCJSqlParserUtil.parse("SELECT * FROM users");
            SqlRewriter rewriter = new SqlRewriter(statement, context, provider);
            String result = rewriter.rewrite();

            // Should use Long.MIN_VALUE as fallback
            assertThat(result).contains("dept_id = '" + Long.MIN_VALUE + "'");
        }

        @Test
        @DisplayName("CUSTOM without provider uses custom condition string")
        void testCustomWithoutProvider() throws Exception {
            DataScope dataScope = new DataScope(
                    "users", "dept_id", DataScopeType.CUSTOM,
                    "user_id = 123", null
            );
            SqlRewriteContext context = createMockContext(List.of(dataScope));

            Statement statement = CCJSqlParserUtil.parse("SELECT * FROM users");
            SqlRewriter rewriter = new SqlRewriter(statement, context);
            String result = rewriter.rewrite();

            assertThat(result).contains("user_id = 123");
        }

        @Test
        @DisplayName("CUSTOM with invalid expression returns null condition")
        void testCustomWithInvalidExpression() throws Exception {
            DataScopeUserContext userContext = DataScopeUserContext.builder().build();
            DataScopeContextProvider provider = () -> userContext;

            // Create a custom condition that will fail to parse
            DataScope dataScope = new DataScope(
                    "users", "dept_id", DataScopeType.CUSTOM,
                    "!!!INVALID_EXPRESSION!!!", null
            );
            SqlRewriteContext context = createMockContext(List.of(dataScope));

            Statement statement = CCJSqlParserUtil.parse("SELECT * FROM users");
            SqlRewriter rewriter = new SqlRewriter(statement, context, provider);
            String result = rewriter.rewrite();

            // Invalid expression returns null, so no condition is added
            assertThat(result).isEqualTo("SELECT * FROM users");
        }
    }
}