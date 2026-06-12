package io.github.afgprojects.framework.security.core.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NoOpLoginFailureStorage 测试
 */
@DisplayName("NoOpLoginFailureStorage 测试")
class NoOpLoginFailureStorageTest {

    private NoOpLoginFailureStorage storage;

    @BeforeEach
    void setUp() {
        storage = new NoOpLoginFailureStorage();
    }

    @Test
    @DisplayName("recordFailure 应不抛异常")
    void shouldNotThrowOnRecordFailure() {
        storage.recordFailure("user1", "admin", "tenant1", "127.0.0.1");
    }

    @Test
    @DisplayName("getFailureCount 应返回 0")
    void shouldReturnZeroFailureCount() {
        storage.recordFailure("user1", "admin", "tenant1", "127.0.0.1");

        assertThat(storage.getFailureCount("user1")).isZero();
    }

    @Test
    @DisplayName("isLocked 应返回 false")
    void shouldReturnFalseOnLocked() {
        assertThat(storage.isLocked("user1")).isFalse();
    }

    @Test
    @DisplayName("getLockedUntil 应返回 null")
    void shouldReturnNullLockedUntil() {
        assertThat(storage.getLockedUntil("user1")).isNull();
    }

    @Test
    @DisplayName("unlock 应不抛异常")
    void shouldNotThrowOnUnlock() {
        storage.unlock("user1");
    }

    @Test
    @DisplayName("reset 应不抛异常")
    void shouldNotThrowOnReset() {
        storage.reset("user1");
    }
}
