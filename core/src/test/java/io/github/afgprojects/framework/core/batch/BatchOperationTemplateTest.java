package io.github.afgprojects.framework.core.batch;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.config.AfgCoreProperties;

/**
 * {@link BatchOperationTemplate} 单元测试。
 * <p>
 * 测试批量操作模板的顺序执行、并行执行、重试机制、分批执行以及错误容忍等功能。
 *
 * @see BatchOperationTemplate
 * @see BatchResult
 * @see BatchError
 * @see AfgCoreProperties.BatchConfig
 */
@DisplayName("BatchOperationTemplate 测试")
class BatchOperationTemplateTest {

    private BatchOperationTemplate template;

    @BeforeEach
    void setUp() {
        template = new BatchOperationTemplate();
    }

    /**
     * execute 顺序执行测试。
     * <p>
     * 验证顺序执行批量操作的基本功能，包括正常执行、异常处理和进度回调。
     */
    @Nested
    @DisplayName("execute 顺序执行测试")
    class ExecuteTests {

        /**
         * 测试顺序执行所有元素并返回正确结果。
         */
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

        /**
         * 测试空列表执行返回空结果。
         */
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

        /**
         * 测试执行过程中异常被正确捕获并记录到错误列表。
         */
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

        /**
         * 测试进度回调在执行过程中被正确触发。
         */
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

    /**
     * executeParallel 并行执行测试。
     * <p>
     * 验证并行执行批量操作的功能，包括并行度控制和异常处理。
     */
    @Nested
    @DisplayName("executeParallel 并行执行测试")
    class ExecuteParallelTests {

        /**
         * 测试并行执行所有元素并返回正确结果。
         */
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

        /**
         * 测试并行执行时异常被正确捕获并记录。
         */
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

        /**
         * 测试并行执行空列表返回空结果。
         */
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

    /**
     * executeWithRetry 重试测试。
     * <p>
     * 验证失败操作的重试机制，包括重试成功和超过最大重试次数的情况。
     */
    @Nested
    @DisplayName("executeWithRetry 重试测试")
    class ExecuteWithRetryTests {

        /**
         * 测试失败的操作在重试后成功。
         */
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

        /**
         * 测试超过最大重试次数后记录错误。
         */
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

    /**
     * executeInBatches 分批执行测试。
     * <p>
     * 验证分批执行功能，包括正常分批和批次大小大于元素数量的情况。
     */
    @Nested
    @DisplayName("executeInBatches 分批执行测试")
    class ExecuteInBatchesTests {

        /**
         * 测试按指定批次大小分批执行元素。
         */
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

        /**
         * 测试批次大小大于元素数量时正常执行。
         */
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

    /**
     * 错误容忍测试。
     * <p>
     * 验证错误容忍率和遇到错误立即停止的功能。
     */
    @Nested
    @DisplayName("错误容忍测试")
    class ErrorToleranceTests {

        /**
         * 测试超过错误容忍率时停止处理。
         */
        @Test
        @DisplayName("超过错误容忍率应该停止处理")
        void shouldStopWhenExceedErrorTolerance() {
            // given
            AfgCoreProperties props = new AfgCoreProperties();
            props.getBatch().setErrorTolerance(0.3); // 30% 容忍率
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

        /**
         * 测试配置 stopOnError 时遇到错误立即停止。
         */
        @Test
        @DisplayName("遇到错误立即停止")
        void shouldStopImmediatelyOnError() {
            // given
            AfgCoreProperties props = new AfgCoreProperties();
            props.getBatch().setStopOnError(true);
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

    /**
     * BatchResult 测试。
     * <p>
     * 验证批量操作结果的成功率计算和状态判断。
     */
    @Nested
    @DisplayName("BatchResult 测试")
    class BatchResultTests {

        /**
         * 测试成功率的正确计算。
         */
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

        /**
         * 测试全部成功时 isAllSuccess 返回 true。
         */
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

        /**
         * 测试全部失败时 isAllFailed 返回 true。
         */
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

        /**
         * 测试空结果返回默认值。
         */
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

    /**
     * BatchError 测试。
     * <p>
     * 验证批量操作错误记录的创建。
     */
    @Nested
    @DisplayName("BatchError 测试")
    class BatchErrorTests {

        /**
         * 测试创建简单的错误信息。
         */
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

        /**
         * 测试创建包含元素的错误信息。
         */
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

        /**
         * 测试创建包含异常的错误信息。
         */
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

    /**
     * BatchProperties 测试。
     * <p>
     * 验证批量操作配置属性的默认值和并行度计算。
     */
    @Nested
    @DisplayName("BatchProperties 测试")
    class BatchPropertiesTests {

        /**
         * 测试返回默认配置值。
         */
        @Test
        @DisplayName("应该返回默认配置")
        void shouldReturnDefaultConfig() {
            // when
            AfgCoreProperties.BatchConfig props = new AfgCoreProperties.BatchConfig();

            // then
            assertThat(props.getDefaultBatchSize()).isEqualTo(100);
            assertThat(props.getDefaultParallelism()).isZero();
            assertThat(props.getErrorTolerance()).isEqualTo(1.0);
            assertThat(props.isStopOnError()).isFalse();
        }

        /**
         * 测试实际并行度的计算，未配置时使用 CPU 核心数。
         */
        @Test
        @DisplayName("应该计算实际并行度")
        void shouldCalculateActualParallelism() {
            // given
            AfgCoreProperties.BatchConfig props = new AfgCoreProperties.BatchConfig();

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
