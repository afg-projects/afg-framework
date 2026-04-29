package io.github.afgprojects.framework.data.sql;

import io.github.afgprojects.framework.data.core.dialect.DatabaseType;
import io.github.afgprojects.framework.data.core.sql.SqlParser;
import io.github.afgprojects.framework.data.core.sql.SqlStatement;
import io.github.afgprojects.framework.data.core.scope.DataScope;
import io.github.afgprojects.framework.data.core.sql.SqlRewriteContext;
import io.github.afgprojects.framework.data.sql.parser.JSqlParserImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JSqlParser SQL 解析器测试
 */
class JSqlParserImplTest {

    @Test
    @DisplayName("解析 SELECT 语句")
    void testParseSelect() {
        SqlParser parser = new JSqlParserImpl(DatabaseType.MYSQL);
        SqlStatement statement = parser.parse("SELECT id, name FROM users WHERE status = 1");

        assertThat(statement.getType()).isEqualTo(SqlStatement.SqlType.SELECT);
        assertThat(statement.getTables()).contains("users");
        assertThat(statement.toSql()).contains("SELECT");
    }

    @Test
    @DisplayName("解析 INSERT 语句")
    void testParseInsert() {
        SqlParser parser = new JSqlParserImpl(DatabaseType.MYSQL);
        SqlStatement statement = parser.parse("INSERT INTO users (id, name) VALUES (1, 'test')");

        assertThat(statement.getType()).isEqualTo(SqlStatement.SqlType.INSERT);
        assertThat(statement.getTables()).contains("users");
    }

    @Test
    @DisplayName("解析 UPDATE 语句")
    void testParseUpdate() {
        SqlParser parser = new JSqlParserImpl(DatabaseType.MYSQL);
        SqlStatement statement = parser.parse("UPDATE users SET name = 'new' WHERE id = 1");

        assertThat(statement.getType()).isEqualTo(SqlStatement.SqlType.UPDATE);
        assertThat(statement.getTables()).contains("users");
    }

    @Test
    @DisplayName("解析 DELETE 语句")
    void testParseDelete() {
        SqlParser parser = new JSqlParserImpl(DatabaseType.MYSQL);
        SqlStatement statement = parser.parse("DELETE FROM users WHERE id = 1");

        assertThat(statement.getType()).isEqualTo(SqlStatement.SqlType.DELETE);
        assertThat(statement.getTables()).contains("users");
    }

    @Test
    @DisplayName("验证有效 SQL")
    void testValidateValidSql() {
        SqlParser parser = new JSqlParserImpl();
        assertThat(parser.validate("SELECT * FROM users")).isTrue();
    }

    @Test
    @DisplayName("验证无效 SQL")
    void testValidateInvalidSql() {
        SqlParser parser = new JSqlParserImpl();
        assertThat(parser.validate("INVALID SQL STATEMENT")).isFalse();
    }

    @Test
    @DisplayName("解析复杂 JOIN 查询")
    void testParseJoinQuery() {
        SqlParser parser = new JSqlParserImpl();
        SqlStatement statement = parser.parse(
            "SELECT u.id, u.name, o.order_no FROM users u JOIN orders o ON u.id = o.user_id WHERE u.status = 1"
        );

        assertThat(statement.getType()).isEqualTo(SqlStatement.SqlType.SELECT);
        assertThat(statement.getTables()).contains("users", "orders");
    }

    @Nested
    @DisplayName("Exception Handling Tests")
    class ExceptionHandlingTests {

        @Test
        @DisplayName("parse() 对无效 SQL 抛出 IllegalArgumentException")
        void testParseInvalidSqlThrowsException() {
            SqlParser parser = new JSqlParserImpl();
            assertThatThrownBy(() -> parser.parse("INVALID SQL STATEMENT"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("SQL 解析失败");
        }

        @Test
        @DisplayName("rewrite() 对无效 SQL 抛出 IllegalArgumentException")
        void testRewriteInvalidSqlThrowsException() {
            SqlParser parser = new JSqlParserImpl();
            SqlRewriteContext context =
                    new SqlRewriteContext() {
                        @Override
                        public @org.jspecify.annotations.NonNull DatabaseType getDatabaseType() {
                            return DatabaseType.MYSQL;
                        }

                        @Override
                        public java.util.@org.jspecify.annotations.NonNull List<DataScope> getDataScopes() {
                            return java.util.Collections.emptyList();
                        }

                        @Override
                        public @org.jspecify.annotations.Nullable String getTenantId() {
                            return null;
                        }

                        @Override
                        public boolean isSoftDeleteFilter() {
                            return false;
                        }

                        @Override
                        public @org.jspecify.annotations.Nullable String getSoftDeleteColumn() {
                            return null;
                        }

                        @Override
                        public @org.jspecify.annotations.Nullable String getDeletedValue() {
                            return null;
                        }

                        @Override
                        public boolean isIgnoreDataScope() {
                            return true;
                        }

                        @Override
                        public boolean isIgnoreTenant() {
                            return true;
                        }

                        @Override
                        public @org.jspecify.annotations.Nullable String getTableAlias(@org.jspecify.annotations.NonNull String tableName) {
                            return null;
                        }
                    };

            assertThatThrownBy(() -> parser.rewrite("INVALID SQL STATEMENT", context))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("SQL 解析失败");
        }

        @Test
        @DisplayName("rewrite() 成功改写 SQL")
        void testRewriteSuccessful() {
            SqlParser parser = new JSqlParserImpl();
            SqlRewriteContext context =
                    new SqlRewriteContext() {
                        @Override
                        public @org.jspecify.annotations.NonNull DatabaseType getDatabaseType() {
                            return DatabaseType.MYSQL;
                        }

                        @Override
                        public java.util.@org.jspecify.annotations.NonNull List<DataScope> getDataScopes() {
                            return java.util.Collections.emptyList();
                        }

                        @Override
                        public @org.jspecify.annotations.Nullable String getTenantId() {
                            return null;
                        }

                        @Override
                        public boolean isSoftDeleteFilter() {
                            return false;
                        }

                        @Override
                        public @org.jspecify.annotations.Nullable String getSoftDeleteColumn() {
                            return null;
                        }

                        @Override
                        public @org.jspecify.annotations.Nullable String getDeletedValue() {
                            return null;
                        }

                        @Override
                        public boolean isIgnoreDataScope() {
                            return true;
                        }

                        @Override
                        public boolean isIgnoreTenant() {
                            return true;
                        }

                        @Override
                        public @org.jspecify.annotations.Nullable String getTableAlias(@org.jspecify.annotations.NonNull String tableName) {
                            return null;
                        }
                    };

            String result = parser.rewrite("SELECT * FROM users WHERE id = 1", context);
            assertThat(result).contains("SELECT");
            assertThat(result).contains("users");
        }
    }
}