package io.github.afgprojects.framework.data.sql;

import io.github.afgprojects.framework.data.sql.builder.SqlInsertBuilderImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SQL INSERT 构建器测试
 */
class SqlInsertBuilderImplTest {

    @Test
    @DisplayName("构建单行 INSERT")
    void testBuildSingleRowInsert() {
        String sql = new SqlInsertBuilderImpl()
            .into("users")
            .columns("id", "name", "status")
            .values(1, "test", 1)
            .toSql();

        assertThat(sql).isEqualTo("INSERT INTO `users` (`id`, `name`, `status`) VALUES (?, ?, ?)");
    }

    @Test
    @DisplayName("构建多行 INSERT")
    void testBuildMultiRowInsert() {
        String sql = new SqlInsertBuilderImpl()
            .into("users")
            .columns("id", "name")
            .values(1, "user1")
            .row(2, "user2")
            .row(3, "user3")
            .toSql();

        assertThat(sql).isEqualTo("INSERT INTO `users` (`id`, `name`) VALUES (?, ?), (?, ?), (?, ?)");
    }

    @Test
    @DisplayName("获取 INSERT 参数")
    void testGetInsertParameters() {
        java.util.List<Object> params = new SqlInsertBuilderImpl()
            .into("users")
            .columns("id", "name")
            .values(1, "test")
            .getParameters();

        assertThat(params).containsExactly(1, "test");
    }

    @Test
    @DisplayName("获取多行 INSERT 参数")
    void testGetMultiRowInsertParameters() {
        java.util.List<Object> params = new SqlInsertBuilderImpl()
            .into("users")
            .columns("id", "name")
            .values(1, "user1")
            .row(2, "user2")
            .getParameters();

        assertThat(params).containsExactly(1, "user1", 2, "user2");
    }

    @Test
    @DisplayName("构建无列名的 INSERT")
    void testBuildInsertWithoutColumns() {
        String sql = new SqlInsertBuilderImpl()
            .into("users")
            .values(1, "test", 1)
            .toSql();

        assertThat(sql).isEqualTo("INSERT INTO `users` VALUES (?, ?, ?)");
    }
}