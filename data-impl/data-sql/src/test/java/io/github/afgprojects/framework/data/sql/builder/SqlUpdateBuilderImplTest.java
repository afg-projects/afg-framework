package io.github.afgprojects.framework.data.sql.builder;

import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.data.core.query.Condition;
import io.github.afgprojects.framework.data.core.sql.SqlUpdateBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SqlUpdateBuilderImpl 测试
 */
@DisplayName("SqlUpdateBuilderImpl 测试")
class SqlUpdateBuilderImplTest {

    @Nested
    @DisplayName("基本 UPDATE")
    class BasicUpdateTests {

        @Test
        @DisplayName("UPDATE 单列 + WHERE")
        void shouldGenerateUpdateWithWhere() {
            Condition where = Conditions.eq("id", 1);
            String sql = new SqlUpdateBuilderImpl()
                    .table("user")
                    .set("name", "Tom")
                    .where(where)
                    .toSql();
            assertThat(sql).isEqualTo("UPDATE `user` SET `name` = ? WHERE id = ?");
        }

        @Test
        @DisplayName("UPDATE 多列 SET")
        void shouldGenerateUpdateMultipleColumns() {
            Condition where = Conditions.eq("id", 1);
            String sql = new SqlUpdateBuilderImpl()
                    .table("user")
                    .set("name", "Tom")
                    .set("age", 25)
                    .where(where)
                    .toSql();
            assertThat(sql).isEqualTo("UPDATE `user` SET `name` = ?, `age` = ? WHERE id = ?");
        }

        @Test
        @DisplayName("UPDATE 无 WHERE")
        void shouldGenerateUpdateWithoutWhere() {
            String sql = new SqlUpdateBuilderImpl()
                    .table("user")
                    .set("name", "Tom")
                    .toSql();
            assertThat(sql).isEqualTo("UPDATE `user` SET `name` = ?");
        }
    }

    @Nested
    @DisplayName("getParameters")
    class GetParametersTests {

        @Test
        @DisplayName("SET 参数 + WHERE 参数")
        void shouldReturnSetAndWhereParameters() {
            Condition where = Conditions.eq("id", 1);
            List<Object> params = new SqlUpdateBuilderImpl()
                    .table("user")
                    .set("name", "Tom")
                    .set("age", 25)
                    .where(where)
                    .getParameters();
            assertThat(params).containsExactly("Tom", 25, 1);
        }

        @Test
        @DisplayName("只有 SET 参数")
        void shouldReturnOnlySetParameters() {
            List<Object> params = new SqlUpdateBuilderImpl()
                    .table("user")
                    .set("name", "Tom")
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
            SqlUpdateBuilder builder = new SqlUpdateBuilderImpl()
                    .table("user")
                    .set("name", "Tom");
            assertThatThrownBy(builder::execute)
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("Use DataManager to execute update");
        }
    }
}
