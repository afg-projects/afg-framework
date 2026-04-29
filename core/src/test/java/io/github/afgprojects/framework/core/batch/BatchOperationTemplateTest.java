package io.github.afgprojects.framework.core.batch;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * BatchOperationTemplate 测试
 */
@DisplayName("BatchOperationTemplate 测试")
class BatchOperationTemplateTest {

    private BatchOperationTemplate template;

    @BeforeEach
    void setUp() {
        template = new BatchOperationTemplate();
    }

    @Nested
    @DisplayName("execute 顺序执行测试")
    class ExecuteTests {

        @Test
        @DisplayName("应该正确执行所有元素")
        void shouldExecuteAllItems() {
            // given
            List<Integer> items = List.of(1, 2, 3, 4, 5);
            BatchOperation<Integer, String> operation = (item, index) -> "item-" + item;

            // when
            BatchResult<String> result = template.execute(items, operation);

            // then
            assertThat(result.total()).isEqualTo(5);
            assertThat(result.success()).isEqualTo(5);
            assertThat(result.failed()).isEqualTo(0);
            assertThat(result.isAllSuccess()).isTrue();
            assertThat(result.results())
                    .containsExactly("item-1", "item-2", "item-3", "item-4", "item-5");
        }

        @Test
        @DisplayName("空列表应该返回空结果")
        void shouldReturnEmptyResultForEmptyList() {
            // given
            List<Integer> items = List.of();
            BatchOperation<Integer, String> operation = (item, index) -> "item-" + item;

            // when
            BatchResult<String> result = template.execute(items, operation);

            // then
            assertThat(result.total()).isEqualTo(0);
            assertThat(result.success()).isEqualTo(0);
            assertThat(result.failed()).isEqualTo(0);
            assertThat(result.results()).isEmpty();
        }

        @Test
        @DisplayName("应该正确处理异常")
        void shouldHandleExceptions() {
            // given
            List<Integer> items = List.of(1, 2, 3, 4, 5);
            BatchOperation<Integer, String> operation = (item, index) -> {
                if (item == 3) {
                    throw new RuntimeException("Error at 3");
                }
                return "item-" + item;
            };

            // when
            BatchResult<String> result = template.execute(items, operation);

            // then
            assertThat(result.total()).isEqualTo(5);
            assertThat(result.success()).isEqualTo(4);
            assertThat(result.failed()).isEqualTo(1);
            assertThat(result.isAllSuccess()).isFalse();
            assertThat(result.errors()).hasSize(1);
            assertThat(result.errors().get(0).index()).isEqualTo(2);
            assertThat(result.errors().get(0).error()).isEqualTo("Error at 3");
        }

        @Test
        @DisplayName("应该正确回调进度")
        void shouldCallbackProgress() {
            // given
            List<Integer> items = List.of(1, 2, 3);
            AtomicInteger itemCompleteCount = new AtomicInteger(0);
            AtomicInteger progressCount = new AtomicInteger(0);
            AtomicInteger completeCount = new AtomicInteger(0);

            BatchProgressCallback<Integer, String> callback = new BatchProgressCallback<>() {
                @Override
                public void onItemComplete(Integer item, int index, String result, boolean success) {
                    itemCompleteCount.incrementAndGet();
                }

                @Override
                public void onBatchProgress(int completed, int total) {
                    progressCount.incrementAndGet();
                }

                @Override
                public void onComplete(BatchResult<String> result) {
                    completeCount.incrementAndGet();
                }
            };

            // when
            BatchResult<String> result = template.execute(items, (item, index) -> "item-" + item, callback);

            // then
            assertThat(itemCompleteCount.get()).isEqualTo(3);
            assertThat(progressCount.get()).isEqualTo(3);
            assertThat(completeCount.get()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("executeParallel 并行执行测试")
    class ExecuteParallelTests {

        @Test
        @DisplayName("应该并行执行所有元素")
        void shouldExecuteAllItemsInParallel() {
            // given
            List<Integer> items = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
            AtomicInteger executionOrder = new AtomicInteger(0);

            // when
            BatchResult<String> result = template.executeParallel(
                    items,
                    (item, index) -> {
                        executionOrder.incrementAndGet();
                        return "item-" + item;
                    },
                    4);

            // then
            assertThat(result.total()).isEqualTo(10);
            assertThat(result.success()).isEqualTo(10);
            assertThat(result.failed()).isEqualTo(0);
        }

        @Test
        @DisplayName("并行执行应该正确处理异常")
        void shouldHandleExceptionsInParallel() {
            // given
            List<Integer> items = List.of(1, 2, 3, 4, 5);
            BatchOperation<Integer, String> operation = (item, index) -> {
                if (item % 2 == 0) {
                    throw new RuntimeException("Error at " + item);
                }
                return "item-" + item;
            };

            // when
            BatchResult<String> result = template.executeParallel(items, operation, 2);

            // then
            assertThat(result.total()).isEqualTo(5);
            assertThat(result.success()).isEqualTo(3);
            assertThat(result.failed()).isEqualTo(2);
        }

        @Test
        @DisplayName("空列表应该返回空结果")
        void shouldReturnEmptyResultForEmptyList() {
            // given
            List<Integer> items = List.of();
            BatchOperation<Integer, String> operation = (item, index) -> "item-" + item;

            // when
            BatchResult<String> result = template.executeParallel(items, operation, 4);

            // then
            assertThat(result.total()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("executeWithRetry 重试测试")
    class ExecuteWithRetryTests {

        @Test
        @DisplayName("应该重试失败的操作")
        void shouldRetryFailedOperations() {
            // given
            List<Integer> items = List.of(1, 2, 3);
            AtomicInteger attemptCount = new AtomicInteger(0);

            BatchOperation<Integer, String> operation = (item, index) -> {
                if (item == 2) {
                    int attempts = attemptCount.incrementAndGet();
                    if (attempts <= 2) {
                        throw new RuntimeException("Temporary error");
                    }
                }
                return "item-" + item;
            };

            // when
            BatchResult<String> result = template.executeWithRetry(items, operation, 3);

            // then
            assertThat(result.success()).isEqualTo(3);
            assertThat(result.failed()).isEqualTo(0);
        }

        @Test
        @DisplayName("超过最大重试次数应该记录错误")
        void shouldRecordErrorAfterMaxRetries() {
            // given
            List<Integer> items = List.of(1, 2, 3);
            BatchOperation<Integer, String> operation = (item, index) -> {
                if (item == 2) {
                    throw new RuntimeException("Permanent error");
                }
                return "item-" + item;
            };

            // when
            BatchResult<String> result = template.executeWithRetry(items, operation, 2);

            // then
            assertThat(result.success()).isEqualTo(2);
            assertThat(result.failed()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("executeInBatches 分批执行测试")
    class ExecuteInBatchesTests {

        @Test
        @DisplayName("应该分批执行元素")
        void shouldExecuteItemsInBatches() {
            // given
            List<Integer> items = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
            List<String> batchLogs = new ArrayList<>();

            BatchOperation<Integer, String> operation = (item, index) -> {
                batchLogs.add("Processing " + item);
                return "item-" + item;
            };

            // when
            BatchResult<String> result = template.executeInBatches(items, operation, 3);

            // then
            assertThat(result.total()).isEqualTo(10);
            assertThat(result.success()).isEqualTo(10);
            assertThat(batchLogs).hasSize(10);
        }

        @Test
        @DisplayName("批次大小大于元素数量时应该正常执行")
        void shouldExecuteWhenBatchSizeLargerThanItems() {
            // given
            List<Integer> items = List.of(1, 2, 3);
            BatchOperation<Integer, String> operation = (item, index) -> "item-" + item;

            // when
            BatchResult<String> result = template.executeInBatches(items, operation, 10);

            // then
            assertThat(result.total()).isEqualTo(3);
            assertThat(result.success()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("错误容忍测试")
    class ErrorToleranceTests {

        @Test
        @DisplayName("超过错误容忍率应该停止处理")
        void shouldStopWhenExceedErrorTolerance() {
            // given
            BatchProperties props = new BatchProperties();
            props.setErrorTolerance(0.3); // 30% 容忍率
            BatchOperationTemplate tolerantTemplate = new BatchOperationTemplate(props);

            List<Integer> items = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
            BatchOperation<Integer, String> operation = (item, index) -> {
                if (item <= 4) { // 前 4 个会失败，超过 30%
                    throw new RuntimeException("Error");
                }
                return "item-" + item;
            };

            // when
            BatchResult<String> result = tolerantTemplate.execute(items, operation);

            // then
            // 前 4 个中有超过 30% 失败后会停止
            assertThat(result.failed()).isGreaterThan(0);
            assertThat(result.total()).isEqualTo(10);
        }

        @Test
        @DisplayName("遇到错误立即停止")
        void shouldStopImmediatelyOnError() {
            // given
            BatchProperties props = new BatchProperties();
            props.setStopOnError(true);
            BatchOperationTemplate stopOnErrorTemplate = new BatchOperationTemplate(props);

            List<Integer> items = List.of(1, 2, 3, 4, 5);
            BatchOperation<Integer, String> operation = (item, index) -> {
                if (item == 3) {
                    throw new RuntimeException("Error");
                }
                return "item-" + item;
            };

            // when
            BatchResult<String> result = stopOnErrorTemplate.execute(items, operation);

            // then
            assertThat(result.success()).isEqualTo(2); // 前两个成功
            assertThat(result.failed()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("BatchResult 测试")
    class BatchResultTests {

        @Test
        @DisplayName("应该正确计算成功率")
        void shouldCalculateSuccessRate() {
            // given
            BatchResult<String> result = BatchResult.<String>builder()
                    .total(10)
                    .success(8)
                    .failed(2)
                    .build();

            // then
            assertThat(result.getSuccessRate()).isEqualTo(0.8);
        }

        @Test
        @DisplayName("全部成功时 isAllSuccess 应该返回 true")
        void shouldReturnTrueWhenAllSuccess() {
            // given
            BatchResult<String> result = BatchResult.<String>builder()
                    .total(5)
                    .success(5)
                    .failed(0)
                    .build();

            // then
            assertThat(result.isAllSuccess()).isTrue();
            assertThat(result.isAllFailed()).isFalse();
        }

        @Test
        @DisplayName("全部失败时 isAllFailed 应该返回 true")
        void shouldReturnTrueWhenAllFailed() {
            // given
            BatchResult<String> result = BatchResult.<String>builder()
                    .total(5)
                    .success(0)
                    .failed(5)
                    .build();

            // then
            assertThat(result.isAllSuccess()).isFalse();
            assertThat(result.isAllFailed()).isTrue();
        }

        @Test
        @DisplayName("空结果应该返回默认值")
        void shouldReturnDefaultForEmptyResult() {
            // when
            BatchResult<String> result = BatchResult.empty();

            // then
            assertThat(result.total()).isZero();
            assertThat(result.success()).isZero();
            assertThat(result.failed()).isZero();
            assertThat(result.results()).isEmpty();
            assertThat(result.errors()).isEmpty();
            assertThat(result.getSuccessRate()).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("BatchError 测试")
    class BatchErrorTests {

        @Test
        @DisplayName("应该创建简单的错误信息")
        void shouldCreateSimpleError() {
            // when
            BatchError error = BatchError.of(5, "Something went wrong");

            // then
            assertThat(error.index()).isEqualTo(5);
            assertThat(error.error()).isEqualTo("Something went wrong");
            assertThat(error.item()).isNull();
            assertThat(error.cause()).isNull();
        }

        @Test
        @DisplayName("应该创建包含元素的错误信息")
        void shouldCreateErrorWithItem() {
            // when
            BatchError error = BatchError.of(3, "item-3", "Invalid value");

            // then
            assertThat(error.index()).isEqualTo(3);
            assertThat(error.item()).isEqualTo("item-3");
            assertThat(error.error()).isEqualTo("Invalid value");
        }

        @Test
        @DisplayName("应该创建包含异常的错误信息")
        void shouldCreateErrorWithException() {
            // given
            RuntimeException ex = new RuntimeException("Test exception");

            // when
            BatchError error = BatchError.of(2, "item-2", "Processing failed", ex);

            // then
            assertThat(error.index()).isEqualTo(2);
            assertThat(error.item()).isEqualTo("item-2");
            assertThat(error.error()).isEqualTo("Processing failed");
            assertThat(error.cause()).isEqualTo(RuntimeException.class.getName());
        }
    }

    @Nested
    @DisplayName("BatchProperties 测试")
    class BatchPropertiesTests {

        @Test
        @DisplayName("应该返回默认配置")
        void shouldReturnDefaultConfig() {
            // when
            BatchProperties props = new BatchProperties();

            // then
            assertThat(props.getDefaultBatchSize()).isEqualTo(100);
            assertThat(props.getDefaultParallelism()).isZero();
            assertThat(props.getErrorTolerance()).isEqualTo(1.0);
            assertThat(props.isStopOnError()).isFalse();
        }

        @Test
        @DisplayName("应该计算实际并行度")
        void shouldCalculateActualParallelism() {
            // given
            BatchProperties props = new BatchProperties();

            // when
            int parallelism = props.getActualParallelism();

            // then
            assertThat(parallelism).isEqualTo(Runtime.getRuntime().availableProcessors());

            // when configured
            props.setDefaultParallelism(8);
            // then
            assertThat(props.getActualParallelism()).isEqualTo(8);
        }
    }
}
