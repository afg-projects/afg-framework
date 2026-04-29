package io.github.afgprojects.framework.core.lock.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * LockException 测试
 */
@DisplayName("LockException 测试")
class LockExceptionTest {

    @Test
    @DisplayName("应该正确创建带消息的异常")
    void shouldCreateExceptionWithMessage() {
        // given
        String lockKey = "test-lock";
        String message = "Lock error";

        // when
        LockException exception = new LockException(lockKey, message);

        // then
        assertThat(exception.getLockKey()).isEqualTo(lockKey);
        assertThat(exception.getMessage()).isEqualTo(message);
    }

    @Test
    @DisplayName("应该正确创建带原因的异常")
    void shouldCreateExceptionWithCause() {
        // given
        String lockKey = "test-lock";
        String message = "Lock error";
        Throwable cause = new RuntimeException("Root cause");

        // when
        LockException exception = new LockException(lockKey, message, cause);

        // then
        assertThat(exception.getLockKey()).isEqualTo(lockKey);
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
    }
}
