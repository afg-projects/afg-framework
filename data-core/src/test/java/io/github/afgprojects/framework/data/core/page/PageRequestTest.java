package io.github.afgprojects.framework.data.core.page;

import io.github.afgprojects.framework.data.core.query.Sort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PageRequest 测试
 */
@DisplayName("PageRequest 测试")
class PageRequestTest {

    @Nested
    @DisplayName("of 方法")
    class OfTests {

        @Test
        @DisplayName("of(1, 10) 应创建正确的分页请求")
        void shouldCreateCorrectPageRequest_whenOf() {
            PageRequest request = PageRequest.of(1, 10);
            assertThat(request.page()).isEqualTo(1);
            assertThat(request.size()).isEqualTo(10);
            assertThat(request.sort()).isNull();
        }

        @Test
        @DisplayName("of(1, 10, sort) 应创建带排序的分页请求")
        void shouldCreatePageRequestWithSort_whenOfWithSort() {
            Sort sort = Sort.asc("name");
            PageRequest request = PageRequest.of(1, 10, sort);
            assertThat(request.sort()).isEqualTo(sort);
        }
    }

    @Nested
    @DisplayName("参数纠正")
    class ParameterCorrectionTests {

        @Test
        @DisplayName("of(0, 0) 应纠正为 page=1, size=10")
        void shouldCorrectToDefault_whenZeroValues() {
            PageRequest request = PageRequest.of(0, 0);
            assertThat(request.page()).isEqualTo(1);
            assertThat(request.size()).isEqualTo(10);
        }

        @Test
        @DisplayName("of(-1, -5) 应纠正为 page=1, size=10")
        void shouldCorrectToDefault_whenNegativeValues() {
            PageRequest request = PageRequest.of(-1, -5);
            assertThat(request.page()).isEqualTo(1);
            assertThat(request.size()).isEqualTo(10);
        }

        @Test
        @DisplayName("of(1, 2000) 应限制 size 为 1000")
        void shouldLimitSize_whenExceedsMax() {
            PageRequest request = PageRequest.of(1, 2000);
            assertThat(request.size()).isEqualTo(1000);
        }
    }

    @Nested
    @DisplayName("offset 方法")
    class OffsetTests {

        @Test
        @DisplayName("offset() = (page-1) * size")
        void shouldCalculateOffsetCorrectly() {
            assertThat(PageRequest.of(1, 10).offset()).isEqualTo(0);
            assertThat(PageRequest.of(2, 10).offset()).isEqualTo(10);
            assertThat(PageRequest.of(3, 20).offset()).isEqualTo(40);
        }
    }

    @Nested
    @DisplayName("nextPage / previousPage 方法")
    class NavigationTests {

        @Test
        @DisplayName("nextPage() 应返回下一页")
        void shouldReturnNextPage_whenNextPage() {
            PageRequest request = PageRequest.of(1, 10);
            PageRequest next = request.nextPage();
            assertThat(next.page()).isEqualTo(2);
            assertThat(next.size()).isEqualTo(10);
        }

        @Test
        @DisplayName("previousPage() 应返回上一页")
        void shouldReturnPreviousPage_whenPreviousPage() {
            PageRequest request = PageRequest.of(3, 10);
            PageRequest previous = request.previousPage();
            assertThat(previous.page()).isEqualTo(2);
        }

        @Test
        @DisplayName("previousPage() 在第1页时应返回第1页")
        void shouldReturnFirstPage_whenPreviousFromFirst() {
            PageRequest request = PageRequest.of(1, 10);
            PageRequest previous = request.previousPage();
            assertThat(previous.page()).isEqualTo(1);
        }

        @Test
        @DisplayName("nextPage/previousPage 应保留排序")
        void shouldPreserveSort_whenNavigation() {
            Sort sort = Sort.asc("name");
            PageRequest request = PageRequest.of(1, 10, sort);
            PageRequest next = request.nextPage();
            assertThat(next.sort()).isEqualTo(sort);
        }
    }

    @Nested
    @DisplayName("totalPages 方法")
    class TotalPagesTests {

        @Test
        @DisplayName("totalPages(100) size=10 应返回 10")
        void shouldReturn10_whenTotal100Size10() {
            PageRequest request = PageRequest.of(1, 10);
            assertThat(request.totalPages(100)).isEqualTo(10);
        }

        @Test
        @DisplayName("totalPages(0) 应返回 0")
        void shouldReturn0_whenTotal0() {
            PageRequest request = PageRequest.of(1, 10);
            assertThat(request.totalPages(0)).isEqualTo(0);
        }

        @Test
        @DisplayName("totalPages(15) size=10 应返回 2")
        void shouldReturn2_whenTotal15Size10() {
            PageRequest request = PageRequest.of(1, 10);
            assertThat(request.totalPages(15)).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("defaultPage 方法")
    class DefaultPageTests {

        @Test
        @DisplayName("defaultPage() 应返回 page=1, size=10")
        void shouldReturnDefaultPage_whenDefaultPage() {
            PageRequest request = PageRequest.defaultPage();
            assertThat(request.page()).isEqualTo(1);
            assertThat(request.size()).isEqualTo(10);
            assertThat(request.sort()).isNull();
        }
    }

    @Nested
    @DisplayName("firstPage 方法")
    class FirstPageTests {

        @Test
        @DisplayName("firstPage(20) 应返回 page=1, size=20")
        void shouldReturnFirstPage_whenFirstPage() {
            PageRequest request = PageRequest.firstPage(20);
            assertThat(request.page()).isEqualTo(1);
            assertThat(request.size()).isEqualTo(20);
        }
    }

    @Nested
    @DisplayName("hasSort 方法")
    class HasSortTests {

        @Test
        @DisplayName("无排序时 hasSort 应返回 false")
        void shouldReturnFalse_whenNoSort() {
            PageRequest request = PageRequest.of(1, 10);
            assertThat(request.hasSort()).isFalse();
        }

        @Test
        @DisplayName("有排序时 hasSort 应返回 true")
        void shouldReturnTrue_whenHasSort() {
            PageRequest request = PageRequest.of(1, 10, Sort.asc("name"));
            assertThat(request.hasSort()).isTrue();
        }

        @Test
        @DisplayName("UNSORTED 排序时 hasSort 应返回 false")
        void shouldReturnFalse_whenUnsorted() {
            PageRequest request = PageRequest.of(1, 10, Sort.unsorted());
            assertThat(request.hasSort()).isFalse();
        }
    }

    @Nested
    @DisplayName("isFirst / hasPrevious 方法")
    class FirstPreviousTests {

        @Test
        @DisplayName("page=1 时 isFirst=true, hasPrevious=false")
        void shouldReturnTrue_whenFirstPage() {
            PageRequest request = PageRequest.of(1, 10);
            assertThat(request.isFirst()).isTrue();
            assertThat(request.hasPrevious()).isFalse();
        }

        @Test
        @DisplayName("page>1 时 isFirst=false, hasPrevious=true")
        void shouldReturnFalse_whenNotFirstPage() {
            PageRequest request = PageRequest.of(2, 10);
            assertThat(request.isFirst()).isFalse();
            assertThat(request.hasPrevious()).isTrue();
        }
    }

    @Nested
    @DisplayName("withPage / withSize / withSort 方法")
    class WithMethodsTests {

        @Test
        @DisplayName("withPage(5) 应返回新页码")
        void shouldReturnNewPage_whenWithPage() {
            PageRequest request = PageRequest.of(1, 10);
            PageRequest newRequest = request.withPage(5);
            assertThat(newRequest.page()).isEqualTo(5);
            assertThat(newRequest.size()).isEqualTo(10);
        }

        @Test
        @DisplayName("withSize(20) 应返回新大小")
        void shouldReturnNewSize_whenWithSize() {
            PageRequest request = PageRequest.of(1, 10);
            PageRequest newRequest = request.withSize(20);
            assertThat(newRequest.size()).isEqualTo(20);
            assertThat(newRequest.page()).isEqualTo(1);
        }

        @Test
        @DisplayName("withSort(sort) 应返回新排序")
        void shouldReturnNewSort_whenWithSort() {
            PageRequest request = PageRequest.of(1, 10);
            Sort sort = Sort.desc("age");
            PageRequest newRequest = request.withSort(sort);
            assertThat(newRequest.sort()).isEqualTo(sort);
        }

        @Test
        @DisplayName("withSort(direction, properties) 应返回新排序")
        void shouldReturnNewSort_whenWithSortDirection() {
            PageRequest request = PageRequest.of(1, 10);
            PageRequest newRequest = request.withSort(Sort.Direction.DESC, "age");
            assertThat(newRequest.hasSort()).isTrue();
        }
    }
}
