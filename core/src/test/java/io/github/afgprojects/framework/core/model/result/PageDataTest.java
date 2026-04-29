package io.github.afgprojects.framework.core.model.result;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PageDataTest {

    @Nested
    @DisplayName("Factory Method Tests")
    class FactoryMethodTests {

        @Test
        @DisplayName("empty() should return empty page data")
        void empty_shouldReturnEmptyPageData() {
            PageData<String> page = PageData.empty();

            assertThat(page.records()).isEmpty();
            assertThat(page.total()).isZero();
            assertThat(page.page()).isEqualTo(1);
            assertThat(page.size()).isEqualTo(10);
            assertThat(page.pages()).isZero();
            assertThat(page.hasNext()).isFalse();
            assertThat(page.hasPrevious()).isFalse();
        }

        @Test
        @DisplayName("of() should create page data with correct derived fields")
        void of_shouldCreatePageDataWithDerivedFields() {
            List<String> records = List.of("a", "b", "c");

            PageData<String> page = PageData.of(records, 25, 2, 10);

            assertThat(page.records()).containsExactly("a", "b", "c");
            assertThat(page.total()).isEqualTo(25);
            assertThat(page.page()).isEqualTo(2);
            assertThat(page.size()).isEqualTo(10);
            assertThat(page.pages()).isEqualTo(3);
            assertThat(page.hasNext()).isTrue();
            assertThat(page.hasPrevious()).isTrue();
        }

        @Test
        @DisplayName("of() with first page should have no previous")
        void of_firstPage_shouldHaveNoPrevious() {
            PageData<String> page = PageData.of(List.of("a"), 25, 1, 10);

            assertThat(page.hasPrevious()).isFalse();
            assertThat(page.hasNext()).isTrue();
        }

        @Test
        @DisplayName("of() with last page should have no next")
        void of_lastPage_shouldHaveNoNext() {
            PageData<String> page = PageData.of(List.of("a"), 10, 1, 10);

            assertThat(page.hasNext()).isFalse();
        }
    }

    @Nested
    @DisplayName("Immutability Tests")
    class ImmutabilityTests {

        @Test
        @DisplayName("records should be unmodifiable")
        void records_shouldBeUnmodifiable() {
            PageData<String> page = PageData.of(new ArrayList<>(List.of("a", "b")), 2, 1, 10);

            assertThatThrownBy(() -> page.records().add("c")).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("null records should be treated as empty list")
        void nullRecords_shouldBeTreatedAsEmptyList() {
            PageData<String> page = new PageData<>(null, 0, 1, 10, 0, false, false);

            assertThat(page.records()).isEmpty();
        }

        @Test
        @DisplayName("original list modification should not affect page data")
        void originalListModification_shouldNotAffectPageData() {
            ArrayList<String> original = new ArrayList<>(List.of("a", "b"));
            PageData<String> page = PageData.of(original, 2, 1, 10);

            original.add("c");

            assertThat(page.records()).containsExactly("a", "b");
        }
    }

    @Nested
    @DisplayName("Derived Fields Tests")
    class DerivedFieldsTests {

        @Test
        @DisplayName("pages should be calculated correctly")
        void pages_shouldBeCalculatedCorrectly() {
            assertThat(PageData.of(List.of("a"), 100, 1, 10).pages()).isEqualTo(10);
            assertThat(PageData.of(List.of("a"), 101, 1, 10).pages()).isEqualTo(11);
            assertThat(PageData.of(List.of("a"), 0, 1, 10).pages()).isZero();
        }

        @Test
        @DisplayName("pages should be 0 when size is 0")
        void pages_shouldBeZeroWhenSizeIsZero() {
            PageData<String> page = PageData.of(List.of(), 0, 1, 0);

            assertThat(page.pages()).isZero();
        }
    }
}
