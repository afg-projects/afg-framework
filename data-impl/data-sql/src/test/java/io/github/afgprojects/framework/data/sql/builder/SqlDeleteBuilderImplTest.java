package io.github.afgprojects.framework.data.sql.builder;

import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.data.core.query.Condition;
import io.github.afgprojects.framework.data.core.sql.SqlDeleteBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SqlDeleteBuilderImpl 测试
 */
@DisplayName("SqlDeleteBuilderImpl 测试")
class SqlDeleteBuilderImplTest {

    @Nested
    @DisplayName("基本 DELETE")
    class BasicDeleteTests {

        @Test
        @DisplayName("DELETE FROM table")
        void shouldGenerateDeleteFromTable() {
            String sql = new SqlDeleteBuilderImpl()
                    .from("user")
                    .toSql();
            assertThat(sql).isEqualTo("DELETE FROM `user`");
        }

        @Test
        @DisplayName("DELETE WHERE")
        void shouldGenerateDeleteWithWhere() {
            Condition where = Conditions.eq("id", 1);
            String sql = new SqlDeleteBuilderImpl()
                    .from("user")
                    .where(where)
                    .toSql();
            assertThat(sql).isEqualTo("DELETE FROM `user` WHERE id = ?");
        }
    }

    @Nested
    @DisplayName("getParameters")
    class GetParametersTests {

        @Test
        @DisplayName("WHERE 参数")
        void shouldReturnWhereParameters() {
            Condition where = Conditions.eq("id", 1);
            List<Object> params = new SqlDeleteBuilderImpl()
                    .from("user")
                    .where(where)
                    .getParameters();
            assertThat(params).containsExactly(1);
        }

        @Test
        @DisplayName("无 WHERE 时参数为空")
        void shouldReturnEmptyParametersWithoutWhere() {
            List<Object> params = new SqlDeleteBuilderImpl()
                    .from("user")
                    .getParameters();
            assertThat(params).isEmpty();
        }
    }

    @Nested
    @DisplayName("execute 方法")
    class ExecuteMethodTests {

        @Test
        @DisplayName("execute() 抛 UnsupportedOperationException")
        void shouldThrowUnsupportedOperationExceptionForExecute() {
            SqlDeleteBuilder builder = new SqlDeleteBuilderImpl()
                    .from("user");
            assertThatThrownBy(builder::execute)
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("Use DataManager to execute delete");
        }
    }
}
