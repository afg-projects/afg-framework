package io.github.afgprojects.framework.security.core.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NoOpLoginFailureTracker 测试
 */
@DisplayName("NoOpLoginFailureTracker 测试")
class NoOpLoginFailureTrackerTest {

    private NoOpLoginFailureTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = new NoOpLoginFailureTracker();
    }

    @Test
    @DisplayName("recordFailure 应不抛异常")
    void shouldNotThrowOnRecordFailure() {
        tracker.recordFailure("user1", "tenant1", "127.0.0.1");
    }

    @Test
    @DisplayName("getFailureCount 应返回 0")
    void shouldReturnZeroFailureCount() {
        tracker.recordFailure("user1", "tenant1", "127.0.0.1");

        assertThat(tracker.getFailureCount("user1", "tenant1")).isZero();
    }

    @Test
    @DisplayName("isLocked 应返回 false")
    void shouldReturnFalseOnLocked() {
        assertThat(tracker.isLocked("user1", "tenant1")).isFalse();
    }

    @Test
    @DisplayName("getLockedUntil 应返回 null")
    void shouldReturnNullLockedUntil() {
        assertThat(tracker.getLockedUntil("user1", "tenant1")).isNull();
    }

    @Test
    @DisplayName("unlock 应不抛异常")
    void shouldNotThrowOnUnlock() {
        tracker.unlock("user1", "tenant1");
    }

    @Test
    @DisplayName("reset 应不抛异常")
    void shouldNotThrowOnReset() {
        tracker.reset("user1", "tenant1");
    }
}
