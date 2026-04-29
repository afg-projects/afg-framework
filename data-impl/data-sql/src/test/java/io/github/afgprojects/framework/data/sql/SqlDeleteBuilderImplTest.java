package io.github.afgprojects.framework.data.sql;

import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.data.core.query.Condition;
import io.github.afgprojects.framework.data.sql.builder.SqlDeleteBuilderImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SQL DELETE 构建器测试
 */
class SqlDeleteBuilderImplTest {

    @Test
    @DisplayName("构建带 WHERE 条件的 DELETE")
    void testBuildDeleteWithWhere() {
        Condition condition = Conditions.eq("id", 1);
        String sql = new SqlDeleteBuilderImpl()
            .from("users")
            .where(condition)
            .toSql();

        assertThat(sql).isEqualTo("DELETE FROM `users` WHERE id = ?");
    }

    @Test
    @DisplayName("构建无条件 DELETE")
    void testBuildDeleteWithoutWhere() {
        String sql = new SqlDeleteBuilderImpl()
            .from("users")
            .toSql();

        assertThat(sql).isEqualTo("DELETE FROM `users`");
    }

    @Test
    @DisplayName("获取 DELETE 参数")
    void testGetDeleteParameters() {
        Condition condition = Conditions.eq("id", 1);
        java.util.List<Object> params = new SqlDeleteBuilderImpl()
            .from("users")
            .where(condition)
            .getParameters();

        assertThat(params).containsExactly(1);
    }

    @Test
    @DisplayName("构建复杂条件 DELETE")
    void testBuildComplexDelete() {
        Condition condition = Conditions.builder()
            .eq("status", 0)
            .like("name", "test")
            .build();

        String sql = new SqlDeleteBuilderImpl()
            .from("users")
            .where(condition)
            .toSql();

        assertThat(sql).contains("WHERE");
        assertThat(sql).contains("AND");
    }

    @Test
    @DisplayName("构建 IN 条件 DELETE")
    void testBuildDeleteWithInCondition() {
        Condition condition = Conditions.in("id", java.util.List.of(1, 2, 3));
        String sql = new SqlDeleteBuilderImpl()
            .from("users")
            .where(condition)
            .toSql();

        assertThat(sql).isEqualTo("DELETE FROM `users` WHERE id IN (?, ?, ?)");
    }
}