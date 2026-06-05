package io.github.afgprojects.framework.core.batch;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("BatchOperationTemplate")
class BatchOperationTemplateTest {

    private final BatchOperationTemplate template = new BatchOperationTemplate();

    @Nested
    @DisplayName("execute (sequential)")
    class Execute {

        @Test
        @DisplayName("should return empty result for empty list")
        void shouldReturnEmptyResult_forEmptyList() {
            BatchResult<String> result = template.execute(List.<String>of(), (item, index) -> item);

            assertThat(result.total()).isEqualTo(0);
            assertThat(result.success()).isEqualTo(0);
            assertThat(result.failed()).isEqualTo(0);
            assertThat(result.results()).isEmpty();
            assertThat(result.errors()).isEmpty();
        }

        @Test
        @DisplayName("should execute all items successfully")
        void shouldExecuteAllItemsSuccessfully() {
            List<Integer> items = List.of(1, 2, 3);

            BatchResult<String> result = template.execute(items, (item, index) -> "result-" + item);

            assertThat(result.total()).isEqualTo(3);
            assertThat(result.success()).isEqualTo(3);
            assertThat(result.failed()).isEqualTo(0);
            assertThat(result.results()).containsExactly("result-1", "result-2", "result-3");
            assertThat(result.isAllSuccess()).isTrue();
        }

        @Test
        @DisplayName("should pass correct index to operation")
        void shouldPassCorrectIndexToOperation() {
            List<String> items = List.of("a", "b", "c");
            List<Integer> indices = new ArrayList<>();

            template.execute(items, (item, index) -> {
                indices.add(index);
                return item;
            });

            assertThat(indices).containsExactly(0, 1, 2);
        }

        @Test
        @DisplayName("should handle exceptions and continue by default")
        void shouldHandleExceptions_andContinueByDefault() {
            List<Integer> items = List.of(1, 2, 3);

            BatchResult<String> result = template.execute(items, (item, index) -> {
                if (item == 2) {
                    throw new RuntimeException("error on 2");
                }
                return "result-" + item;
            });

            assertThat(result.total()).isEqualTo(3);
            assertThat(result.success()).isEqualTo(2);
            assertThat(result.failed()).isEqualTo(1);
            assertThat(result.results()).containsExactly("result-1", "result-3");
            assertThat(result.errors()).hasSize(1);
            assertThat(result.errors().get(0).index()).isEqualTo(1);
        }

        @Test
        @DisplayName("should record duration")
        void shouldRecordDuration() {
            BatchResult<String> result = template.execute(List.of("a"), (item, index) -> item);

            assertThat(result.duration()).isNotNull();
            assertThat(result.duration()).isGreaterThanOrEqualTo(java.time.Duration.ZERO);
        }
    }

    @Nested
    @DisplayName("executeParallel")
    class ExecuteParallel {

        @Test
        @DisplayName("should return empty result for empty list")
        void shouldReturnEmptyResult_forEmptyList() {
            BatchResult<String> result = template.executeParallel(List.<String>of(), (item, index) -> item, 2);

            assertThat(result.total()).isEqualTo(0);
            assertThat(result.results()).isEmpty();
        }

        @Test
        @DisplayName("should execute all items in parallel")
        void shouldExecuteAllItemsInParallel() {
            List<Integer> items = List.of(1, 2, 3, 4, 5);

            BatchResult<String> result = template.executeParallel(items, (item, index) -> "result-" + item, 2);

            assertThat(result.total()).isEqualTo(5);
            assertThat(result.success()).isEqualTo(5);
            assertThat(result.failed()).isEqualTo(0);
        }

        @Test
        @DisplayName("should handle exceptions in parallel execution")
        void shouldHandleExceptions_inParallelExecution() {
            List<Integer> items = List.of(1, 2, 3);

            BatchResult<String> result = template.executeParallel(items, (item, index) -> {
                if (item == 2) {
                    throw new RuntimeException("error on 2");
                }
                return "result-" + item;
            }, 2);

            assertThat(result.total()).isEqualTo(3);
            assertThat(result.success()).isEqualTo(2);
            assertThat(result.failed()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("executeWithRetry")
    class ExecuteWithRetry {

        @Test
        @DisplayName("should return empty result for empty list")
        void shouldReturnEmptyResult_forEmptyList() {
            BatchResult<String> result = template.executeWithRetry(List.<String>of(), (item, index) -> item, 3);

            assertThat(result.total()).isEqualTo(0);
        }

        @Test
        @DisplayName("should succeed on first attempt")
        void shouldSucceedOnFirstAttempt() {
            List<Integer> items = List.of(1, 2, 3);

            BatchResult<String> result = template.executeWithRetry(items, (item, index) -> "result-" + item, 3);

            assertThat(result.isAllSuccess()).isTrue();
            assertThat(result.results()).containsExactly("result-1", "result-2", "result-3");
        }

        @Test
        @DisplayName("should retry and succeed after failures")
        void shouldRetryAndSucceed_afterFailures() {
            AtomicInteger attemptCount = new AtomicInteger(0);

            BatchResult<String> result = template.executeWithRetry(List.of("item"), (item, index) -> {
                int attempt = attemptCount.incrementAndGet();
                if (attempt < 3) {
                    throw new RuntimeException("attempt " + attempt + " failed");
                }
                return "success";
            }, 3);

            assertThat(result.isAllSuccess()).isTrue();
            assertThat(result.results()).containsExactly("success");
            assertThat(attemptCount.get()).isEqualTo(3);
        }

        @Test
        @DisplayName("should fail after max attempts exceeded")
        void shouldFailAfterMaxAttemptsExceeded() {
            BatchResult<String> result = template.executeWithRetry(List.of("item"), (item, index) -> {
                throw new RuntimeException("always fails");
            }, 2);

            assertThat(result.isAllFailed()).isTrue();
            assertThat(result.errors()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("executeInBatches")
    class ExecuteInBatches {

        @Test
        @DisplayName("should return empty result for empty list")
        void shouldReturnEmptyResult_forEmptyList() {
            BatchResult<String> result = template.executeInBatches(List.<String>of(), (item, index) -> item, 2);

            assertThat(result.total()).isEqualTo(0);
        }

        @Test
        @DisplayName("should process items in batches")
        void shouldProcessItemsInBatches() {
            List<Integer> items = List.of(1, 2, 3, 4, 5);

            BatchResult<String> result = template.executeInBatches(items, (item, index) -> "result-" + item, 2);

            assertThat(result.total()).isEqualTo(5);
            assertThat(result.success()).isEqualTo(5);
            assertThat(result.results()).containsExactly("result-1", "result-2", "result-3", "result-4", "result-5");
        }

        @Test
        @DisplayName("should handle errors in batch execution")
        void shouldHandleErrorsInBatchExecution() {
            List<Integer> items = List.of(1, 2, 3, 4, 5);

            BatchResult<String> result = template.executeInBatches(items, (item, index) -> {
                if (item == 3) {
                    throw new RuntimeException("error on 3");
                }
                return "result-" + item;
            }, 2);

            assertThat(result.total()).isEqualTo(5);
            assertThat(result.success()).isEqualTo(4);
            assertThat(result.failed()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("progress callback")
    class ProgressCallback {

        @Test
        @DisplayName("should notify item completion")
        void shouldNotifyItemCompletion() {
            List<String> completedItems = new ArrayList<>();
            List<Boolean> successFlags = new ArrayList<>();

            template.execute(List.of("a", "b"), (item, index) -> item, new BatchProgressCallback<>() {
                @Override
                public void onItemComplete(String item, int index, String result, boolean success) {
                    completedItems.add(item);
                    successFlags.add(success);
                }

                @Override
                public void onBatchProgress(int completed, int total) {}

                @Override
                public void onComplete(BatchResult<String> result) {}
            });

            assertThat(completedItems).containsExactly("a", "b");
            assertThat(successFlags).containsExactly(true, true);
        }

        @Test
        @DisplayName("should notify batch progress")
        void shouldNotifyBatchProgress() {
            List<String> progressUpdates = new ArrayList<>();

            template.execute(List.of("a", "b", "c"), (item, index) -> item, new BatchProgressCallback<>() {
                @Override
                public void onItemComplete(String item, int index, String result, boolean success) {}

                @Override
                public void onBatchProgress(int completed, int total) {
                    progressUpdates.add(completed + "/" + total);
                }

                @Override
                public void onComplete(BatchResult<String> result) {}
            });

            assertThat(progressUpdates).containsExactly("1/3", "2/3", "3/3");
        }

        @Test
        @DisplayName("should notify on complete")
        void shouldNotifyOnComplete() {
            List<BatchResult<String>> completedResults = new ArrayList<>();

            template.execute(List.of("a"), (item, index) -> item, new BatchProgressCallback<>() {
                @Override
                public void onItemComplete(String item, int index, String result, boolean success) {}

                @Override
                public void onBatchProgress(int completed, int total) {}

                @Override
                public void onComplete(BatchResult<String> result) {
                    completedResults.add(result);
                }
            });

            assertThat(completedResults).hasSize(1);
            assertThat(completedResults.get(0).isAllSuccess()).isTrue();
        }
    }
}
