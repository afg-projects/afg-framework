package io.github.afgprojects.framework.data.sql.builder;

import io.github.afgprojects.framework.data.core.dialect.MySQLDialect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * CteClauseBuilder 单元测试
 */
@DisplayName("CteClauseBuilder 测试")
class CteClauseBuilderTest {

    private final MySQLDialect dialect = new MySQLDialect();
    private CteClauseBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new CteClauseBuilder(dialect);
    }

    /**
     * 创建一个简单的 CTE 查询构建器（用于测试）
     */
    private SqlQueryBuilderImpl simpleCteQuery() {
        return new SqlQueryBuilderImpl(dialect);
    }

    @Nested
    @DisplayName("空构建器")
    class EmptyBuilderTests {

        @Test
        @DisplayName("空构建器 build 返回空字符串")
        void shouldReturnEmptyString_whenEmptyBuilder() {
            assertThat(builder.build()).isEmpty();
        }

        @Test
        @DisplayName("空构建器 hasCtes 返回 false")
        void shouldReturnFalse_whenNoCtes() {
            assertThat(builder.hasCtes()).isFalse();
        }

        @Test
        @DisplayName("空构建器 getParameters 返回空列表")
        void shouldReturnEmptyList_whenNoCtes() {
            assertThat(builder.getParameters()).isEmpty();
        }
    }

    @Nested
    @DisplayName("WITH 子句")
    class WithClauseTests {

        @Test
        @DisplayName("WITH 生成非递归 CTE")
        void shouldGenerateNonRecursiveCte() {
            SqlQueryBuilderImpl cteQuery = simpleCteQuery();
            cteQuery.from("user").select("id", "name");
            builder.with("active_users", cteQuery);

            String sql = builder.build();
            assertThat(sql).startsWith("WITH `active_users` AS (");
            assertThat(sql).endsWith(") ");
            assertThat(sql).doesNotContain("RECURSIVE");
        }

        @Test
        @DisplayName("WITH RECURSIVE 生成递归 CTE")
        void shouldGenerateRecursiveCte() {
            SqlQueryBuilderImpl cteQuery = simpleCteQuery();
            cteQuery.from("org").select("id", "parent_id");
            builder.withRecursive("org_tree", cteQuery);

            String sql = builder.build();
            assertThat(sql).startsWith("WITH RECURSIVE `org_tree` AS (");
        }

        @Test
        @DisplayName("hasCtes 返回 true 当有 CTE 时")
        void shouldReturnTrue_whenHasCtes() {
            builder.with("t", simpleCteQuery().from("user"));
            assertThat(builder.hasCtes()).isTrue();
        }
    }

    @Nested
    @DisplayName("带列名的 CTE")
    class WithColumnNamesTests {

        @Test
        @DisplayName("WITH name(col1, col2) AS (...)")
        void shouldGenerateCteWithColumnNames() {
            SqlQueryBuilderImpl cteQuery = simpleCteQuery();
            cteQuery.from("user").select("id", "name");
            builder.withColumnNames("user_summary", new String[]{"uid", "uname"}, cteQuery);

            String sql = builder.build();
            assertThat(sql).contains("`user_summary`(`uid`, `uname`) AS (");
        }

        @Test
        @DisplayName("WITH RECURSIVE name(col1, col2) AS (...)")
        void shouldGenerateRecursiveCteWithColumnNames() {
            SqlQueryBuilderImpl cteQuery = simpleCteQuery();
            cteQuery.from("org").select("id", "parent_id");
            builder.withRecursiveColumnNames("org_tree", new String[]{"node_id", "parent_node"}, cteQuery);

            String sql = builder.build();
            assertThat(sql).startsWith("WITH RECURSIVE");
            assertThat(sql).contains("`org_tree`(`node_id`, `parent_node`) AS (");
        }
    }

    @Nested
    @DisplayName("多 CTE 组合")
    class MultipleCteTests {

        @Test
        @DisplayName("多个 CTE 用逗号分隔")
        void shouldCombineMultipleCtesWithComma() {
            SqlQueryBuilderImpl cte1 = simpleCteQuery();
            cte1.from("user").select("id", "name");
            SqlQueryBuilderImpl cte2 = simpleCteQuery();
            cte2.from("order").select("id", "amount");

            builder.with("users", cte1).with("orders", cte2);

            String sql = builder.build();
            assertThat(sql).startsWith("WITH ");
            assertThat(sql).contains("`users` AS (");
            assertThat(sql).contains(", `orders` AS (");
        }

        @Test
        @DisplayName("混合递归和非递归 CTE 使用 WITH RECURSIVE")
        void shouldUseRecursiveWhenAnyCteIsRecursive() {
            SqlQueryBuilderImpl cte1 = simpleCteQuery();
            cte1.from("user").select("id");
            SqlQueryBuilderImpl cte2 = simpleCteQuery();
            cte2.from("org").select("id");

            builder.with("users", cte1).withRecursive("org_tree", cte2);

            String sql = builder.build();
            assertThat(sql).startsWith("WITH RECURSIVE ");
        }
    }

    @Nested
    @DisplayName("参数收集")
    class ParameterCollectionTests {

        @Test
        @DisplayName("getParameters 收集 CTE 查询的参数")
        void shouldCollectParametersFromCteQuery() {
            SqlQueryBuilderImpl cteQuery = simpleCteQuery();
            cteQuery.from("user").where(io.github.afgprojects.framework.data.core.condition.Conditions.eq("status", 1));
            builder.with("active_users", cteQuery);

            assertThat(builder.getParameters()).containsExactly(1);
        }

        @Test
        @DisplayName("多个 CTE 参数合并")
        void shouldMergeParametersFromMultipleCtes() {
            SqlQueryBuilderImpl cte1 = simpleCteQuery();
            cte1.from("user").where(io.github.afgprojects.framework.data.core.condition.Conditions.eq("status", 1));
            SqlQueryBuilderImpl cte2 = simpleCteQuery();
            cte2.from("order").where(io.github.afgprojects.framework.data.core.condition.Conditions.eq("amount", 100));

            builder.with("users", cte1).with("orders", cte2);

            assertThat(builder.getParameters()).containsExactly(1, 100);
        }
    }

    @Nested
    @DisplayName("非法 CTE 名称拒绝")
    class InvalidCteNameTests {

        @Test
        @DisplayName("with 拒绝非法 CTE 名称")
        void shouldRejectInvalidCteName() {
            assertThatThrownBy(() -> builder.with("bad name", simpleCteQuery()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("withRecursive 拒绝非法 CTE 名称")
        void shouldRejectInvalidCteNameInRecursive() {
            assertThatThrownBy(() -> builder.withRecursive("1invalid", simpleCteQuery()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("withColumnNames 拒绝非法 CTE 名称")
        void shouldRejectInvalidCteNameWithColumnNames() {
            assertThatThrownBy(() -> builder.withColumnNames("drop;table", new String[]{"a"}, simpleCteQuery()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("withColumnNames 拒绝非法列名")
        void shouldRejectInvalidColumnNameInCte() {
            assertThatThrownBy(() -> builder.withColumnNames("valid_name", new String[]{"bad col"}, simpleCteQuery()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("withRecursiveColumnNames 拒绝非法 CTE 名称")
        void shouldRejectInvalidCteNameInRecursiveWithColumnNames() {
            assertThatThrownBy(() -> builder.withRecursiveColumnNames("bad-name", new String[]{"a"}, simpleCteQuery()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
