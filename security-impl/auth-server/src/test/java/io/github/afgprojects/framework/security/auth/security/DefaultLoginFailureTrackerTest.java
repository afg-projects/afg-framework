package io.github.afgprojects.framework.security.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * DefaultLoginFailureTracker 测试
 */
@DisplayName("DefaultLoginFailureTracker 测试")
class DefaultLoginFailureTrackerTest {

    private DefaultLoginFailureTracker tracker;

    @BeforeEach
    void setUp() {
        // 默认配置：最大失败 5 次，锁定 30 分钟
        tracker = new DefaultLoginFailureTracker(5, Duration.ofMinutes(30));
    }

    @Nested
    @DisplayName("记录失败测试")
    class RecordFailureTests {

        @Test
        @DisplayName("首次记录失败 - 失败次数为 1")
        void shouldRecordFirstFailure() {
            // given
            String userId = "user-1";
            String tenantId = "tenant-1";
            String ip = "192.168.1.1";

            // when
            tracker.recordFailure(userId, tenantId, ip);

            // then
            assertThat(tracker.getFailureCount(userId, tenantId)).isEqualTo(1);
            assertThat(tracker.isLocked(userId, tenantId)).isFalse();
        }

        @Test
        @DisplayName("多次记录失败 - 失败次数累加")
        void shouldIncrementFailureCount() {
            // given
            String userId = "user-2";
            String tenantId = "tenant-1";
            String ip = "192.168.1.1";

            // when
            tracker.recordFailure(userId, tenantId, ip);
            tracker.recordFailure(userId, tenantId, ip);
            tracker.recordFailure(userId, tenantId, ip);

            // then
            assertThat(tracker.getFailureCount(userId, tenantId)).isEqualTo(3);
            assertThat(tracker.isLocked(userId, tenantId)).isFalse();
        }

        @Test
        @DisplayName("不同用户独立计数")
        void shouldTrackDifferentUsersIndependently() {
            // given
            String userId1 = "user-3";
            String userId2 = "user-4";
            String tenantId = "tenant-1";
            String ip = "192.168.1.1";

            // when
            tracker.recordFailure(userId1, tenantId, ip);
            tracker.recordFailure(userId1, tenantId, ip);
            tracker.recordFailure(userId2, tenantId, ip);

            // then
            assertThat(tracker.getFailureCount(userId1, tenantId)).isEqualTo(2);
            assertThat(tracker.getFailureCount(userId2, tenantId)).isEqualTo(1);
        }

        @Test
        @DisplayName("不同租户独立计数")
        void shouldTrackDifferentTenantsIndependently() {
            // given
            String userId = "user-5";
            String tenantId1 = "tenant-1";
            String tenantId2 = "tenant-2";
            String ip = "192.168.1.1";

            // when
            tracker.recordFailure(userId, tenantId1, ip);
            tracker.recordFailure(userId, tenantId1, ip);
            tracker.recordFailure(userId, tenantId2, ip);

            // then
            assertThat(tracker.getFailureCount(userId, tenantId1)).isEqualTo(2);
            assertThat(tracker.getFailureCount(userId, tenantId2)).isEqualTo(1);
        }

        @Test
        @DisplayName("单租户场景 - tenantId 为 null")
        void shouldHandleNullTenantId() {
            // given
            String userId = "user-6";
            String ip = "192.168.1.1";

            // when
            tracker.recordFailure(userId, null, ip);
            tracker.recordFailure(userId, null, ip);

            // then
            assertThat(tracker.getFailureCount(userId, null)).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("获取失败次数测试")
    class GetFailureCountTests {

        @Test
        @DisplayName("无记录返回 0")
        void shouldReturnZeroWhenNoRecord() {
            // given
            String userId = "user-no-record";
            String tenantId = "tenant-1";

            // when
            int count = tracker.getFailureCount(userId, tenantId);

            // then
            assertThat(count).isZero();
        }
    }

    @Nested
    @DisplayName("锁定逻辑测试")
    class LockTests {

        @Test
        @DisplayName("达到最大失败次数 - 自动锁定")
        void shouldLockWhenMaxFailuresReached() {
            // given
            String userId = "user-lock-1";
            String tenantId = "tenant-1";
            String ip = "192.168.1.1";

            // when - 记录 5 次失败
            for (int i = 0; i < 5; i++) {
                tracker.recordFailure(userId, tenantId, ip);
            }

            // then
            assertThat(tracker.isLocked(userId, tenantId)).isTrue();
            assertThat(tracker.getLockedUntil(userId, tenantId)).isNotNull();
        }

        @Test
        @DisplayName("未达到最大失败次数 - 不锁定")
        void shouldNotLockBeforeMaxFailures() {
            // given
            String userId = "user-lock-2";
            String tenantId = "tenant-1";
            String ip = "192.168.1.1";

            // when - 记录 4 次失败
            for (int i = 0; i < 4; i++) {
                tracker.recordFailure(userId, tenantId, ip);
            }

            // then
            assertThat(tracker.isLocked(userId, tenantId)).isFalse();
            assertThat(tracker.getLockedUntil(userId, tenantId)).isNull();
        }

        @Test
        @DisplayName("锁定后继续记录失败 - 失败次数继续增加")
        void shouldContinueIncrementAfterLock() {
            // given
            String userId = "user-lock-3";
            String tenantId = "tenant-1";
            String ip = "192.168.1.1";

            // when - 记录 5 次失败（锁定）
            for (int i = 0; i < 5; i++) {
                tracker.recordFailure(userId, tenantId, ip);
            }
            // 继续记录失败
            tracker.recordFailure(userId, tenantId, ip);

            // then
            assertThat(tracker.getFailureCount(userId, tenantId)).isEqualTo(6);
            assertThat(tracker.isLocked(userId, tenantId)).isTrue();
        }

        @Test
        @DisplayName("锁定时间检查 - 锁定截止时间在未来")
        void shouldSetLockedUntilInFuture() {
            // given
            String userId = "user-lock-4";
            String tenantId = "tenant-1";
            String ip = "192.168.1.1";
            LocalDateTime beforeLock = LocalDateTime.now();

            // when - 记录 5 次失败
            for (int i = 0; i < 5; i++) {
                tracker.recordFailure(userId, tenantId, ip);
            }

            // then
            LocalDateTime lockedUntil = tracker.getLockedUntil(userId, tenantId);
            assertThat(lockedUntil).isNotNull();
            assertThat(lockedUntil).isAfter(beforeLock);
        }

        @Test
        @DisplayName("锁定过期后 - 自动解锁")
        void shouldAutoUnlockAfterLockDuration() {
            // given - 使用很短的锁定时间进行测试
            tracker = new DefaultLoginFailureTracker(3, Duration.ofMillis(100));
            String userId = "user-lock-5";
            String tenantId = "tenant-1";
            String ip = "192.168.1.1";

            // when - 记录 3 次失败（锁定）
            for (int i = 0; i < 3; i++) {
                tracker.recordFailure(userId, tenantId, ip);
            }

            // then - 立即检查应该锁定
            assertThat(tracker.isLocked(userId, tenantId)).isTrue();

            // 等待锁定过期
            await().atMost(200, TimeUnit.MILLISECONDS)
                    .until(() -> !tracker.isLocked(userId, tenantId));

            // 验证已解锁
            assertThat(tracker.isLocked(userId, tenantId)).isFalse();
        }
    }

    @Nested
    @DisplayName("解锁测试")
    class UnlockTests {

        @Test
        @DisplayName("手动解锁 - 清除锁定状态")
        void shouldUnlockManually() {
            // given
            String userId = "user-unlock-1";
            String tenantId = "tenant-1";
            String ip = "192.168.1.1";

            // 记录 5 次失败（锁定）
            for (int i = 0; i < 5; i++) {
                tracker.recordFailure(userId, tenantId, ip);
            }
            assertThat(tracker.isLocked(userId, tenantId)).isTrue();

            // when
            tracker.unlock(userId, tenantId);

            // then
            assertThat(tracker.isLocked(userId, tenantId)).isFalse();
            assertThat(tracker.getLockedUntil(userId, tenantId)).isNull();
        }

        @Test
        @DisplayName("解锁未锁定的账户 - 无操作")
        void shouldDoNothingWhenUnlockingUnlockedAccount() {
            // given
            String userId = "user-unlock-2";
            String tenantId = "tenant-1";

            // when
            tracker.unlock(userId, tenantId);

            // then - 不应抛出异常
            assertThat(tracker.isLocked(userId, tenantId)).isFalse();
            assertThat(tracker.getFailureCount(userId, tenantId)).isZero();
        }

        @Test
        @DisplayName("解锁后失败次数保留")
        void shouldPreserveFailureCountAfterUnlock() {
            // given
            String userId = "user-unlock-3";
            String tenantId = "tenant-1";
            String ip = "192.168.1.1";

            // 记录 5 次失败（锁定）
            for (int i = 0; i < 5; i++) {
                tracker.recordFailure(userId, tenantId, ip);
            }

            // when
            tracker.unlock(userId, tenantId);

            // then - 失败次数应该保留
            assertThat(tracker.getFailureCount(userId, tenantId)).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("重置测试")
    class ResetTests {

        @Test
        @DisplayName("重置 - 清除所有记录")
        void shouldResetAllRecords() {
            // given
            String userId = "user-reset-1";
            String tenantId = "tenant-1";
            String ip = "192.168.1.1";

            // 记录 3 次失败
            for (int i = 0; i < 3; i++) {
                tracker.recordFailure(userId, tenantId, ip);
            }
            assertThat(tracker.getFailureCount(userId, tenantId)).isEqualTo(3);

            // when
            tracker.reset(userId, tenantId);

            // then
            assertThat(tracker.getFailureCount(userId, tenantId)).isZero();
            assertThat(tracker.isLocked(userId, tenantId)).isFalse();
            assertThat(tracker.getLockedUntil(userId, tenantId)).isNull();
        }

        @Test
        @DisplayName("重置锁定状态的账户")
        void shouldResetLockedAccount() {
            // given
            String userId = "user-reset-2";
            String tenantId = "tenant-1";
            String ip = "192.168.1.1";

            // 记录 5 次失败（锁定）
            for (int i = 0; i < 5; i++) {
                tracker.recordFailure(userId, tenantId, ip);
            }
            assertThat(tracker.isLocked(userId, tenantId)).isTrue();

            // when
            tracker.reset(userId, tenantId);

            // then
            assertThat(tracker.getFailureCount(userId, tenantId)).isZero();
            assertThat(tracker.isLocked(userId, tenantId)).isFalse();
            assertThat(tracker.getLockedUntil(userId, tenantId)).isNull();
        }

        @Test
        @DisplayName("重置无记录的账户 - 无操作")
        void shouldDoNothingWhenResettingNonExistentRecord() {
            // given
            String userId = "user-reset-3";
            String tenantId = "tenant-1";

            // when
            tracker.reset(userId, tenantId);

            // then - 不应抛出异常
            assertThat(tracker.getFailureCount(userId, tenantId)).isZero();
        }
    }

    @Nested
    @DisplayName("配置测试")
    class ConfigurationTests {

        @Test
        @DisplayName("自定义最大失败次数")
        void shouldUseCustomMaxFailures() {
            // given
            tracker = new DefaultLoginFailureTracker(3, Duration.ofMinutes(30));
            String userId = "user-config-1";
            String tenantId = "tenant-1";
            String ip = "192.168.1.1";

            // when - 记录 3 次失败
            for (int i = 0; i < 3; i++) {
                tracker.recordFailure(userId, tenantId, ip);
            }

            // then - 应该锁定
            assertThat(tracker.isLocked(userId, tenantId)).isTrue();
        }

        @Test
        @DisplayName("自定义锁定时间")
        void shouldUseCustomLockDuration() {
            // given
            Duration customLockDuration = Duration.ofHours(1);
            tracker = new DefaultLoginFailureTracker(2, customLockDuration);
            String userId = "user-config-2";
            String tenantId = "tenant-1";
            String ip = "192.168.1.1";
            LocalDateTime beforeLock = LocalDateTime.now();

            // when - 记录 2 次失败（锁定）
            tracker.recordFailure(userId, tenantId, ip);
            tracker.recordFailure(userId, tenantId, ip);

            // then
            LocalDateTime lockedUntil = tracker.getLockedUntil(userId, tenantId);
            assertThat(lockedUntil).isNotNull();
            // 锁定时间应该接近 1 小时
            assertThat(lockedUntil).isAfter(beforeLock.plusMinutes(59));
            assertThat(lockedUntil).isBefore(beforeLock.plusHours(2));
        }
    }

    @Nested
    @DisplayName("并发测试")
    class ConcurrencyTests {

        @Test
        @DisplayName("并发记录失败 - 正确计数")
        void shouldHandleConcurrentRecordFailure() throws InterruptedException {
            // given
            String userId = "user-concurrent-1";
            String tenantId = "tenant-1";
            String ip = "192.168.1.1";
            int threadCount = 10;
            int failuresPerThread = 2;

            // when - 多线程并发记录失败
            Thread[] threads = new Thread[threadCount];
            for (int i = 0; i < threadCount; i++) {
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < failuresPerThread; j++) {
                        tracker.recordFailure(userId, tenantId, ip);
                    }
                });
            }

            for (Thread thread : threads) {
                thread.start();
            }
            for (Thread thread : threads) {
                thread.join();
            }

            // then - 失败次数应该正确
            int expectedCount = threadCount * failuresPerThread;
            assertThat(tracker.getFailureCount(userId, tenantId)).isEqualTo(expectedCount);
        }
    }
}
