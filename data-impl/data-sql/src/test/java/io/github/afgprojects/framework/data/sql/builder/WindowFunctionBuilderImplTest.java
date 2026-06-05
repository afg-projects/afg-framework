package io.github.afgprojects.framework.data.sql.builder;

import io.github.afgprojects.framework.data.core.dialect.MySQLDialect;
import io.github.afgprojects.framework.data.core.query.Sort;
import io.github.afgprojects.framework.data.core.sql.SqlQueryBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WindowFunctionBuilderImpl 单元测试
 */
@DisplayName("WindowFunctionBuilderImpl 测试")
class WindowFunctionBuilderImplTest {

    private final MySQLDialect dialect = new MySQLDialect();

    @Nested
    @DisplayName("PARTITION BY + ORDER BY")
    class PartitionByAndOrderByTests {

        @Test
        @DisplayName("PARTITION BY + ORDER BY 生成完整窗口函数")
        void shouldGenerateFullWindowFunction() {
            SqlQueryBuilderImpl queryBuilder = new SqlQueryBuilderImpl(dialect);
            WindowFunctionBuilderImpl wfBuilder = new WindowFunctionBuilderImpl(queryBuilder, "ROW_NUMBER()");

            SqlQueryBuilder result = wfBuilder
                    .partitionBy("category")
                    .orderBy("score", Sort.Direction.DESC)
                    .end();

            assertThat(result).isSameAs(queryBuilder);
            String sql = result.from("product").toSql();
            assertThat(sql).contains("ROW_NUMBER() OVER (PARTITION BY `category` ORDER BY `score` DESC)");
        }

        @Test
        @DisplayName("多列 PARTITION BY")
        void shouldGenerateMultiColumnPartitionBy() {
            SqlQueryBuilderImpl queryBuilder = new SqlQueryBuilderImpl(dialect);
            WindowFunctionBuilderImpl wfBuilder = new WindowFunctionBuilderImpl(queryBuilder, "RANK()");

            wfBuilder.partitionBy("dept", "team").orderBy("salary", Sort.Direction.DESC).end();

            String sql = queryBuilder.from("employee").toSql();
            assertThat(sql).contains("RANK() OVER (PARTITION BY `dept`, `team` ORDER BY `salary` DESC)");
        }
    }

    @Nested
    @DisplayName("仅 PARTITION BY")
    class PartitionByOnlyTests {

        @Test
        @DisplayName("仅 PARTITION BY 无 ORDER BY")
        void shouldGeneratePartitionByOnly() {
            SqlQueryBuilderImpl queryBuilder = new SqlQueryBuilderImpl(dialect);
            WindowFunctionBuilderImpl wfBuilder = new WindowFunctionBuilderImpl(queryBuilder, "ROW_NUMBER()");

            wfBuilder.partitionBy("region").end();

            String sql = queryBuilder.from("store").toSql();
            assertThat(sql).contains("ROW_NUMBER() OVER (PARTITION BY `region`)");
        }
    }

    @Nested
    @DisplayName("仅 ORDER BY")
    class OrderByOnlyTests {

        @Test
        @DisplayName("仅 ORDER BY 无 PARTITION BY")
        void shouldGenerateOrderByOnly() {
            SqlQueryBuilderImpl queryBuilder = new SqlQueryBuilderImpl(dialect);
            WindowFunctionBuilderImpl wfBuilder = new WindowFunctionBuilderImpl(queryBuilder, "DENSE_RANK()");

            wfBuilder.orderBy("score", Sort.Direction.ASC).end();

            String sql = queryBuilder.from("student").toSql();
            assertThat(sql).contains("DENSE_RANK() OVER (ORDER BY `score` ASC)");
        }

        @Test
        @DisplayName("使用 Sort 对象的 ORDER BY")
        void shouldGenerateOrderByWithSortObject() {
            SqlQueryBuilderImpl queryBuilder = new SqlQueryBuilderImpl(dialect);
            WindowFunctionBuilderImpl wfBuilder = new WindowFunctionBuilderImpl(queryBuilder, "ROW_NUMBER()");

            Sort sort = Sort.by(Sort.Order.desc("created_at"));
            wfBuilder.orderBy(sort).end();

            String sql = queryBuilder.from("event").toSql();
            assertThat(sql).contains("ROW_NUMBER() OVER (ORDER BY `created_at` DESC)");
        }
    }

    @Nested
    @DisplayName("裸窗口函数")
    class BareWindowFunctionTests {

        @Test
        @DisplayName("无 PARTITION BY 无 ORDER BY 生成空 OVER()")
        void shouldGenerateEmptyOverClause() {
            SqlQueryBuilderImpl queryBuilder = new SqlQueryBuilderImpl(dialect);
            WindowFunctionBuilderImpl wfBuilder = new WindowFunctionBuilderImpl(queryBuilder, "ROW_NUMBER()");

            wfBuilder.end();

            String sql = queryBuilder.from("item").toSql();
            assertThat(sql).contains("ROW_NUMBER() OVER ()");
        }
    }

    @Nested
    @DisplayName("end() 返回父构建器")
    class EndReturnTests {

        @Test
        @DisplayName("end() 返回原始 SqlQueryBuilder")
        void shouldReturnParentQueryBuilder() {
            SqlQueryBuilderImpl queryBuilder = new SqlQueryBuilderImpl(dialect);
            WindowFunctionBuilderImpl wfBuilder = new WindowFunctionBuilderImpl(queryBuilder, "ROW_NUMBER()");

            SqlQueryBuilder result = wfBuilder.end();

            assertThat(result).isSameAs(queryBuilder);
        }

        @Test
        @DisplayName("end() 后可以继续链式调用")
        void shouldAllowChainingAfterEnd() {
            SqlQueryBuilderImpl queryBuilder = new SqlQueryBuilderImpl(dialect);
            WindowFunctionBuilderImpl wfBuilder = new WindowFunctionBuilderImpl(queryBuilder, "ROW_NUMBER()");

            String sql = wfBuilder.partitionBy("type").orderBy("id", Sort.Direction.ASC).end()
                    .from("product")
                    .toSql();

            assertThat(sql).contains("ROW_NUMBER() OVER (PARTITION BY `type` ORDER BY `id` ASC)");
            assertThat(sql).contains("FROM `product`");
        }
    }

    @Nested
    @DisplayName("不同窗口函数类型")
    class DifferentWindowFunctionTypesTests {

        @Test
        @DisplayName("RANK() 窗口函数")
        void shouldGenerateRankFunction() {
            SqlQueryBuilderImpl queryBuilder = new SqlQueryBuilderImpl(dialect);
            WindowFunctionBuilderImpl wfBuilder = new WindowFunctionBuilderImpl(queryBuilder, "RANK()");

            wfBuilder.orderBy("score", Sort.Direction.DESC).end();

            String sql = queryBuilder.from("student").toSql();
            assertThat(sql).contains("RANK() OVER (ORDER BY `score` DESC)");
        }

        @Test
        @DisplayName("DENSE_RANK() 窗口函数")
        void shouldGenerateDenseRankFunction() {
            SqlQueryBuilderImpl queryBuilder = new SqlQueryBuilderImpl(dialect);
            WindowFunctionBuilderImpl wfBuilder = new WindowFunctionBuilderImpl(queryBuilder, "DENSE_RANK()");

            wfBuilder.orderBy("score", Sort.Direction.DESC).end();

            String sql = queryBuilder.from("student").toSql();
            assertThat(sql).contains("DENSE_RANK() OVER (ORDER BY `score` DESC)");
        }

        @Test
        @DisplayName("LEAD(score) 窗口函数")
        void shouldGenerateLeadFunction() {
            SqlQueryBuilderImpl queryBuilder = new SqlQueryBuilderImpl(dialect);
            WindowFunctionBuilderImpl wfBuilder = new WindowFunctionBuilderImpl(queryBuilder, "LEAD(`score`)");

            wfBuilder.orderBy("id", Sort.Direction.ASC).end();

            String sql = queryBuilder.from("student").toSql();
            assertThat(sql).contains("LEAD(`score`) OVER (ORDER BY `id` ASC)");
        }
    }
}
