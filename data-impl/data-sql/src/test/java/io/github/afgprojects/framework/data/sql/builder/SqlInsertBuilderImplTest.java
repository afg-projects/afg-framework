package io.github.afgprojects.framework.data.sql.builder;

import io.github.afgprojects.framework.data.core.dialect.H2Dialect;
import io.github.afgprojects.framework.data.core.dialect.PostgreSQLDialect;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SqlInsertBuilderImpl 测试
 */
@DisplayName("SqlInsertBuilderImpl Tests")
class SqlInsertBuilderImplTest {

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("默认构造函数使用 MySQL 方言")
        void testDefaultConstructor() {
            SqlInsertBuilderImpl builder = new SqlInsertBuilderImpl();
            String sql = builder.into("users").columns("name", "email").values("test", "test@example.com").toSql();
            assertThat(sql).contains("`users`");
            assertThat(sql).contains("`name`");
            assertThat(sql).contains("`email`");
        }

        @Test
        @DisplayName("使用自定义方言构造")
        void testCustomDialectConstructor() {
            SqlInsertBuilderImpl builder = new SqlInsertBuilderImpl(new PostgreSQLDialect());
            String sql = builder.into("users").columns("name").values("test").toSql();
            assertThat(sql).contains("\"users\"");
            assertThat(sql).contains("\"name\"");
        }

        @Test
        @DisplayName("使用 H2 方言构造")
        void testH2DialectConstructor() {
            SqlInsertBuilderImpl builder = new SqlInsertBuilderImpl(new H2Dialect());
            String sql = builder.into("users").columns("name").values("test").toSql();
            // H2 方言不添加引号
            assertThat(sql).contains("INSERT INTO users");
        }
    }

    @Nested
    @DisplayName("INSERT Statement Tests")
    class InsertStatementTests {

        @Test
        @DisplayName("基本 INSERT 语句")
        void testBasicInsert() {
            SqlInsertBuilderImpl builder = new SqlInsertBuilderImpl();
            String sql = builder.into("users")
                    .columns("name", "email")
                    .values("test", "test@example.com")
                    .toSql();

            assertThat(sql).isEqualTo("INSERT INTO `users` (`name`, `email`) VALUES (?, ?)");
        }

        @Test
        @DisplayName("无列名 INSERT 语句")
        void testInsertWithoutColumns() {
            SqlInsertBuilderImpl builder = new SqlInsertBuilderImpl();
            String sql = builder.into("users")
                    .values("test", "test@example.com")
                    .toSql();

            assertThat(sql).isEqualTo("INSERT INTO `users` VALUES (?, ?)");
        }

        @Test
        @DisplayName("多行 INSERT 语句")
        void testMultiRowInsert() {
            SqlInsertBuilderImpl builder = new SqlInsertBuilderImpl();
            String sql = builder.into("users")
                    .columns("name", "email")
                    .values("test1", "test1@example.com")
                    .values("test2", "test2@example.com")
                    .toSql();

            assertThat(sql).contains("INSERT INTO `users` (`name`, `email`) VALUES");
            assertThat(sql).contains("(?, ?), (?, ?)");
        }

        @Test
        @DisplayName("使用 row() 方法添加多行")
        void testInsertWithRowMethod() {
            SqlInsertBuilderImpl builder = new SqlInsertBuilderImpl();
            String sql = builder.into("users")
                    .columns("name", "email")
                    .row("test1", "test1@example.com")
                    .row("test2", "test2@example.com")
                    .row("test3", "test3@example.com")
                    .toSql();

            assertThat(sql).contains("(?, ?), (?, ?), (?, ?)");
        }
    }

    @Nested
    @DisplayName("Parameters Tests")
    class ParametersTests {

        @Test
        @DisplayName("获取单行参数")
        void testGetSingleRowParameters() {
            SqlInsertBuilderImpl builder = new SqlInsertBuilderImpl();
            builder.into("users")
                    .columns("name", "email")
                    .values("test", "test@example.com");

            var params = builder.getParameters();

            assertThat(params).hasSize(2);
            assertThat(params).containsExactly("test", "test@example.com");
        }

        @Test
        @DisplayName("获取多行参数")
        void testGetMultiRowParameters() {
            SqlInsertBuilderImpl builder = new SqlInsertBuilderImpl();
            builder.into("users")
                    .columns("name", "email")
                    .values("test1", "test1@example.com")
                    .values("test2", "test2@example.com");

            var params = builder.getParameters();

            assertThat(params).hasSize(4);
            assertThat(params).containsExactly("test1", "test1@example.com", "test2", "test2@example.com");
        }

        @Test
        @DisplayName("空参数列表")
        void testEmptyParameters() {
            SqlInsertBuilderImpl builder = new SqlInsertBuilderImpl();
            builder.into("users");

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
            SqlInsertBuilderImpl builder = new SqlInsertBuilderImpl();
            builder.into("users").columns("name").values("test");

            assertThatThrownBy(builder::execute)
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("Use DataManager to execute insert");
        }

        @Test
        @DisplayName("executeAndReturnKey() 抛出 UnsupportedOperationException")
        void testExecuteAndReturnKeyThrowsException() {
            SqlInsertBuilderImpl builder = new SqlInsertBuilderImpl();
            builder.into("users").columns("name").values("test");

            assertThatThrownBy(builder::executeAndReturnKey)
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("Use DataManager to execute insert");
        }
    }
}
