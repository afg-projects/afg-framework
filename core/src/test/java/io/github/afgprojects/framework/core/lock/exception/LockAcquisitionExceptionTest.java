package io.github.afgprojects.framework.core.lock.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * LockAcquisitionException 测试
 */
@DisplayName("LockAcquisitionException 测试")
class LockAcquisitionExceptionTest {

    @Test
    @DisplayName("应该正确创建仅带锁键的异常")
    void shouldCreateExceptionWithLockKeyOnly() {
        // given
        String lockKey = "test-lock";

        // when
        LockAcquisitionException exception = new LockAcquisitionException(lockKey);

        // then
        assertThat(exception.getLockKey()).isEqualTo(lockKey);
        assertThat(exception.getMessage()).contains(lockKey);
    }

    @Test
    @DisplayName("应该正确创建带自定义消息的异常")
    void shouldCreateExceptionWithCustomMessage() {
        // given
        String lockKey = "test-lock";
        String message = "Custom error message";

        // when
        LockAcquisitionException exception = new LockAcquisitionException(lockKey, message);

        // then
        assertThat(exception.getLockKey()).isEqualTo(lockKey);
        assertThat(exception.getMessage()).isEqualTo(message);
    }

    @Test
    @DisplayName("应该正确创建带原因的异常")
    void shouldCreateExceptionWithCause() {
        // given
        String lockKey = "test-lock";
        String message = "Lock acquisition failed";
        Throwable cause = new RuntimeException("Redis connection error");

        // when
        LockAcquisitionException exception = new LockAcquisitionException(lockKey, message, cause);

        // then
        assertThat(exception.getLockKey()).isEqualTo(lockKey);
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
    }
}
