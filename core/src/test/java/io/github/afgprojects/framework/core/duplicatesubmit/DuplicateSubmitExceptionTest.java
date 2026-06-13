package io.github.afgprojects.framework.core.duplicatesubmit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.afgprojects.framework.commons.exception.CommonErrorCode;
import io.github.afgprojects.framework.core.duplicatesubmit.exception.DuplicateSubmitException;

@DisplayName("DuplicateSubmitException")
class DuplicateSubmitExceptionTest {

    @Nested
    @DisplayName("construction with submit key")
    class ConstructionWithSubmitKey {

        @Test
        @DisplayName("should create exception with submit key and default message")
        void shouldCreateExceptionWithSubmitKeyAndDefaultMessage() {
            DuplicateSubmitException exception = new DuplicateSubmitException("order:submit:123");

            assertThat(exception.getSubmitKey()).isEqualTo("order:submit:123");
            assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.DUPLICATE_SUBMIT);
            assertThat(exception.getCode()).isEqualTo(CommonErrorCode.DUPLICATE_SUBMIT.getCode());
        }
    }

    @Nested
    @DisplayName("construction with submit key and custom message")
    class ConstructionWithSubmitKeyAndCustomMessage {

        @Test
        @DisplayName("should create exception with custom message")
        void shouldCreateExceptionWithCustomMessage() {
            DuplicateSubmitException exception = new DuplicateSubmitException("order:submit:123", "订单正在处理中");

            assertThat(exception.getSubmitKey()).isEqualTo("order:submit:123");
            assertThat(exception.getMessage()).isEqualTo("订单正在处理中");
            assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.DUPLICATE_SUBMIT);
        }
    }

    @Nested
    @DisplayName("errorCode")
    class ErrorCode {

        @Test
        @DisplayName("should use DUPLICATE_SUBMIT error code")
        void shouldUseDuplicateSubmitErrorCode() {
            DuplicateSubmitException exception = new DuplicateSubmitException("key");

            assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.DUPLICATE_SUBMIT);
            assertThat(exception.getCode()).isEqualTo(10303);
        }
    }
}
