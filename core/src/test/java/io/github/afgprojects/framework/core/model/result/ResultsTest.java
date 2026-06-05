package io.github.afgprojects.framework.core.model.result;

import io.github.afgprojects.framework.commons.exception.CommonErrorCode;
import io.github.afgprojects.framework.commons.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Results")
class ResultsTest {

    @Nested
    @DisplayName("success")
    class Success {

        @Test
        @DisplayName("should create success result with data")
        void shouldCreateSuccessResultWithData() {
            var result = Results.success("hello");

            assertThat(result.code()).isEqualTo(0);
            assertThat(result.data()).isEqualTo("hello");
            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("should create success result with null data")
        void shouldCreateSuccessResultWithNullData() {
            var result = Results.success(null);

            assertThat(result.code()).isEqualTo(0);
            assertThat(result.data()).isNull();
            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("should create success result with message and data")
        void shouldCreateSuccessResultWithMessageAndData() {
            var result = Results.success("Operation completed", "hello");

            assertThat(result.code()).isEqualTo(0);
            assertThat(result.message()).isEqualTo("Operation completed");
            assertThat(result.data()).isEqualTo("hello");
            assertThat(result.isSuccess()).isTrue();
        }
    }

    @Nested
    @DisplayName("fail")
    class Fail {

        @Test
        @DisplayName("should create fail result from ErrorCode")
        void shouldCreateFailResultFromErrorCode() {
            var result = Results.fail(CommonErrorCode.FAIL);

            assertThat(result.code()).isEqualTo(CommonErrorCode.FAIL.getCode());
            assertThat(result.message()).isEqualTo(CommonErrorCode.FAIL.getMessage());
            assertThat(result.data()).isNull();
            assertThat(result.isSuccess()).isFalse();
        }

        @Test
        @DisplayName("should create fail result with custom ErrorCode")
        void shouldCreateFailResultWithCustomErrorCode() {
            ErrorCode customError = new ErrorCode() {
                @Override
                public int getCode() {
                    return 409;
                }

                @Override
                public String getMessage() {
                    return "Conflict";
                }
            };

            var result = Results.fail(customError);

            assertThat(result.code()).isEqualTo(409);
            assertThat(result.message()).isEqualTo("Conflict");
            assertThat(result.isSuccess()).isFalse();
        }

        @Test
        @DisplayName("should create fail result from UNAUTHORIZED error code")
        void shouldCreateFailResultFromUnauthorizedErrorCode() {
            var result = Results.fail(CommonErrorCode.UNAUTHORIZED);

            assertThat(result.code()).isEqualTo(10400);
            assertThat(result.isSuccess()).isFalse();
        }
    }

    @Nested
    @DisplayName("page")
    class Page {

        @Test
        @DisplayName("should create paged result")
        void shouldCreatePagedResult() {
            var result = Results.page(java.util.List.of("a", "b"), 20, 1, 10);

            assertThat(result.code()).isEqualTo(0);
            assertThat(result.data().records()).containsExactly("a", "b");
            assertThat(result.data().total()).isEqualTo(20);
            assertThat(result.data().page()).isEqualTo(1);
            assertThat(result.data().size()).isEqualTo(10);
            assertThat(result.isSuccess()).isTrue();
        }
    }

    @Nested
    @DisplayName("Result record accessors")
    class ResultRecordAccessors {

        @Test
        @DisplayName("should have traceId and requestId (may be null without Spring context)")
        void shouldHaveTraceIdAndRequestId() {
            var result = Results.success("test");

            // traceId and requestId may be null in unit test context
            // Just verify the accessors exist and don't throw
            assertThat(result.traceId()).isNull();
            assertThat(result.requestId()).isNull();
        }
    }
}