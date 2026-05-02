package io.github.afgprojects.framework.core.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * BatchResult 测试
 */
@DisplayName("BatchResult 测试")
class BatchResultTest {

    @Nested
    @DisplayName("静态工厂方法测试")
    class StaticFactoryTests {

        @Test
        @DisplayName("应该创建空结果")
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
    @DisplayName("Builder 测试")
    class BuilderTests {

        @Test
        @DisplayName("应该使用 Builder 构建结果")
        void shouldBuildResult() {
            BatchError error = BatchError.of(0, "失败");
            BatchResult<String> result = BatchResult.<String>builder()
                    .total(10)
                    .success(8)
                    .failed(2)
                    .addResult("result-1")
                    .addResult("result-2")
                    .addError(error)
                    .duration(Duration.ofMillis(100))
                    .build();

            assertThat(result.total()).isEqualTo(10);
            assertThat(result.success()).isEqualTo(8);
            assertThat(result.failed()).isEqualTo(2);
            assertThat(result.results()).hasSize(2);
            assertThat(result.errors()).hasSize(1);
            assertThat(result.duration()).isEqualTo(Duration.ofMillis(100));
        }

        @Test
        @DisplayName("应该支持批量添加结果")
        void shouldAddAllResults() {
            BatchResult<String> result = BatchResult.<String>builder()
                    .addAllResults(List.of("r1", "r2", "r3"))
                    .build();

            assertThat(result.results()).hasSize(3);
        }

        @Test
        @DisplayName("应该支持批量添加错误")
        void shouldAddAllErrors() {
            BatchError error1 = BatchError.of(0, "error1");
            BatchError error2 = BatchError.of(1, "error2");

            BatchResult<String> result = BatchResult.<String>builder()
                    .addAllErrors(List.of(error1, error2))
                    .build();

            assertThat(result.errors()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("状态判断测试")
    class StatusTests {

        @Test
        @DisplayName("isAllSuccess 应该正确判断")
        void shouldCheckAllSuccess() {
            BatchResult<String> allSuccess = new BatchResult<>(10, 10, 0, Collections.emptyList(), Collections.emptyList(), Duration.ZERO);
            BatchResult<String> partialSuccess = new BatchResult<>(10, 8, 2, Collections.emptyList(), Collections.emptyList(), Duration.ZERO);
            BatchResult<String> empty = BatchResult.empty();

            assertThat(allSuccess.isAllSuccess()).isTrue();
            assertThat(partialSuccess.isAllSuccess()).isFalse();
            assertThat(empty.isAllSuccess()).isFalse();
        }

        @Test
        @DisplayName("isAllFailed 应该正确判断")
        void shouldCheckAllFailed() {
            BatchResult<String> allFailed = new BatchResult<>(10, 0, 10, Collections.emptyList(), Collections.emptyList(), Duration.ZERO);
            BatchResult<String> partialFailed = new BatchResult<>(10, 8, 2, Collections.emptyList(), Collections.emptyList(), Duration.ZERO);
            BatchResult<String> empty = BatchResult.empty();

            assertThat(allFailed.isAllFailed()).isTrue();
            assertThat(partialFailed.isAllFailed()).isFalse();
            assertThat(empty.isAllFailed()).isFalse();
        }

        @Test
        @DisplayName("getSuccessRate 应该正确计算")
        void shouldCalculateSuccessRate() {
            BatchResult<String> result = new BatchResult<>(10, 8, 2, Collections.emptyList(), Collections.emptyList(), Duration.ZERO);
            BatchResult<String> empty = BatchResult.empty();

            assertThat(result.getSuccessRate()).isEqualTo(0.8);
            assertThat(empty.getSuccessRate()).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("不可变列表测试")
    class ImmutableListTests {

        @Test
        @DisplayName("results 应该是不可变的")
        void shouldHaveImmutableResults() {
            BatchResult<String> result = BatchResult.<String>builder()
                    .addResult("test")
                    .build();

            assertThatThrownBy(() -> result.results().add("new"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("errors 应该是不可变的")
        void shouldHaveImmutableErrors() {
            BatchError error = BatchError.of(0, "error");
            BatchResult<String> result = BatchResult.<String>builder()
                    .addError(error)
                    .build();

            assertThatThrownBy(() -> result.errors().add(BatchError.of(1, "new")))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

}
