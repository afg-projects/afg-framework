package io.github.afgprojects.framework.data.sql.builder;

import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.data.core.dialect.H2Dialect;
import io.github.afgprojects.framework.data.core.dialect.PostgreSQLDialect;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SqlDeleteBuilderImpl 测试
 */
@DisplayName("SqlDeleteBuilderImpl Tests")
class SqlDeleteBuilderImplTest {

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("默认构造函数使用 MySQL 方言")
        void testDefaultConstructor() {
            SqlDeleteBuilderImpl builder = new SqlDeleteBuilderImpl();
            String sql = builder.from("users").toSql();
            assertThat(sql).contains("`users`");
        }

        @Test
        @DisplayName("使用自定义方言构造")
        void testCustomDialectConstructor() {
            SqlDeleteBuilderImpl builder = new SqlDeleteBuilderImpl(new PostgreSQLDialect());
            String sql = builder.from("users").toSql();
            assertThat(sql).contains("\"users\"");
        }

        @Test
        @DisplayName("使用 H2 方言构造")
        void testH2DialectConstructor() {
            SqlDeleteBuilderImpl builder = new SqlDeleteBuilderImpl(new H2Dialect());
            String sql = builder.from("users").toSql();
            // H2 方言不添加引号
            assertThat(sql).isEqualTo("DELETE FROM users");
        }
    }

    @Nested
    @DisplayName("DELETE Statement Tests")
    class DeleteStatementTests {

        @Test
        @DisplayName("基本 DELETE 语句")
        void testBasicDelete() {
            SqlDeleteBuilderImpl builder = new SqlDeleteBuilderImpl();
            String sql = builder.from("users").toSql();

            assertThat(sql).isEqualTo("DELETE FROM `users`");
        }

        @Test
        @DisplayName("带 WHERE 条件的 DELETE 语句")
        void testDeleteWithWhere() {
            SqlDeleteBuilderImpl builder = new SqlDeleteBuilderImpl();
            String sql = builder.from("users")
                    .where(Conditions.eq("id", 1))
                    .toSql();

            assertThat(sql).isEqualTo("DELETE FROM `users` WHERE id = ?");
        }

        @Test
        @DisplayName("复杂 WHERE 条件")
        void testDeleteWithComplexWhere() {
            SqlDeleteBuilderImpl builder = new SqlDeleteBuilderImpl();
            String sql = builder.from("users")
                    .where(Conditions.builder()
                            .eq("status", 0)
                            .like("name", "test")
                            .build())
                    .toSql();

            assertThat(sql).contains("DELETE FROM `users` WHERE");
            assertThat(sql).contains("AND");
        }
    }

    @Nested
    @DisplayName("Parameters Tests")
    class ParametersTests {

        @Test
        @DisplayName("获取参数列表")
        void testGetParameters() {
            SqlDeleteBuilderImpl builder = new SqlDeleteBuilderImpl();
            builder.from("users")
                    .where(Conditions.builder()
                            .eq("id", 1)
                            .eq("status", 0)
                            .build());

            var params = builder.getParameters();

            assertThat(params).hasSize(2);
            assertThat(params).containsExactly(1, 0);
        }

        @Test
        @DisplayName("无 WHERE 条件时获取空参数")
        void testGetParametersWithoutWhere() {
            SqlDeleteBuilderImpl builder = new SqlDeleteBuilderImpl();
            builder.from("users");

            var params = builder.getParameters();

            assertThat(params).isEmpty();
        }
    }

    @Nested
    @DisplayName("Execute Tests")
    class ExecuteTests {

        @Test
        @DisplayName("execute() 抛出 UnsupportedOperationException")
        void testExecuteThrowsException() {
            SqlDeleteBuilderImpl builder = new SqlDeleteBuilderImpl();
            builder.from("users");

            assertThatThrownBy(builder::execute)
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("Use DataManager to execute delete");
        }
    }
}
