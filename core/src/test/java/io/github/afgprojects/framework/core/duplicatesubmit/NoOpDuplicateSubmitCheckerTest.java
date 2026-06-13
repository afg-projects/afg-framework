package io.github.afgprojects.framework.core.duplicatesubmit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.afgprojects.framework.core.api.duplicatesubmit.NoOpDuplicateSubmitChecker;

@DisplayName("NoOpDuplicateSubmitChecker")
class NoOpDuplicateSubmitCheckerTest {

    private final NoOpDuplicateSubmitChecker checker = new NoOpDuplicateSubmitChecker();

    @Nested
    @DisplayName("tryAcquire")
    class TryAcquire {

        @Test
        @DisplayName("should always return true for any key")
        void shouldAlwaysReturnTrueForAnyKey() {
            assertThat(checker.tryAcquire("any-key", 3000)).isTrue();
        }

        @Test
        @DisplayName("should return true for same key on second call")
        void shouldReturnTrueForSameKeyOnSecondCall() {
            checker.tryAcquire("same-key", 3000);
            assertThat(checker.tryAcquire("same-key", 3000)).isTrue();
        }

        @Test
        @DisplayName("should return true for zero interval")
        void shouldReturnTrueForZeroInterval() {
            assertThat(checker.tryAcquire("key", 0)).isTrue();
        }
    }

    @Nested
    @DisplayName("release")
    class Release {

        @Test
        @DisplayName("should not throw on release after acquire")
        void shouldNotThrowOnReleaseAfterAcquire() {
            checker.tryAcquire("key", 3000);
            checker.release("key");
            assertThat(checker.tryAcquire("key", 3000)).isTrue();
        }

        @Test
        @DisplayName("should not throw on release for non-existent key")
        void shouldNotThrowOnReleaseForNonExistentKey() {
            checker.release("non-existent-key");
        }
    }
}
