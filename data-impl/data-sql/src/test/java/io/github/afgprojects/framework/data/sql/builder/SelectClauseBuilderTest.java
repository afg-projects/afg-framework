package io.github.afgprojects.framework.data.sql.builder;

import io.github.afgprojects.framework.data.core.dialect.MySQLDialect;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SelectClauseBuilder 单元测试
 */
@DisplayName("SelectClauseBuilder 测试")
class SelectClauseBuilderTest {

    private final SelectClauseBuilder builder = new SelectClauseBuilder(new MySQLDialect());

    @Nested
    @DisplayName("空构建器默认行为")
    class EmptyBuilderTests {

        @Test
        @DisplayName("空构建器 build 返回 *")
        void shouldReturnAsterisk_whenEmptyBuilder() {
            assertThat(builder.build()).isEqualTo("*");
        }

        @Test
        @DisplayName("空构建器 isEmpty 返回 true")
        void shouldReturnTrue_whenIsEmpty() {
            assertThat(builder.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("空构建器 isDistinct 返回 false")
        void shouldReturnFalse_whenIsDistinctOnEmptyBuilder() {
            assertThat(builder.isDistinct()).isFalse();
        }
    }

    @Nested
    @DisplayName("列选择")
    class ColumnSelectionTests {

        @Test
        @DisplayName("select 指定单列")
        void shouldSelectSingleColumn() {
            builder.select("name");
            assertThat(builder.build()).isEqualTo("`name`");
        }

        @Test
        @DisplayName("select 指定多列")
        void shouldSelectMultipleColumns() {
            builder.select("id", "name", "age");
            assertThat(builder.build()).isEqualTo("`id`, `name`, `age`");
        }

        @Test
        @DisplayName("select * 直接通过不引用")
        void shouldPassAsteriskDirectlyWithoutQuoting() {
            builder.select("*");
            assertThat(builder.build()).isEqualTo("*");
        }

        @Test
        @DisplayName("selectAll 设置为 *")
        void shouldSetSelectAll() {
            builder.select("id").selectAll();
            assertThat(builder.build()).isEqualTo("*");
        }

        @Test
        @DisplayName("select 带 table.column 格式整体引用")
        void shouldSelectWithTableDotColumn() {
            builder.select("user.name");
            // quoteIdentifier 将整个标识符包裹，不拆分 table.column
            assertThat(builder.build()).isEqualTo("`user.name`");
        }

        @Test
        @DisplayName("select 覆盖之前的列")
        void shouldOverridePreviousColumns() {
            builder.select("id");
            builder.select("name");
            assertThat(builder.build()).isEqualTo("`name`");
        }
    }

    @Nested
    @DisplayName("DISTINCT")
    class DistinctTests {

        @Test
        @DisplayName("distinct 添加 DISTINCT 前缀")
        void shouldAddDistinctPrefix() {
            builder.select("name").distinct();
            assertThat(builder.build()).isEqualTo("DISTINCT `name`");
        }

        @Test
        @DisplayName("distinct + 空列返回 DISTINCT *")
        void shouldReturnDistinctAsterisk_whenNoColumns() {
            builder.distinct();
            assertThat(builder.build()).isEqualTo("DISTINCT *");
        }

        @Test
        @DisplayName("isDistinct 返回 true")
        void shouldReturnTrue_whenDistinctSet() {
            builder.distinct();
            assertThat(builder.isDistinct()).isTrue();
        }
    }

    @Nested
    @DisplayName("聚合函数")
    class AggregateFunctionTests {

        @Test
        @DisplayName("count(column) 生成 COUNT(`column`)")
        void shouldGenerateCountColumn() {
            builder.count("id");
            assertThat(builder.build()).isEqualTo("COUNT(`id`)");
        }

        @Test
        @DisplayName("count() 生成 COUNT(*)")
        void shouldGenerateCountAll() {
            builder.count();
            assertThat(builder.build()).isEqualTo("COUNT(*)");
        }

        @Test
        @DisplayName("countDistinct 生成 COUNT(DISTINCT `column`)")
        void shouldGenerateCountDistinct() {
            builder.countDistinct("status");
            assertThat(builder.build()).isEqualTo("COUNT(DISTINCT `status`)");
        }

        @Test
        @DisplayName("sum 生成 SUM(`column`)")
        void shouldGenerateSum() {
            builder.sum("amount");
            assertThat(builder.build()).isEqualTo("SUM(`amount`)");
        }

        @Test
        @DisplayName("avg 生成 AVG(`column`)")
        void shouldGenerateAvg() {
            builder.avg("score");
            assertThat(builder.build()).isEqualTo("AVG(`score`)");
        }

        @Test
        @DisplayName("max 生成 MAX(`column`)")
        void shouldGenerateMax() {
            builder.max("price");
            assertThat(builder.build()).isEqualTo("MAX(`price`)");
        }

        @Test
        @DisplayName("min 生成 MIN(`column`)")
        void shouldGenerateMin() {
            builder.min("price");
            assertThat(builder.build()).isEqualTo("MIN(`price`)");
        }

        @Test
        @DisplayName("聚合函数与列选择组合")
        void shouldCombineColumnsWithAggregates() {
            builder.select("category").sum("amount").count();
            assertThat(builder.build()).isEqualTo("`category`, SUM(`amount`), COUNT(*)");
        }
    }

    @Nested
    @DisplayName("非法列名拒绝")
    class InvalidColumnNameTests {

        @Test
        @DisplayName("select 拒绝含空格的列名")
        void shouldRejectColumnNameWithSpace() {
            assertThatThrownBy(() -> builder.select("invalid name"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("select 拒绝以数字开头的列名")
        void shouldRejectColumnNameStartingWithDigit() {
            assertThatThrownBy(() -> builder.select("1col"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("count 拒绝非法列名")
        void shouldRejectInvalidColumnInCount() {
            assertThatThrownBy(() -> builder.count("drop table"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("sum 拒绝非法列名")
        void shouldRejectInvalidColumnInSum() {
            assertThatThrownBy(() -> builder.sum("; DELETE"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("avg 拒绝非法列名")
        void shouldRejectInvalidColumnInAvg() {
            assertThatThrownBy(() -> builder.avg("col; DROP"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("max 拒绝非法列名")
        void shouldRejectInvalidColumnInMax() {
            assertThatThrownBy(() -> builder.max("1invalid"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("min 拒绝非法列名")
        void shouldRejectInvalidColumnInMin() {
            assertThatThrownBy(() -> builder.min("bad-name"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("countDistinct 拒绝非法列名")
        void shouldRejectInvalidColumnInCountDistinct() {
            assertThatThrownBy(() -> builder.countDistinct("bad col"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("窗口函数表达式")
    class WindowFunctionExpressionTests {

        @Test
        @DisplayName("addWindowFunctionExpression 添加原始表达式")
        void shouldAddRawWindowFunctionExpression() {
            builder.select("id").addWindowFunctionExpression("ROW_NUMBER() OVER (ORDER BY `score` DESC)");
            assertThat(builder.build()).isEqualTo("`id`, ROW_NUMBER() OVER (ORDER BY `score` DESC)");
        }
    }

    @Nested
    @DisplayName("getSelectColumns 和 setSelectColumns")
    class AccessorTests {

        @Test
        @DisplayName("getSelectColumns 返回已选列")
        void shouldReturnSelectedColumns() {
            builder.select("id", "name");
            assertThat(builder.getSelectColumns()).containsExactly("`id`", "`name`");
        }

        @Test
        @DisplayName("setSelectColumns 覆盖列列表")
        void shouldOverrideColumnsViaSetter() {
            builder.select("id");
            builder.setSelectColumns(java.util.List.of("custom_col"));
            assertThat(builder.build()).isEqualTo("custom_col");
        }
    }
}
