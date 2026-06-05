package io.github.afgprojects.framework.data.sql.builder;

import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.data.core.dialect.MySQLDialect;
import io.github.afgprojects.framework.data.core.query.Condition;
import io.github.afgprojects.framework.data.sql.converter.ConditionToSqlConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JoinClauseBuilder 单元测试
 */
@DisplayName("JoinClauseBuilder 测试")
class JoinClauseBuilderTest {

    private final MySQLDialect dialect = new MySQLDialect();
    private final ConditionToSqlConverter converter = new ConditionToSqlConverter(dialect);
    private JoinClauseBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new JoinClauseBuilder(dialect, converter);
    }

    @Nested
    @DisplayName("空构建器")
    class EmptyBuilderTests {

        @Test
        @DisplayName("空构建器 build 返回空字符串")
        void shouldReturnEmptyString_whenEmptyBuilder() {
            assertThat(builder.build()).isEmpty();
        }

        @Test
        @DisplayName("空构建器 hasJoins 返回 false")
        void shouldReturnFalse_whenNoJoins() {
            assertThat(builder.hasJoins()).isFalse();
        }
    }

    @Nested
    @DisplayName("JOIN 类型")
    class JoinTypeTests {

        @Test
        @DisplayName("JOIN 生成 JOIN 子句")
        void shouldGenerateJoinClause() {
            Condition on = Conditions.eq("user.id", "order.user_id");
            builder.join("order", on);
            String sql = builder.build();
            assertThat(sql).startsWith(" JOIN `order` ON");
            assertThat(sql).contains("user.id = ?");
        }

        @Test
        @DisplayName("INNER JOIN 生成 INNER JOIN 子句")
        void shouldGenerateInnerJoinClause() {
            Condition on = Conditions.eq("user.id", "dept.user_id");
            builder.innerJoin("dept", on);
            String sql = builder.build();
            assertThat(sql).startsWith(" INNER JOIN `dept` ON");
        }

        @Test
        @DisplayName("LEFT JOIN 生成 LEFT JOIN 子句")
        void shouldGenerateLeftJoinClause() {
            Condition on = Conditions.eq("user.id", "profile.user_id");
            builder.leftJoin("profile", on);
            String sql = builder.build();
            assertThat(sql).startsWith(" LEFT JOIN `profile` ON");
        }

        @Test
        @DisplayName("RIGHT JOIN 生成 RIGHT JOIN 子句")
        void shouldGenerateRightJoinClause() {
            Condition on = Conditions.eq("user.id", "role.user_id");
            builder.rightJoin("role", on);
            String sql = builder.build();
            assertThat(sql).startsWith(" RIGHT JOIN `role` ON");
        }
    }

    @Nested
    @DisplayName("带别名 JOIN")
    class JoinWithAliasTests {

        @Test
        @DisplayName("JOIN 带别名")
        void shouldGenerateJoinWithAlias() {
            Condition on = Conditions.eq("u.id", "o.user_id");
            builder.join("order", "o", on);
            String sql = builder.build();
            assertThat(sql).contains("JOIN `order` `o` ON");
        }

        @Test
        @DisplayName("LEFT JOIN 带别名")
        void shouldGenerateLeftJoinWithAlias() {
            Condition on = Conditions.eq("u.id", "p.user_id");
            builder.leftJoin("profile", "p", on);
            String sql = builder.build();
            assertThat(sql).contains("LEFT JOIN `profile` `p` ON");
        }
    }

    @Nested
    @DisplayName("多 JOIN 组合")
    class MultipleJoinTests {

        @Test
        @DisplayName("多个 JOIN 组合")
        void shouldCombineMultipleJoins() {
            Condition on1 = Conditions.eq("u.id", "o.user_id");
            Condition on2 = Conditions.eq("o.id", "i.order_id");
            builder.join("order", "o", on1).leftJoin("item", "i", on2);
            String sql = builder.build();
            assertThat(sql).contains("JOIN `order` `o` ON");
            assertThat(sql).contains("LEFT JOIN `item` `i` ON");
        }

        @Test
        @DisplayName("hasJoins 返回 true 当有 JOIN 时")
        void shouldReturnTrue_whenHasJoins() {
            Condition on = Conditions.eq("u.id", "o.user_id");
            builder.join("order", on);
            assertThat(builder.hasJoins()).isTrue();
        }
    }

    @Nested
    @DisplayName("非法表名拒绝")
    class InvalidTableNameTests {

        @Test
        @DisplayName("join 拒绝非法表名")
        void shouldRejectInvalidTableName() {
            Condition on = Conditions.eq("a.id", "b.a_id");
            assertThatThrownBy(() -> builder.join("bad table", on))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("leftJoin 拒绝非法表名")
        void shouldRejectInvalidTableNameInLeftJoin() {
            Condition on = Conditions.eq("a.id", "b.a_id");
            assertThatThrownBy(() -> builder.leftJoin("1invalid", on))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rightJoin 拒绝非法表名")
        void shouldRejectInvalidTableNameInRightJoin() {
            Condition on = Conditions.eq("a.id", "b.a_id");
            assertThatThrownBy(() -> builder.rightJoin("drop;table", on))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("innerJoin 拒绝非法表名")
        void shouldRejectInvalidTableNameInInnerJoin() {
            Condition on = Conditions.eq("a.id", "b.a_id");
            assertThatThrownBy(() -> builder.innerJoin("bad name", on))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("带别名的 join 拒绝非法别名")
        void shouldRejectInvalidAlias() {
            Condition on = Conditions.eq("a.id", "b.a_id");
            assertThatThrownBy(() -> builder.join("order", "bad alias", on))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("带别名的 leftJoin 拒绝非法别名")
        void shouldRejectInvalidAliasInLeftJoin() {
            Condition on = Conditions.eq("a.id", "b.a_id");
            assertThatThrownBy(() -> builder.leftJoin("order", "1bad", on))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
