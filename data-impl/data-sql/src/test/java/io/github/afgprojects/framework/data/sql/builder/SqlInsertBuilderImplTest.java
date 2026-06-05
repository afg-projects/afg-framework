package io.github.afgprojects.framework.data.sql.builder;

import io.github.afgprojects.framework.data.core.sql.SqlInsertBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SqlInsertBuilderImpl 测试
 */
@DisplayName("SqlInsertBuilderImpl 测试")
class SqlInsertBuilderImplTest {

    @Nested
    @DisplayName("基本 INSERT")
    class BasicInsertTests {

        @Test
        @DisplayName("INSERT 指定列")
        void shouldGenerateInsertWithColumns() {
            String sql = new SqlInsertBuilderImpl()
                    .into("user")
                    .columns("name", "age")
                    .values("Tom", 25)
                    .toSql();
            assertThat(sql).isEqualTo("INSERT INTO `user` (`name`, `age`) VALUES (?, ?)");
        }

        @Test
        @DisplayName("INSERT 无指定列")
        void shouldGenerateInsertWithoutColumns() {
            String sql = new SqlInsertBuilderImpl()
                    .into("user")
                    .values("Tom", 25)
                    .toSql();
            assertThat(sql).isEqualTo("INSERT INTO `user` VALUES (?, ?)");
        }
    }

    @Nested
    @DisplayName("多行 VALUES")
    class MultiRowTests {

        @Test
        @DisplayName("多行 VALUES")
        void shouldGenerateMultiRowValues() {
            String sql = new SqlInsertBuilderImpl()
                    .into("user")
                    .columns("name")
                    .values("Tom")
                    .values("Jerry")
                    .toSql();
            assertThat(sql).isEqualTo("INSERT INTO `user` (`name`) VALUES (?), (?)");
        }

        @Test
        @DisplayName("row 方法等同于 values")
        void shouldGenerateRowValues() {
            String sql = new SqlInsertBuilderImpl()
                    .into("user")
                    .columns("name", "age")
                    .row("Tom", 25)
                    .row("Jerry", 30)
                    .toSql();
            assertThat(sql).isEqualTo("INSERT INTO `user` (`name`, `age`) VALUES (?, ?), (?, ?)");
        }
    }

    @Nested
    @DisplayName("getParameters")
    class GetParametersTests {

        @Test
        @DisplayName("参数按行顺序展平")
        void shouldFlattenParametersByRow() {
            List<Object> params = new SqlInsertBuilderImpl()
                    .into("user")
                    .columns("name", "age")
                    .values("Tom", 25)
                    .values("Jerry", 30)
                    .getParameters();
            assertThat(params).containsExactly("Tom", 25, "Jerry", 30);
        }

        @Test
        @DisplayName("单行参数")
        void shouldReturnSingleRowParameters() {
            List<Object> params = new SqlInsertBuilderImpl()
                    .into("user")
                    .columns("name")
                    .values("Tom")
                    .getParameters();
            assertThat(params).containsExactly("Tom");
        }
    }

    @Nested
    @DisplayName("execute 方法")
    class ExecuteMethodTests {

        @Test
        @DisplayName("execute() 抛 UnsupportedOperationException")
        void shouldThrowUnsupportedOperationExceptionForExecute() {
            SqlInsertBuilder builder = new SqlInsertBuilderImpl()
                    .into("user")
                    .columns("name")
                    .values("Tom");
            assertThatThrownBy(builder::execute)
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("Use DataManager to execute insert");
        }

        @Test
        @DisplayName("executeAndReturnKey() 抛 UnsupportedOperationException")
        void shouldThrowUnsupportedOperationExceptionForExecuteAndReturnKey() {
            SqlInsertBuilder builder = new SqlInsertBuilderImpl()
                    .into("user")
                    .columns("name")
                    .values("Tom");
            assertThatThrownBy(builder::executeAndReturnKey)
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("Use DataManager to execute insert");
        }
    }

    @Nested
    @DisplayName("标识符验证")
    class IdentifierValidationTests {

        @Test
        @DisplayName("非法表名应抛异常")
        void shouldThrowExceptionForInvalidTableName() {
            SqlInsertBuilderImpl builder = new SqlInsertBuilderImpl();
            assertThatThrownBy(() -> builder.into("user; DROP TABLE user;--"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("table name");
        }

        @Test
        @DisplayName("非法列名应抛异常")
        void shouldThrowExceptionForInvalidColumnName() {
            SqlInsertBuilderImpl builder = new SqlInsertBuilderImpl();
            assertThatThrownBy(() -> builder.columns("name; DROP TABLE user;--"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("column name");
        }
    }
}
