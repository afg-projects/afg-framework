package io.github.afgprojects.framework.data.core.sql;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SqlBuilderInterfaceTest {

    @Test
    void shouldDefineSqlQueryBuilder() {
        assertThat(SqlQueryBuilder.class).isInterface();
    }

    @Test
    void shouldDefineSqlInsertBuilder() {
        assertThat(SqlInsertBuilder.class).isInterface();
    }

    @Test
    void shouldDefineSqlUpdateBuilder() {
        assertThat(SqlUpdateBuilder.class).isInterface();
    }

    @Test
    void shouldDefineSqlDeleteBuilder() {
        assertThat(SqlDeleteBuilder.class).isInterface();
    }

    @Test
    void shouldDefineSqlParser() {
        assertThat(SqlParser.class).isInterface();
    }

    @Test
    void shouldDefineSqlStatement() {
        assertThat(SqlStatement.class).isInterface();
        assertThat(SqlStatement.SqlType.values()).contains(
            SqlStatement.SqlType.SELECT,
            SqlStatement.SqlType.INSERT,
            SqlStatement.SqlType.UPDATE,
            SqlStatement.SqlType.DELETE
        );
    }

    @Test
    void shouldDefineSqlRewriteContext() {
        assertThat(SqlRewriteContext.class).isInterface();
    }
}