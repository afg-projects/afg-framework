package io.github.afgprojects.framework.commons.model;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * PageData 测试
 */
@DisplayName("PageData 测试")
class PageDataTest {

    @Nested
    @DisplayName("of(records, total, page, size) 方法")
    class OfTests {

        @Test
        @DisplayName("应正确计算 pages, hasNext, hasPrevious")
        void shouldCalculatePaginationFields() {
            List<String> records = List.of("a", "b", "c");

            PageData<String> pageData = PageData.of(records, 25, 2, 10);

            assertThat(pageData.records()).containsExactly("a", "b", "c");
            assertThat(pageData.total()).isEqualTo(25);
            assertThat(pageData.page()).isEqualTo(2);
            assertThat(pageData.size()).isEqualTo(10);
            assertThat(pageData.pages()).isEqualTo(3);
            assertThat(pageData.hasNext()).isTrue();
            assertThat(pageData.hasPrevious()).isTrue();
        }

        @Test
        @DisplayName("第一页 hasPrevious 应为 false")
        void shouldHaveNoPreviousOnFirstPage() {
            PageData<String> pageData = PageData.of(List.of("a"), 20, 1, 10);

            assertThat(pageData.hasPrevious()).isFalse();
            assertThat(pageData.hasNext()).isTrue();
        }

        @Test
        @DisplayName("最后一页 hasNext 应为 false")
        void shouldHaveNoNextOnLastPage() {
            PageData<String> pageData = PageData.of(List.of("a"), 10, 1, 10);

            assertThat(pageData.pages()).isEqualTo(1);
            assertThat(pageData.hasNext()).isFalse();
        }

        @Test
        @DisplayName("total=0 时 pages=0, hasNext=false, hasPrevious=false")
        void shouldHandleZeroTotal() {
            PageData<String> pageData = PageData.of(List.of(), 0, 1, 10);

            assertThat(pageData.total()).isEqualTo(0);
            assertThat(pageData.pages()).isEqualTo(0);
            assertThat(pageData.hasNext()).isFalse();
            assertThat(pageData.hasPrevious()).isFalse();
        }

        @Test
        @DisplayName("total 不能被 size 整除时 pages 应向上取整")
        void shouldCeilPagesWhenNotDivisible() {
            PageData<String> pageData = PageData.of(List.of("a"), 11, 1, 10);

            assertThat(pageData.pages()).isEqualTo(2);
        }

        @Test
        @DisplayName("size=0 时 pages 应为 0")
        void shouldReturnZeroPagesWhenSizeIsZero() {
            PageData<String> pageData = PageData.of(List.of(), 10, 1, 0);

            assertThat(pageData.pages()).isEqualTo(0);
        }

        @Test
        @DisplayName("records 为 null 时应返回空列表")
        void shouldReturnEmptyListWhenRecordsIsNull() {
            PageData<String> pageData = PageData.of(null, 0, 1, 10);

            assertThat(pageData.records()).isEmpty();
        }

        @Test
        @DisplayName("records 应为不可变列表")
        void shouldReturnUnmodifiableRecords() {
            PageData<String> pageData = PageData.of(List.of("a", "b"), 2, 1, 10);

            assertThatThrownBy(() -> pageData.records().add("c"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("empty() 方法")
    class EmptyTests {

        @Test
        @DisplayName("应返回空分页结果")
        void shouldReturnEmptyPageData() {
            PageData<Object> pageData = PageData.empty();

            assertThat(pageData.records()).isEmpty();
            assertThat(pageData.total()).isEqualTo(0);
            assertThat(pageData.page()).isEqualTo(1);
            assertThat(pageData.size()).isEqualTo(10);
            assertThat(pageData.pages()).isEqualTo(0);
            assertThat(pageData.hasNext()).isFalse();
            assertThat(pageData.hasPrevious()).isFalse();
        }
    }

    @Nested
    @DisplayName("serialVersionUID")
    class SerialTests {

        @Test
        @DisplayName("PageData 应实现 Serializable")
        void shouldImplementSerializable() {
            PageData<String> pageData = PageData.of(List.of("a"), 1, 1, 10);
            assertThat(pageData).isInstanceOf(java.io.Serializable.class);
        }
    }

    @Nested
    @DisplayName("empty(page, size) 方法")
    class EmptyWithPageAndSizeTests {

        @Test
        @DisplayName("应返回指定页码和大小的空分页")
        void shouldReturnEmptyWithGivenPageAndSize() {
            PageData<String> pageData = PageData.empty(2, 20);

            assertThat(pageData.records()).isEmpty();
            assertThat(pageData.total()).isEqualTo(0);
            assertThat(pageData.page()).isEqualTo(2);
            assertThat(pageData.size()).isEqualTo(20);
        }
    }

    @Nested
    @DisplayName("singlePage(records) 方法")
    class SinglePageTests {

        @Test
        @DisplayName("应返回单页结果")
        void shouldReturnSinglePage() {
            List<String> records = List.of("a", "b", "c");
            PageData<String> pageData = PageData.singlePage(records);

            assertThat(pageData.records()).containsExactly("a", "b", "c");
            assertThat(pageData.total()).isEqualTo(3);
            assertThat(pageData.page()).isEqualTo(1);
            assertThat(pageData.size()).isEqualTo(3);
            assertThat(pageData.pages()).isEqualTo(1);
            assertThat(pageData.isFirst()).isTrue();
            assertThat(pageData.isLast()).isTrue();
        }
    }

    @Nested
    @DisplayName("map(Function) 方法")
    class MapTests {

        @Test
        @DisplayName("应转换 records 类型并保留分页信息")
        void shouldTransformRecordsAndPreservePagination() {
            List<String> records = List.of("a", "bb", "ccc");
            PageData<String> pageData = PageData.of(records, 100, 1, 10);

            PageData<Integer> mapped = pageData.map(String::length);

            assertThat(mapped.records()).containsExactly(1, 2, 3);
            assertThat(mapped.total()).isEqualTo(100);
            assertThat(mapped.page()).isEqualTo(1);
            assertThat(mapped.size()).isEqualTo(10);
        }

        @Test
        @DisplayName("map 空分页应返回空分页")
        void shouldReturnEmptyWhenMapEmpty() {
            PageData<String> pageData = PageData.empty();
            PageData<Integer> mapped = pageData.map(String::length);

            assertThat(mapped.records()).isEmpty();
            assertThat(mapped.total()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("便捷方法")
    class ConvenienceMethodTests {

        @Test
        @DisplayName("isFirst() 在第一页时应返回 true")
        void shouldReturnTrueWhenFirstPage() {
            PageData<String> pageData = PageData.of(List.of("a"), 20, 1, 10);
            assertThat(pageData.isFirst()).isTrue();
        }

        @Test
        @DisplayName("isFirst() 在非第一页时应返回 false")
        void shouldReturnFalseWhenNotFirstPage() {
            PageData<String> pageData = PageData.of(List.of("a"), 20, 2, 10);
            assertThat(pageData.isFirst()).isFalse();
        }

        @Test
        @DisplayName("isLast() 在最后一页时应返回 true")
        void shouldReturnTrueWhenLastPage() {
            PageData<String> pageData = PageData.of(List.of("a"), 10, 1, 10);
            assertThat(pageData.isLast()).isTrue();
        }

        @Test
        @DisplayName("isLast() 在非最后一页时应返回 false")
        void shouldReturnFalseWhenNotLastPage() {
            PageData<String> pageData = PageData.of(List.of("a"), 20, 1, 10);
            assertThat(pageData.isLast()).isFalse();
        }

        @Test
        @DisplayName("hasContent() 有内容时应返回 true")
        void shouldReturnTrueWhenHasContent() {
            PageData<String> pageData = PageData.of(List.of("a"), 1, 1, 10);
            assertThat(pageData.hasContent()).isTrue();
        }

        @Test
        @DisplayName("hasContent() 无内容时应返回 false")
        void shouldReturnFalseWhenNoContent() {
            PageData<String> pageData = PageData.empty();
            assertThat(pageData.hasContent()).isFalse();
        }

        @Test
        @DisplayName("getNumberOfElements() 应返回当前页记录数")
        void shouldReturnCurrentPageRecordCount() {
            PageData<String> pageData = PageData.of(List.of("a", "b", "c"), 100, 1, 10);
            assertThat(pageData.getNumberOfElements()).isEqualTo(3);
        }

        @Test
        @DisplayName("getOffset() 应正确计算偏移量")
        void shouldCalculateOffset() {
            PageData<String> pageData = PageData.of(List.of("a"), 100, 3, 10);
            assertThat(pageData.getOffset()).isEqualTo(20);
        }

        @Test
        @DisplayName("getOffset() 第一页偏移量应为 0")
        void shouldReturnZeroOffsetOnFirstPage() {
            PageData<String> pageData = PageData.of(List.of("a"), 100, 1, 10);
            assertThat(pageData.getOffset()).isEqualTo(0);
        }
    }
}
