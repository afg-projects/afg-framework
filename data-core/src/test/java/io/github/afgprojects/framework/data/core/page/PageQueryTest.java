package io.github.afgprojects.framework.data.core.page;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PageQuery 测试")
class PageQueryTest {

    @Nested
    @DisplayName("构造器参数校验测试")
    class ConstructorTests {

        @Test
        @DisplayName("正常参数创建")
        void shouldCreateWithValidParameters() {
            PageQuery query = new PageQuery(2, 20, "name", true);

            assertThat(query.page()).isEqualTo(2);
            assertThat(query.size()).isEqualTo(20);
            assertThat(query.orderBy()).isEqualTo("name");
            assertThat(query.asc()).isTrue();
        }

        @ParameterizedTest
        @ValueSource(longs = {0, -1, -100, Long.MIN_VALUE})
        @DisplayName("页码小于1时自动修正为1")
        void shouldClampPageToMinimum(long invalidPage) {
            PageQuery query = new PageQuery(invalidPage, 10, null, true);

            assertThat(query.page()).isEqualTo(1);
        }

        @Test
        @DisplayName("页码为1时保持不变")
        void shouldKeepPageOne() {
            PageQuery query = new PageQuery(1, 10, null, true);

            assertThat(query.page()).isEqualTo(1);
        }

        @ParameterizedTest
        @ValueSource(longs = {0, -1, -100, Long.MIN_VALUE})
        @DisplayName("每页大小小于1时使用默认值")
        void shouldUseDefaultSizeWhenInvalid(long invalidSize) {
            PageQuery query = new PageQuery(1, invalidSize, null, true);

            assertThat(query.size()).isEqualTo(PageQuery.DEFAULT_SIZE);
        }

        @ParameterizedTest
        @ValueSource(longs = {1001, 2000, Long.MAX_VALUE})
        @DisplayName("每页大小超过最大值时使用最大值")
        void shouldUseMaxSizeWhenExceeded(long oversized) {
            PageQuery query = new PageQuery(1, oversized, null, true);

            assertThat(query.size()).isEqualTo(PageQuery.MAX_SIZE);
        }

        @Test
        @DisplayName("每页大小为边界值1000时保持不变")
        void shouldKeepMaxSize() {
            PageQuery query = new PageQuery(1, 1000, null, true);

            assertThat(query.size()).isEqualTo(1000);
        }

        @Test
        @DisplayName("orderBy可以为null")
        void shouldAllowNullOrderBy() {
            PageQuery query = new PageQuery(1, 10, null, true);

            assertThat(query.orderBy()).isNull();
        }

        @Test
        @DisplayName("orderBy可以为空字符串")
        void shouldAllowEmptyOrderBy() {
            PageQuery query = new PageQuery(1, 10, "", true);

            assertThat(query.orderBy()).isEmpty();
        }
    }

    @Nested
    @DisplayName("静态工厂方法测试")
    class FactoryMethodTests {

        @Test
        @DisplayName("of(page, size) 创建默认排序的分页参数")
        void shouldCreateWithDefaultSort() {
            PageQuery query = PageQuery.of(2, 15);

            assertThat(query.page()).isEqualTo(2);
            assertThat(query.size()).isEqualTo(15);
            assertThat(query.orderBy()).isNull();
            assertThat(query.asc()).isTrue();
        }

        @Test
        @DisplayName("of(page, size, orderBy, asc) 创建指定排序的分页参数")
        void shouldCreateWithSpecifiedSort() {
            PageQuery query = PageQuery.of(3, 25, "create_time", false);

            assertThat(query.page()).isEqualTo(3);
            assertThat(query.size()).isEqualTo(25);
            assertThat(query.orderBy()).isEqualTo("create_time");
            assertThat(query.asc()).isFalse();
        }

        @Test
        @DisplayName("defaultPage() 创建默认分页参数")
        void shouldCreateDefaultPage() {
            PageQuery query = PageQuery.defaultPage();

            assertThat(query.page()).isEqualTo(1);
            assertThat(query.size()).isEqualTo(PageQuery.DEFAULT_SIZE);
            assertThat(query.orderBy()).isNull();
            assertThat(query.asc()).isTrue();
        }

        @Test
        @DisplayName("常量值验证")
        void shouldVerifyConstants() {
            assertThat(PageQuery.DEFAULT_SIZE).isEqualTo(10);
            assertThat(PageQuery.MAX_SIZE).isEqualTo(1000);
        }
    }

    @Nested
    @DisplayName("偏移量计算测试")
    class OffsetTests {

        @ParameterizedTest
        @CsvSource({
            "1, 10, 0",    // 第1页，偏移量0
            "2, 10, 10",   // 第2页，偏移量10
            "3, 20, 40",   // 第3页，每页20条，偏移量40
            "10, 5, 45",   // 第10页，每页5条，偏移量45
            "100, 100, 9900" // 第100页，每页100条
        })
        @DisplayName("偏移量计算正确")
        void shouldCalculateOffsetCorrectly(long page, long size, long expectedOffset) {
            PageQuery query = new PageQuery(page, size, null, true);

            assertThat(query.offset()).isEqualTo(expectedOffset);
        }

        @Test
        @DisplayName("大页码和大页数的偏移量计算（避免溢出）")
        void shouldHandleLargeOffsetWithoutOverflow() {
            // 使用long类型确保不溢出
            PageQuery query = new PageQuery(10000, 100, null, true);

            assertThat(query.offset()).isEqualTo(999900L);
        }

        @Test
        @DisplayName("修正后页码的偏移量计算")
        void shouldCalculateOffsetAfterPageClamping() {
            // 页码被修正为1
            PageQuery query = new PageQuery(-5, 20, null, true);

            assertThat(query.page()).isEqualTo(1);
            assertThat(query.offset()).isEqualTo(0);
        }

        @Test
        @DisplayName("修正后大小的偏移量计算")
        void shouldCalculateOffsetAfterSizeClamping() {
            // 大小被修正为默认值10
            PageQuery query = new PageQuery(3, -1, null, true);

            assertThat(query.size()).isEqualTo(PageQuery.DEFAULT_SIZE);
            assertThat(query.offset()).isEqualTo(20); // (3-1) * 10 = 20
        }
    }

    @Nested
    @DisplayName("排序相关方法测试")
    class OrderTests {

        @Test
        @DisplayName("hasOrder() - 有排序字段时返回true")
        void shouldReturnTrueWhenHasOrder() {
            PageQuery query = new PageQuery(1, 10, "name", true);

            assertThat(query.hasOrder()).isTrue();
        }

        @Test
        @DisplayName("hasOrder() - orderBy为null时返回false")
        void shouldReturnFalseWhenOrderByIsNull() {
            PageQuery query = new PageQuery(1, 10, null, true);

            assertThat(query.hasOrder()).isFalse();
        }

        @Test
        @DisplayName("hasOrder() - orderBy为空字符串时返回false")
        void shouldReturnFalseWhenOrderByIsEmpty() {
            PageQuery query = new PageQuery(1, 10, "", true);

            assertThat(query.hasOrder()).isFalse();
        }

        @Test
        @DisplayName("hasOrder() - orderBy为空白字符串时返回true（按业务逻辑，空白不是空字符串）")
        void shouldReturnTrueWhenOrderByIsWhitespace() {
            PageQuery query = new PageQuery(1, 10, "  ", true);

            // 空白字符串不是null也不是empty，所以返回true
            assertThat(query.hasOrder()).isTrue();
        }

        @Test
        @DisplayName("orderDirection() - 升序时返回ASC")
        void shouldReturnAscWhenAscIsTrue() {
            PageQuery query = new PageQuery(1, 10, "name", true);

            assertThat(query.orderDirection()).isEqualTo("ASC");
        }

        @Test
        @DisplayName("orderDirection() - 降序时返回DESC")
        void shouldReturnDescWhenAscIsFalse() {
            PageQuery query = new PageQuery(1, 10, "name", false);

            assertThat(query.orderDirection()).isEqualTo("DESC");
        }

        @Test
        @DisplayName("即使没有排序字段，orderDirection()也能正确返回")
        void shouldReturnDirectionEvenWithoutOrderBy() {
            PageQuery queryNoOrder = new PageQuery(1, 10, null, true);
            PageQuery queryAscFalse = new PageQuery(1, 10, null, false);

            assertThat(queryNoOrder.orderDirection()).isEqualTo("ASC");
            assertThat(queryAscFalse.orderDirection()).isEqualTo("DESC");
        }
    }

    @Nested
    @DisplayName("边界和特殊场景测试")
    class EdgeCaseTests {

        @Test
        @DisplayName("极小页码修正边界")
        void shouldClampMinPage() {
            PageQuery query = new PageQuery(Long.MIN_VALUE, 10, null, true);

            assertThat(query.page()).isEqualTo(1);
        }

        @Test
        @DisplayName("极大页码保持不变")
        void shouldKeepMaxPage() {
            PageQuery query = new PageQuery(Long.MAX_VALUE, 10, null, true);

            assertThat(query.page()).isEqualTo(Long.MAX_VALUE);
        }

        @Test
        @DisplayName("极大每页大小修正")
        void shouldClampMaxSize() {
            PageQuery query = new PageQuery(1, Long.MAX_VALUE, null, true);

            assertThat(query.size()).isEqualTo(PageQuery.MAX_SIZE);
        }

        @Test
        @DisplayName("完整参数组合测试")
        void shouldHandleFullParameterCombination() {
            // 各种参数组合
            PageQuery q1 = PageQuery.of(1, 10);  // 默认排序
            PageQuery q2 = PageQuery.of(5, 50, "id", true);  // 升序
            PageQuery q3 = PageQuery.of(10, 100, "time", false);  // 降序
            PageQuery q4 = PageQuery.defaultPage();  // 默认页

            assertThat(q1.hasOrder()).isFalse();
            assertThat(q2.hasOrder()).isTrue();
            assertThat(q2.orderDirection()).isEqualTo("ASC");
            assertThat(q3.hasOrder()).isTrue();
            assertThat(q3.orderDirection()).isEqualTo("DESC");
            assertThat(q4.page()).isEqualTo(1);
            assertThat(q4.size()).isEqualTo(10);
        }

        @Test
        @DisplayName("链式边界修正测试")
        void shouldHandleMultipleClamping() {
            // 页码和大小都需要修正
            PageQuery query = new PageQuery(-100, -50, "name", true);

            assertThat(query.page()).isEqualTo(1);
            assertThat(query.size()).isEqualTo(PageQuery.DEFAULT_SIZE);
            assertThat(query.offset()).isEqualTo(0);
        }

        @Test
        @DisplayName("参数恰好等于边界值")
        void shouldHandleExactBoundaryValues() {
            PageQuery queryMinPage = new PageQuery(1, 10, null, true);
            PageQuery queryMinSize = new PageQuery(1, 1, null, true);
            PageQuery queryMaxSize = new PageQuery(1, 1000, null, true);

            assertThat(queryMinPage.page()).isEqualTo(1);
            assertThat(queryMinSize.size()).isEqualTo(1);
            assertThat(queryMaxSize.size()).isEqualTo(1000);
        }
    }

    @Nested
    @DisplayName("Record特性测试")
    class RecordTests {

        @Test
        @DisplayName("equals和hashCode测试")
        void shouldHaveCorrectEqualsAndHashCode() {
            PageQuery q1 = new PageQuery(2, 20, "name", true);
            PageQuery q2 = new PageQuery(2, 20, "name", true);
            PageQuery q3 = new PageQuery(2, 20, "name", false);

            assertThat(q1).isEqualTo(q2);
            assertThat(q1.hashCode()).isEqualTo(q2.hashCode());
            assertThat(q1).isNotEqualTo(q3);
        }

        @Test
        @DisplayName("toString包含关键字段")
        void shouldHaveToStringWithFields() {
            PageQuery query = new PageQuery(3, 25, "create_time", false);
            String str = query.toString();

            assertThat(str).contains("3");
            assertThat(str).contains("25");
            assertThat(str).contains("create_time");
        }
    }
}
