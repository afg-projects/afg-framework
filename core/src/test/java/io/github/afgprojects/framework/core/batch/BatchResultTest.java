package io.github.afgprojects.framework.core.batch;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("BatchResult")
class BatchResultTest {

    @Nested
    @DisplayName("empty")
    class Empty {

        @Test
        @DisplayName("should create empty result")
        void shouldCreateEmptyResult() {
            BatchResult<String> result = BatchResult.empty();

            assertThat(result.total()).isEqualTo(0);
            assertThat(result.success()).isEqualTo(0);
            assertThat(result.failed()).isEqualTo(0);
            assertThat(result.results()).isEmpty();
            assertThat(result.errors()).isEmpty();
            assertThat(result.duration()).isEqualTo(Duration.ZERO);
        }
    }

    @Nested
    @DisplayName("isAllSuccess")
    class IsAllSuccess {

        @Test
        @DisplayName("should return true when all succeed")
        void shouldReturnTrue_whenAllSucceed() {
            BatchResult<String> result = new BatchResult<>(3, 3, 0,
                    List.of("a", "b", "c"), List.of(), Duration.ZERO);

            assertThat(result.isAllSuccess()).isTrue();
        }

        @Test
        @DisplayName("should return false when some fail")
        void shouldReturnFalse_whenSomeFail() {
            BatchResult<String> result = new BatchResult<>(3, 2, 1,
                    List.of("a", "b"), List.of(BatchError.of(2, "error")), Duration.ZERO);

            assertThat(result.isAllSuccess()).isFalse();
        }

        @Test
        @DisplayName("should return false when total is zero")
        void shouldReturnFalse_whenTotalIsZero() {
            BatchResult<String> result = BatchResult.empty();

            assertThat(result.isAllSuccess()).isFalse();
        }
    }

    @Nested
    @DisplayName("isAllFailed")
    class IsAllFailed {

        @Test
        @DisplayName("should return true when all fail")
        void shouldReturnTrue_whenAllFail() {
            BatchResult<String> result = new BatchResult<>(2, 0, 2,
                    List.of(), List.of(BatchError.of(0, "e1"), BatchError.of(1, "e2")), Duration.ZERO);

            assertThat(result.isAllFailed()).isTrue();
        }

        @Test
        @DisplayName("should return false when some succeed")
        void shouldReturnFalse_whenSomeSucceed() {
            BatchResult<String> result = new BatchResult<>(2, 1, 1,
                    List.of("a"), List.of(BatchError.of(1, "e1")), Duration.ZERO);

            assertThat(result.isAllFailed()).isFalse();
        }
    }

    @Nested
    @DisplayName("getSuccessRate")
    class GetSuccessRate {

        @Test
        @DisplayName("should return 1.0 when all succeed")
        void shouldReturn1_whenAllSucceed() {
            BatchResult<String> result = new BatchResult<>(3, 3, 0,
                    List.of("a", "b", "c"), List.of(), Duration.ZERO);

            assertThat(result.getSuccessRate()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("should return 0.0 when all fail")
        void shouldReturn0_whenAllFail() {
            BatchResult<String> result = new BatchResult<>(3, 0, 3,
                    List.of(), List.of(BatchError.of(0, "e"), BatchError.of(1, "e"), BatchError.of(2, "e")), Duration.ZERO);

            assertThat(result.getSuccessRate()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("should return correct rate for partial success")
        void shouldReturnCorrectRate_forPartialSuccess() {
            BatchResult<String> result = new BatchResult<>(4, 3, 1,
                    List.of("a", "b", "c"), List.of(BatchError.of(3, "e")), Duration.ZERO);

            assertThat(result.getSuccessRate()).isEqualTo(0.75);
        }

        @Test
        @DisplayName("should return 0.0 when total is zero")
        void shouldReturn0_whenTotalIsZero() {
            BatchResult<String> result = BatchResult.empty();

            assertThat(result.getSuccessRate()).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("immutability")
    class Immutability {

        @Test
        @DisplayName("should return unmodifiable results list")
        void shouldReturnUnmodifiableResultsList() {
            BatchResult<String> result = new BatchResult<>(1, 1, 0,
                    List.of("a"), List.of(), Duration.ZERO);

            assertThatThrownBy(() -> result.results().add("b"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("should return unmodifiable errors list")
        void shouldReturnUnmodifiableErrorsList() {
            BatchResult<String> result = new BatchResult<>(1, 0, 1,
                    List.of(), List.of(BatchError.of(0, "e")), Duration.ZERO);

            assertThatThrownBy(() -> result.errors().add(BatchError.of(1, "e2")))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTest {

        @Test
        @DisplayName("should build result with all fields")
        void shouldBuildResultWithAllFields() {
            BatchResult<String> result = BatchResult.<String>builder()
                    .total(2)
                    .success(2)
                    .failed(0)
                    .addResult("a")
                    .addResult("b")
                    .duration(Duration.ofMillis(100))
                    .build();

            assertThat(result.total()).isEqualTo(2);
            assertThat(result.success()).isEqualTo(2);
            assertThat(result.failed()).isEqualTo(0);
            assertThat(result.results()).containsExactly("a", "b");
            assertThat(result.duration()).isEqualTo(Duration.ofMillis(100));
        }

        @Test
        @DisplayName("should add errors")
        void shouldAddErrors() {
            BatchResult<String> result = BatchResult.<String>builder()
                    .total(1)
                    .success(0)
                    .failed(1)
                    .addError(BatchError.of(0, "error"))
                    .build();

            assertThat(result.errors()).hasSize(1);
            assertThat(result.errors().get(0).error()).isEqualTo("error");
        }

        @Test
        @DisplayName("should add all results")
        void shouldAddAllResults() {
            BatchResult<String> result = BatchResult.<String>builder()
                    .total(2)
                    .success(2)
                    .addAllResults(List.of("a", "b"))
                    .build();

            assertThat(result.results()).containsExactly("a", "b");
        }
    }
}
