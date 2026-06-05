package io.github.afgprojects.framework.core.batch;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BatchError")
class BatchErrorTest {

    @Nested
    @DisplayName("of with index and error")
    class OfWithIndexAndError {

        @Test
        @DisplayName("should create BatchError with index and error message")
        void shouldCreateBatchErrorWithIndexAndErrorMessage() {
            BatchError error = BatchError.of(2, "something went wrong");

            assertThat(error.index()).isEqualTo(2);
            assertThat(error.item()).isNull();
            assertThat(error.error()).isEqualTo("something went wrong");
            assertThat(error.cause()).isNull();
        }
    }

    @Nested
    @DisplayName("of with index, item, and error")
    class OfWithIndexItemAndError {

        @Test
        @DisplayName("should create BatchError with item representation")
        void shouldCreateBatchErrorWithItemRepresentation() {
            BatchError error = BatchError.of(1, "item-1", "processing failed");

            assertThat(error.index()).isEqualTo(1);
            assertThat(error.item()).isEqualTo("item-1");
            assertThat(error.error()).isEqualTo("processing failed");
            assertThat(error.cause()).isNull();
        }

        @Test
        @DisplayName("should accept null item")
        void shouldAcceptNullItem() {
            BatchError error = BatchError.of(1, null, "error message");

            assertThat(error.item()).isNull();
            assertThat(error.error()).isEqualTo("error message");
        }
    }

    @Nested
    @DisplayName("of with exception")
    class OfWithException {

        @Test
        @DisplayName("should create BatchError from exception")
        void shouldCreateBatchErrorFromException() {
            RuntimeException exception = new RuntimeException("test error");
            BatchError error = BatchError.of(1, "item-1", "test error", exception);

            assertThat(error.index()).isEqualTo(1);
            assertThat(error.item()).isEqualTo("item-1");
            assertThat(error.error()).isEqualTo("test error");
            assertThat(error.cause()).isEqualTo("java.lang.RuntimeException");
        }

        @Test
        @DisplayName("should handle null exception")
        void shouldHandleNullException() {
            BatchError error = BatchError.of(1, "item-1", "error", null);

            assertThat(error.cause()).isNull();
        }
    }

    @Nested
    @DisplayName("equals and hashCode")
    class EqualsAndHashCode {

        @Test
        @DisplayName("should be equal when all fields match")
        void shouldBeEqual_whenAllFieldsMatch() {
            BatchError error1 = BatchError.of(1, "error");
            BatchError error2 = BatchError.of(1, "error");

            assertThat(error1).isEqualTo(error2);
            assertThat(error1.hashCode()).isEqualTo(error2.hashCode());
        }

        @Test
        @DisplayName("should not be equal when index differs")
        void shouldNotBeEqual_whenIndexDiffers() {
            BatchError error1 = BatchError.of(1, "error");
            BatchError error2 = BatchError.of(2, "error");

            assertThat(error1).isNotEqualTo(error2);
        }

        @Test
        @DisplayName("should not be equal when error message differs")
        void shouldNotBeEqual_whenErrorMessageDiffers() {
            BatchError error1 = BatchError.of(1, "error1");
            BatchError error2 = BatchError.of(1, "error2");

            assertThat(error1).isNotEqualTo(error2);
        }
    }
}