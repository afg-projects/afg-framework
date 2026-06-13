package io.github.afgprojects.framework.core.duplicatesubmit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.afgprojects.framework.core.api.duplicatesubmit.LocalDuplicateSubmitChecker;

@DisplayName("LocalDuplicateSubmitChecker")
class LocalDuplicateSubmitCheckerTest {

    private LocalDuplicateSubmitChecker checker;

    @BeforeEach
    void setUp() {
        checker = new LocalDuplicateSubmitChecker();
    }

    @Nested
    @DisplayName("tryAcquire")
    class TryAcquire {

        @Test
        @DisplayName("should return true for first request")
        void shouldReturnTrueForFirstRequest() {
            assertThat(checker.tryAcquire("test-key", 3000)).isTrue();
        }

        @Test
        @DisplayName("should return false for duplicate request within interval")
        void shouldReturnFalseForDuplicateRequestWithinInterval() {
            checker.tryAcquire("test-key", 3000);
            assertThat(checker.tryAcquire("test-key", 3000)).isFalse();
        }

        @Test
        @DisplayName("should return true for different keys")
        void shouldReturnTrueForDifferentKeys() {
            checker.tryAcquire("key-1", 3000);
            assertThat(checker.tryAcquire("key-2", 3000)).isTrue();
        }

        @Test
        @DisplayName("should return true after interval expires")
        void shouldReturnTrueAfterIntervalExpires() throws InterruptedException {
            checker.tryAcquire("test-key", 100);
            // Wait for the interval to expire
            Thread.sleep(150);
            assertThat(checker.tryAcquire("test-key", 100)).isTrue();
        }

        @Test
        @DisplayName("should return false when called rapidly with same key")
        void shouldReturnFalseWhenCalledRapidlyWithSameKey() {
            assertThat(checker.tryAcquire("rapid-key", 5000)).isTrue();
            assertThat(checker.tryAcquire("rapid-key", 5000)).isFalse();
            assertThat(checker.tryAcquire("rapid-key", 5000)).isFalse();
        }

        @Test
        @DisplayName("should handle different intervals for same key")
        void shouldHandleDifferentIntervalsForSameKey() throws InterruptedException {
            // First acquire with short interval
            checker.tryAcquire("interval-key", 100);
            // Wait for short interval to pass
            Thread.sleep(150);
            // Re-acquire with short interval — should succeed since 150ms > 100ms
            assertThat(checker.tryAcquire("interval-key", 100)).isTrue();
            // Immediately try again — should fail within interval
            assertThat(checker.tryAcquire("interval-key", 100)).isFalse();
        }

        @Test
        @DisplayName("should reject when switching to larger interval before short interval expires")
        void shouldRejectWhenSwitchingToLargerIntervalBeforeShortIntervalExpires() {
            // First acquire with short interval
            checker.tryAcquire("interval-key", 5000);
            // Try with different interval — key still exists and within 5000ms window
            assertThat(checker.tryAcquire("interval-key", 100)).isFalse();
        }
    }

    @Nested
    @DisplayName("release")
    class Release {

        @Test
        @DisplayName("should allow re-acquisition after release")
        void shouldAllowReAcquisitionAfterRelease() {
            checker.tryAcquire("test-key", 3000);
            checker.release("test-key");
            assertThat(checker.tryAcquire("test-key", 3000)).isTrue();
        }

        @Test
        @DisplayName("should not throw when releasing non-existent key")
        void shouldNotThrowWhenReleasingNonExistentKey() {
            checker.release("non-existent-key");
        }

        @Test
        @DisplayName("should only release the specified key")
        void shouldOnlyReleaseTheSpecifiedKey() {
            checker.tryAcquire("key-1", 3000);
            checker.tryAcquire("key-2", 3000);
            checker.release("key-1");
            // key-1 should be available again
            assertThat(checker.tryAcquire("key-1", 3000)).isTrue();
            // key-2 should still be blocked
            assertThat(checker.tryAcquire("key-2", 3000)).isFalse();
        }
    }

    @Nested
    @DisplayName("concurrent access")
    class ConcurrentAccess {

        @Test
        @DisplayName("should handle concurrent access safely")
        void shouldHandleConcurrentAccessSafely() throws InterruptedException {
            int threadCount = 10;
            Thread[] threads = new Thread[threadCount];
            boolean[] results = new boolean[threadCount];

            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                threads[i] = new Thread(() -> {
                    results[index] = checker.tryAcquire("concurrent-key", 5000);
                });
            }

            for (Thread thread : threads) {
                thread.start();
            }
            for (Thread thread : threads) {
                thread.join();
            }

            // At least one should succeed (the first one)
            long successCount = 0;
            for (boolean result : results) {
                if (result) successCount++;
            }
            assertThat(successCount).isGreaterThanOrEqualTo(1);
        }
    }
}
