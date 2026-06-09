package io.github.afgprojects.framework.core.model.result;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.commons.model.PageData;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PageData")
class PageDataTest {

    @Nested
    @DisplayName("of")
    class Of {

        @Test
        @DisplayName("should create PageData with correct pagination info")
        void shouldCreatePageDataWithCorrectPaginationInfo() {
            var data = PageData.of(java.util.List.of("a", "b", "c"), 30, 1, 10);

            assertThat(data.records()).containsExactly("a", "b", "c");
            assertThat(data.total()).isEqualTo(30);
            assertThat(data.page()).isEqualTo(1);
            assertThat(data.size()).isEqualTo(10);
            assertThat(data.pages()).isEqualTo(3);
            assertThat(data.hasNext()).isTrue();
            assertThat(data.hasPrevious()).isFalse();
        }

        @Test
        @DisplayName("should calculate pages correctly for exact division")
        void shouldCalculatePagesCorrectly_forExactDivision() {
            var data = PageData.of(java.util.List.of(), 20, 1, 10);

            assertThat(data.pages()).isEqualTo(2);
        }

        @Test
        @DisplayName("should calculate pages correctly for partial last page")
        void shouldCalculatePagesCorrectly_forPartialLastPage() {
            var data = PageData.of(java.util.List.of(), 21, 1, 10);

            assertThat(data.pages()).isEqualTo(3);
        }

        @Test
        @DisplayName("should set hasNext true when not on last page")
        void shouldSetHasNextTrue_whenNotOnLastPage() {
            var data = PageData.of(java.util.List.of(), 30, 1, 10);

            assertThat(data.hasNext()).isTrue();
        }

        @Test
        @DisplayName("should set hasNext false when on last page")
        void shouldSetHasNextFalse_whenOnLastPage() {
            var data = PageData.of(java.util.List.of(), 20, 2, 10);

            assertThat(data.hasNext()).isFalse();
        }

        @Test
        @DisplayName("should set hasPrevious true when not on first page")
        void shouldSetHasPreviousTrue_whenNotOnFirstPage() {
            var data = PageData.of(java.util.List.of(), 30, 2, 10);

            assertThat(data.hasPrevious()).isTrue();
        }

        @Test
        @DisplayName("should set hasPrevious false when on first page")
        void shouldSetHasPreviousFalse_whenOnFirstPage() {
            var data = PageData.of(java.util.List.of(), 30, 1, 10);

            assertThat(data.hasPrevious()).isFalse();
        }

        @Test
        @DisplayName("should handle zero size gracefully")
        void shouldHandleZeroSizeGracefully() {
            var data = PageData.of(java.util.List.of(), 30, 1, 0);

            assertThat(data.pages()).isEqualTo(0);
            assertThat(data.hasNext()).isFalse();
        }
    }

    @Nested
    @DisplayName("empty")
    class Empty {

        @Test
        @DisplayName("should create empty PageData")
        void shouldCreateEmptyPageData() {
            var data = PageData.empty();

            assertThat(data.records()).isEmpty();
            assertThat(data.total()).isEqualTo(0);
            assertThat(data.page()).isEqualTo(1);
            assertThat(data.size()).isEqualTo(10);
            assertThat(data.pages()).isEqualTo(0);
            assertThat(data.hasNext()).isFalse();
            assertThat(data.hasPrevious()).isFalse();
        }
    }
}