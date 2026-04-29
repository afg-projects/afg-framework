package io.github.afgprojects.framework.data.core.page;

import io.github.afgprojects.framework.data.core.query.Sort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PageRequest 测试")
class PageRequestTest {

    @Nested
    @DisplayName("构造器参数校验测试")
    class ConstructorTests {

        @Test
        @DisplayName("正常参数创建")
        void shouldCreateWithValidParameters() {
            Sort sort = Sort.asc("name");
            PageRequest request = new PageRequest(2, 20, sort);

            assertThat(request.page()).isEqualTo(2);
            assertThat(request.size()).isEqualTo(20);
            assertThat(request.sort()).isEqualTo(sort);
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1, -100, Integer.MIN_VALUE})
        @DisplayName("页码小于1时自动修正为1")
        void shouldClampPageToMinimum(int invalidPage) {
            PageRequest request = new PageRequest(invalidPage, 10, null);

            assertThat(request.page()).isEqualTo(1);
        }

        @Test
        @DisplayName("页码为1时保持不变")
        void shouldKeepPageOne() {
            PageRequest request = new PageRequest(1, 10, null);

            assertThat(request.page()).isEqualTo(1);
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1, -100, Integer.MIN_VALUE})
        @DisplayName("每页大小小于1时使用默认值")
        void shouldUseDefaultSizeWhenInvalid(int invalidSize) {
            PageRequest request = new PageRequest(1, invalidSize, null);

            assertThat(request.size()).isEqualTo(PageRequest.DEFAULT_PAGE_SIZE);
        }

        @ParameterizedTest
        @ValueSource(ints = {1001, 2000, Integer.MAX_VALUE})
        @DisplayName("每页大小超过最大值时使用最大值")
        void shouldUseMaxSizeWhenExceeded(int oversized) {
            PageRequest request = new PageRequest(1, oversized, null);

            assertThat(request.size()).isEqualTo(PageRequest.MAX_PAGE_SIZE);
        }

        @Test
        @DisplayName("每页大小为边界值1000时保持不变")
        void shouldKeepMaxSize() {
            PageRequest request = new PageRequest(1, 1000, null);

            assertThat(request.size()).isEqualTo(1000);
        }

        @Test
        @DisplayName("每页大小为边界值1时保持不变")
        void shouldKeepMinSize() {
            PageRequest request = new PageRequest(1, 1, null);

            assertThat(request.size()).isEqualTo(1);
        }

        @Test
        @DisplayName("sort可以为null")
        void shouldAllowNullSort() {
            PageRequest request = new PageRequest(1, 10, null);

            assertThat(request.sort()).isNull();
        }
    }

    @Nested
    @DisplayName("静态工厂方法测试")
    class FactoryMethodTests {

        @Test
        @DisplayName("of(page, size) 创建无排序的分页参数")
        void shouldCreateWithoutSort() {
            PageRequest request = PageRequest.of(3, 25);

            assertThat(request.page()).isEqualTo(3);
            assertThat(request.size()).isEqualTo(25);
            assertThat(request.sort()).isNull();
        }

        @Test
        @DisplayName("of(page, size, sort) 创建带排序的分页参数")
        void shouldCreateWithSort() {
            Sort sort = Sort.desc("create_time");
            PageRequest request = PageRequest.of(2, 15, sort);

            assertThat(request.page()).isEqualTo(2);
            assertThat(request.size()).isEqualTo(15);
            assertThat(request.sort()).isEqualTo(sort);
        }

        @Test
        @DisplayName("defaultPage() 创建默认分页参数")
        void shouldCreateDefaultPage() {
            PageRequest request = PageRequest.defaultPage();

            assertThat(request.page()).isEqualTo(1);
            assertThat(request.size()).isEqualTo(PageRequest.DEFAULT_PAGE_SIZE);
            assertThat(request.sort()).isNull();
        }

        @Test
        @DisplayName("常量值验证")
        void shouldVerifyConstants() {
            assertThat(PageRequest.DEFAULT_PAGE_SIZE).isEqualTo(10);
            assertThat(PageRequest.MAX_PAGE_SIZE).isEqualTo(1000);
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
        void shouldCalculateOffsetCorrectly(int page, int size, long expectedOffset) {
            PageRequest request = new PageRequest(page, size, null);

            assertThat(request.offset()).isEqualTo(expectedOffset);
        }

        @Test
        @DisplayName("大页码的偏移量计算（使用long避免溢出）")
        void shouldHandleLargeOffset() {
            // 使用大页码验证 long 类型的 offset 计算
            PageRequest request = new PageRequest(100000, 100, null);

            // offset = (100000 - 1) * 100 = 9999900
            assertThat(request.offset()).isEqualTo(9999900L);
        }

        @Test
        @DisplayName("修正后页码的偏移量计算")
        void shouldCalculateOffsetAfterPageClamping() {
            // 页码被修正为1
            PageRequest request = new PageRequest(-5, 20, null);

            assertThat(request.page()).isEqualTo(1);
            assertThat(request.offset()).isEqualTo(0);
        }

        @Test
        @DisplayName("修正后大小的偏移量计算")
        void shouldCalculateOffsetAfterSizeClamping() {
            // 大小被修正为默认值10
            PageRequest request = new PageRequest(3, -1, null);

            assertThat(request.size()).isEqualTo(PageRequest.DEFAULT_PAGE_SIZE);
            assertThat(request.offset()).isEqualTo(20); // (3-1) * 10 = 20
        }
    }

    @Nested
    @DisplayName("排序相关方法测试")
    class SortTests {

        @Test
        @DisplayName("hasSort() - 有排序时返回true")
        void shouldReturnTrueWhenHasSort() {
            Sort sort = Sort.asc("name");
            PageRequest request = new PageRequest(1, 10, sort);

            assertThat(request.hasSort()).isTrue();
        }

        @Test
        @DisplayName("hasSort() - sort为null时返回false")
        void shouldReturnFalseWhenSortIsNull() {
            PageRequest request = new PageRequest(1, 10, null);

            assertThat(request.hasSort()).isFalse();
        }

        @Test
        @DisplayName("hasSort() - sort为UNSORTED时返回false")
        void shouldReturnFalseWhenSortIsUnsorted() {
            PageRequest request = new PageRequest(1, 10, Sort.unsorted());

            assertThat(request.hasSort()).isFalse();
        }

        @Test
        @DisplayName("多字段排序")
        void shouldSupportMultiFieldSort() {
            Sort sort = Sort.by(
                Sort.Order.desc("priority"),
                Sort.Order.asc("create_time")
            );
            PageRequest request = PageRequest.of(1, 10, sort);

            assertThat(request.hasSort()).isTrue();
            assertThat(request.sort().getOrders()).hasSize(2);
        }

        @Test
        @DisplayName("降序排序")
        void shouldSupportDescendingSort() {
            Sort sort = Sort.desc("id");
            PageRequest request = PageRequest.of(1, 10, sort);

            assertThat(request.hasSort()).isTrue();
            assertThat(request.sort().getOrders().get(0).isDescending()).isTrue();
        }

        @Test
        @DisplayName("升序排序")
        void shouldSupportAscendingSort() {
            Sort sort = Sort.asc("name");
            PageRequest request = PageRequest.of(1, 10, sort);

            assertThat(request.hasSort()).isTrue();
            assertThat(request.sort().getOrders().get(0).isAscending()).isTrue();
        }
    }

    @Nested
    @DisplayName("边界和特殊场景测试")
    class EdgeCaseTests {

        @Test
        @DisplayName("极小页码修正边界")
        void shouldClampMinPage() {
            PageRequest request = new PageRequest(Integer.MIN_VALUE, 10, null);

            assertThat(request.page()).isEqualTo(1);
        }

        @Test
        @DisplayName("极大页码保持不变")
        void shouldKeepMaxPage() {
            PageRequest request = new PageRequest(Integer.MAX_VALUE, 10, null);

            assertThat(request.page()).isEqualTo(Integer.MAX_VALUE);
        }

        @Test
        @DisplayName("极大每页大小修正")
        void shouldClampMaxSize() {
            PageRequest request = new PageRequest(1, Integer.MAX_VALUE, null);

            assertThat(request.size()).isEqualTo(PageRequest.MAX_PAGE_SIZE);
        }

        @Test
        @DisplayName("完整参数组合测试")
        void shouldHandleFullParameterCombination() {
            Sort sort = Sort.asc("id");
            PageRequest r1 = PageRequest.of(1, 10);  // 无排序
            PageRequest r2 = PageRequest.of(5, 50, sort);  // 有排序
            PageRequest r3 = PageRequest.defaultPage();  // 默认页

            assertThat(r1.hasSort()).isFalse();
            assertThat(r2.hasSort()).isTrue();
            assertThat(r3.page()).isEqualTo(1);
            assertThat(r3.size()).isEqualTo(10);
        }

        @Test
        @DisplayName("链式边界修正测试")
        void shouldHandleMultipleClamping() {
            // 页码和大小都需要修正
            PageRequest request = new PageRequest(-100, -50, null);

            assertThat(request.page()).isEqualTo(1);
            assertThat(request.size()).isEqualTo(PageRequest.DEFAULT_PAGE_SIZE);
            assertThat(request.offset()).isEqualTo(0);
        }

        @Test
        @DisplayName("所有参数同时修正")
        void shouldClampAllParameters() {
            PageRequest request = new PageRequest(Integer.MIN_VALUE, Integer.MAX_VALUE, null);

            assertThat(request.page()).isEqualTo(1);
            assertThat(request.size()).isEqualTo(PageRequest.MAX_PAGE_SIZE);
        }
    }

    @Nested
    @DisplayName("Record特性测试")
    class RecordTests {

        @Test
        @DisplayName("equals和hashCode测试")
        void shouldHaveCorrectEqualsAndHashCode() {
            Sort sort = Sort.asc("name");
            PageRequest r1 = new PageRequest(2, 20, sort);
            PageRequest r2 = new PageRequest(2, 20, sort);
            PageRequest r3 = new PageRequest(2, 20, null);

            assertThat(r1).isEqualTo(r2);
            assertThat(r1.hashCode()).isEqualTo(r2.hashCode());
            assertThat(r1).isNotEqualTo(r3);
        }

        @Test
        @DisplayName("toString包含关键字段")
        void shouldHaveToStringWithFields() {
            PageRequest request = new PageRequest(3, 25, Sort.asc("name"));
            String str = request.toString();

            assertThat(str).contains("3");
            assertThat(str).contains("25");
        }

        @Test
        @DisplayName("无排序时的toString")
        void shouldHaveToStringWithoutSort() {
            PageRequest request = new PageRequest(1, 10, null);
            String str = request.toString();

            assertThat(str).contains("1");
            assertThat(str).contains("10");
        }
    }
}
