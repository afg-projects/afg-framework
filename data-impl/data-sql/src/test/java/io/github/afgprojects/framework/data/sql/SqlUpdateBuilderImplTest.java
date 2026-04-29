package io.github.afgprojects.framework.data.sql;

import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.data.core.query.Condition;
import io.github.afgprojects.framework.data.sql.builder.SqlUpdateBuilderImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SQL UPDATE 构建器测试
 */
class SqlUpdateBuilderImplTest {

    @Test
    @DisplayName("构建简单 UPDATE")
    void testBuildSimpleUpdate() {
        String sql = new SqlUpdateBuilderImpl()
            .table("users")
            .set("name", "newName")
            .set("status", 1)
            .toSql();

        assertThat(sql).isEqualTo("UPDATE `users` SET `name` = ?, `status` = ?");
    }

    @Test
    @DisplayName("构建带 WHERE 条件的 UPDATE")
    void testBuildUpdateWithWhere() {
        Condition condition = Conditions.eq("id", 1);
        String sql = new SqlUpdateBuilderImpl()
            .table("users")
            .set("name", "newName")
            .where(condition)
            .toSql();

        assertThat(sql).isEqualTo("UPDATE `users` SET `name` = ? WHERE id = ?");
    }

    @Test
    @DisplayName("使用 Map 设置更新值")
    void testBuildUpdateWithMap() {
        String sql = new SqlUpdateBuilderImpl()
            .table("users")
            .set(java.util.Map.of("name", "test", "status", 1))
            .toSql();

        assertThat(sql).contains("UPDATE `users` SET");
        assertThat(sql).contains("`name` = ?");
        assertThat(sql).contains("`status` = ?");
    }

    @Test
    @DisplayName("获取 UPDATE 参数")
    void testGetUpdateParameters() {
        Condition condition = Conditions.eq("id", 1);
        java.util.List<Object> params = new SqlUpdateBuilderImpl()
            .table("users")
            .set("name", "newName")
            .set("status", 2)
            .where(condition)
            .getParameters();

        assertThat(params).containsExactly("newName", 2, 1);
    }

    @Test
    @DisplayName("构建复杂条件 UPDATE")
    void testBuildComplexUpdate() {
        Condition condition = Conditions.builder()
            .eq("status", 0)
            .like("name", "test")
            .build();

        String sql = new SqlUpdateBuilderImpl()
            .table("users")
            .set("status", 1)
            .where(condition)
            .toSql();

        assertThat(sql).contains("WHERE");
        assertThat(sql).contains("AND");
    }
}