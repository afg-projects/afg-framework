package io.github.afgprojects.framework.data.core.query;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Page 测试
 */
@DisplayName("Page 测试")
class PageTest {

    @Nested
    @DisplayName("of 方法")
    class OfTests {

        @Test
        @DisplayName("of(content, 100, 1, 10) 应正确计算分页属性")
        void shouldCalculateCorrectly_whenFirstPage() {
            List<String> content = Arrays.asList("a", "b", "c");
            Page<String> page = Page.of(content, 100, 1, 10);

            assertThat(page.getContent()).containsExactly("a", "b", "c");
            assertThat(page.getTotal()).isEqualTo(100);
            assertThat(page.getPage()).isEqualTo(1);
            assertThat(page.getSize()).isEqualTo(10);
            assertThat(page.getTotalPages()).isEqualTo(10);
            assertThat(page.isFirst()).isTrue();
            assertThat(page.isLast()).isFalse();
            assertThat(page.hasNext()).isTrue();
            assertThat(page.hasPrevious()).isFalse();
            assertThat(page.getOffset()).isEqualTo(0);
            assertThat(page.getNumberOfElements()).isEqualTo(3);
            assertThat(page.hasContent()).isTrue();
        }

        @Test
        @DisplayName("of(content, 15, 2, 10) 应正确计算最后一页")
        void shouldCalculateCorrectly_whenLastPage() {
            List<String> content = Arrays.asList("a", "b", "c", "d", "e");
            Page<String> page = Page.of(content, 15, 2, 10);

            assertThat(page.getTotalPages()).isEqualTo(2);
            assertThat(page.isFirst()).isFalse();
            assertThat(page.isLast()).isTrue();
            assertThat(page.hasNext()).isFalse();
            assertThat(page.hasPrevious()).isTrue();
            assertThat(page.getOffset()).isEqualTo(10);
        }

        @Test
        @DisplayName("负值参数应被纠正")
        void shouldCorrectNegativeValues() {
            List<String> content = Arrays.asList("a");
            Page<String> page = Page.of(content, -10, -1, -5);

            assertThat(page.getTotal()).isEqualTo(0);
            assertThat(page.getPage()).isEqualTo(1);
            assertThat(page.getSize()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("empty 方法")
    class EmptyTests {

        @Test
        @DisplayName("empty() 应返回空分页")
        void shouldReturnEmptyPage_whenEmpty() {
            Page<String> page = Page.empty();

            assertThat(page.getContent()).isEmpty();
            assertThat(page.getTotal()).isEqualTo(0);
            assertThat(page.getPage()).isEqualTo(1);
            assertThat(page.getSize()).isEqualTo(10);
            assertThat(page.hasContent()).isFalse();
            assertThat(page.getTotalPages()).isEqualTo(0);
        }

        @Test
        @DisplayName("empty(page, size) 应返回指定参数的空分页")
        void shouldReturnEmptyPageWithParams_whenEmpty() {
            Page<String> page = Page.empty(2, 20);

            assertThat(page.getContent()).isEmpty();
            assertThat(page.getPage()).isEqualTo(2);
            assertThat(page.getSize()).isEqualTo(20);
        }
    }

    @Nested
    @DisplayName("singlePage 方法")
    class SinglePageTests {

        @Test
        @DisplayName("singlePage(content) 应返回单页结果")
        void shouldReturnSinglePage_whenSinglePage() {
            List<String> content = Arrays.asList("a", "b", "c");
            Page<String> page = Page.singlePage(content);

            assertThat(page.getContent()).containsExactly("a", "b", "c");
            assertThat(page.getTotal()).isEqualTo(3);
            assertThat(page.getPage()).isEqualTo(1);
            assertThat(page.getSize()).isEqualTo(3);
            assertThat(page.getTotalPages()).isEqualTo(1);
            assertThat(page.isFirst()).isTrue();
            assertThat(page.isLast()).isTrue();
        }
    }

    @Nested
    @DisplayName("map 方法")
    class MapTests {

        @Test
        @DisplayName("map(Function) 应转换 content 类型")
        void shouldTransformContent_whenMap() {
            List<String> content = Arrays.asList("a", "bb", "ccc");
            Page<String> page = Page.of(content, 100, 1, 10);

            Page<Integer> mapped = page.map(String::length);

            assertThat(mapped.getContent()).containsExactly(1, 2, 3);
            assertThat(mapped.getTotal()).isEqualTo(100);
            assertThat(mapped.getPage()).isEqualTo(1);
            assertThat(mapped.getSize()).isEqualTo(10);
        }

        @Test
        @DisplayName("map 空分页应返回空分页")
        void shouldReturnEmpty_whenMapEmpty() {
            Page<String> page = Page.empty();
            Page<Integer> mapped = page.map(String::length);

            assertThat(mapped.getContent()).isEmpty();
            assertThat(mapped.getTotal()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("边界情况")
    class EdgeCaseTests {

        @Test
        @DisplayName("total=0 时 totalPages 应为 0")
        void shouldReturnZeroTotalPage_whenTotalIsZero() {
            Page<String> page = Page.of(Collections.emptyList(), 0, 1, 10);
            assertThat(page.getTotalPages()).isEqualTo(0);
        }

        @Test
        @DisplayName("total 不能被 size 整除时 totalPages 应向上取整")
        void shouldRoundUpTotalPages_whenNotDivisible() {
            Page<String> page = Page.of(Collections.emptyList(), 25, 1, 10);
            assertThat(page.getTotalPages()).isEqualTo(3);
        }

        @Test
        @DisplayName("content 不可修改")
        void shouldReturnUnmodifiableContent() {
            List<String> content = Arrays.asList("a", "b");
            Page<String> page = Page.of(content, 10, 1, 10);

            assertThatThrownBy(() -> page.getContent().add("c"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("toString 方法")
    class ToStringTests {

        @Test
        @DisplayName("toString 应包含所有字段")
        void shouldContainAllFields_whenToString() {
            List<String> content = Arrays.asList("a", "b");
            Page<String> page = Page.of(content, 100, 1, 10);

            String result = page.toString();
            assertThat(result).contains("content=[a, b]");
            assertThat(result).contains("total=100");
            assertThat(result).contains("page=1");
            assertThat(result).contains("size=10");
        }
    }

}
