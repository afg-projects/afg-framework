package io.github.afgprojects.framework.data.sql.builder;

import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.data.core.dialect.H2Dialect;
import io.github.afgprojects.framework.data.core.dialect.PostgreSQLDialect;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SqlUpdateBuilderImpl 测试
 */
@DisplayName("SqlUpdateBuilderImpl Tests")
class SqlUpdateBuilderImplTest {

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("默认构造函数使用 MySQL 方言")
        void testDefaultConstructor() {
            SqlUpdateBuilderImpl builder = new SqlUpdateBuilderImpl();
            String sql = builder.table("users").set("name", "test").toSql();
            assertThat(sql).contains("`users`");
            assertThat(sql).contains("`name`");
        }

        @Test
        @DisplayName("使用自定义方言构造")
        void testCustomDialectConstructor() {
            SqlUpdateBuilderImpl builder = new SqlUpdateBuilderImpl(new PostgreSQLDialect());
            String sql = builder.table("users").set("name", "test").toSql();
            assertThat(sql).contains("\"users\"");
            assertThat(sql).contains("\"name\"");
        }

        @Test
        @DisplayName("使用 H2 方言构造")
        void testH2DialectConstructor() {
            SqlUpdateBuilderImpl builder = new SqlUpdateBuilderImpl(new H2Dialect());
            String sql = builder.table("users").set("name", "test").toSql();
            // H2 方言不添加引号
            assertThat(sql).contains("UPDATE users SET name = ?");
        }
    }

    @Nested
    @DisplayName("SET Clause Tests")
    class SetClauseTests {

        @Test
        @DisplayName("单个 SET 子句")
        void testSingleSet() {
            SqlUpdateBuilderImpl builder = new SqlUpdateBuilderImpl();
            String sql = builder.table("users")
                    .set("name", "test")
                    .toSql();

            assertThat(sql).isEqualTo("UPDATE `users` SET `name` = ?");
        }

        @Test
        @DisplayName("多个 SET 子句")
        void testMultipleSet() {
            SqlUpdateBuilderImpl builder = new SqlUpdateBuilderImpl();
            String sql = builder.table("users")
                    .set("name", "test")
                    .set("email", "test@example.com")
                    .set("age", 25)
                    .toSql();

            assertThat(sql).contains("UPDATE `users` SET");
            assertThat(sql).contains("`name` = ?");
            assertThat(sql).contains("`email` = ?");
            assertThat(sql).contains("`age` = ?");
        }

        @Test
        @DisplayName("使用 Map 批量 SET")
        void testSetWithMap() {
            SqlUpdateBuilderImpl builder = new SqlUpdateBuilderImpl();
            String sql = builder.table("users")
                    .set(Map.of("name", "test", "email", "test@example.com"))
                    .toSql();

            assertThat(sql).contains("`name` = ?");
            assertThat(sql).contains("`email` = ?");
        }

        @Test
        @DisplayName("使用 SFunction 设置列值")
        void testSetWithSFunction() {
            SqlUpdateBuilderImpl builder = new SqlUpdateBuilderImpl();
            String sql = builder.table("users")
                    .set(Conditions.getFieldName(TestEntity::getName), "test")
                    .toSql();

            assertThat(sql).contains("`name` = ?");
        }
    }

    @Nested
    @DisplayName("WHERE Clause Tests")
    class WhereClauseTests {

        @Test
        @DisplayName("带 WHERE 条件")
        void testWithWhereCondition() {
            SqlUpdateBuilderImpl builder = new SqlUpdateBuilderImpl();
            String sql = builder.table("users")
                    .set("name", "test")
                    .where(Conditions.eq("id", 1))
                    .toSql();

            assertThat(sql).contains("WHERE");
            assertThat(sql).contains("id = ?");
        }

        @Test
        @DisplayName("无 WHERE 条件")
        void testWithoutWhereCondition() {
            SqlUpdateBuilderImpl builder = new SqlUpdateBuilderImpl();
            String sql = builder.table("users")
                    .set("name", "test")
                    .toSql();

            assertThat(sql).doesNotContain("WHERE");
        }

        @Test
        @DisplayName("复杂 WHERE 条件")
        void testComplexWhereCondition() {
            SqlUpdateBuilderImpl builder = new SqlUpdateBuilderImpl();
            String sql = builder.table("users")
                    .set("status", 1)
                    .where(Conditions.builder()
                            .eq("id", 1)
                            .eq("status", 0)
                            .build())
                    .toSql();

            assertThat(sql).contains("WHERE");
            assertThat(sql).contains("AND");
        }
    }

    @Nested
    @DisplayName("Parameters Tests")
    class ParametersTests {

        @Test
        @DisplayName("获取参数列表")
        void testGetParameters() {
            SqlUpdateBuilderImpl builder = new SqlUpdateBuilderImpl();
            builder.table("users")
                    .set("name", "test")
                    .set("age", 25)
                    .where(Conditions.eq("id", 1));

            var params = builder.getParameters();

            assertThat(params).hasSize(3);
            assertThat(params).containsExactly("test", 25, 1);
        }

        @Test
        @DisplayName("无 WHERE 条件时获取参数")
        void testGetParametersWithoutWhere() {
            SqlUpdateBuilderImpl builder = new SqlUpdateBuilderImpl();
            builder.table("users")
                    .set("name", "test");

            var params = builder.getParameters();

            assertThat(params).hasSize(1);
            assertThat(params).containsExactly("test");
        }
    }

    @Nested
    @DisplayName("Execute Tests")
    class ExecuteTests {

        @Test
        @DisplayName("execute() 抛出 UnsupportedOperationException")
        void testExecuteThrowsException() {
            SqlUpdateBuilderImpl builder = new SqlUpdateBuilderImpl();
            builder.table("users").set("name", "test");

            assertThatThrownBy(builder::execute)
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("Use DataManager to execute update");
        }
    }

    // 测试实体类
    static class TestEntity {
        private Long id;
        private String name;

        public Long getId() { return id; }
        public String getName() { return name; }
    }
}
