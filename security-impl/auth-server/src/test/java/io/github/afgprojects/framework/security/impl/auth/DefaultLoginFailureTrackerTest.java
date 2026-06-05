package io.github.afgprojects.framework.security.impl.auth;

import io.github.afgprojects.framework.security.auth.security.DefaultLoginFailureTracker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DefaultLoginFailureTracker 测试
 */
@DisplayName("DefaultLoginFailureTracker 测试")
class DefaultLoginFailureTrackerTest {

    private DefaultLoginFailureTracker tracker;

    @BeforeEach
    void setUp() {
        // 3 次失败后锁定，锁定 5 分钟
        tracker = new DefaultLoginFailureTracker(3, Duration.ofMinutes(5));
    }

    @Nested
    @DisplayName("recordFailure 方法")
    class RecordFailureTests {

        @Test
        @DisplayName("首次失败应计数为 1")
        void shouldCountOneAfterFirstFailure() {
            tracker.recordFailure("user-001", null, "192.168.1.1");

            assertThat(tracker.getFailureCount("user-001", null)).isEqualTo(1);
        }

        @Test
        @DisplayName("多次失败应累加计数")
        void shouldAccumulateCount() {
            tracker.recordFailure("user-001", null, "192.168.1.1");
            tracker.recordFailure("user-001", null, "192.168.1.2");
            tracker.recordFailure("user-001", null, "192.168.1.3");

            assertThat(tracker.getFailureCount("user-001", null)).isEqualTo(3);
        }

        @Test
        @DisplayName("不同用户应独立计数")
        void shouldCountSeparatelyForDifferentUsers() {
            tracker.recordFailure("user-001", null, "192.168.1.1");
            tracker.recordFailure("user-001", null, "192.168.1.2");
            tracker.recordFailure("user-002", null, "192.168.1.1");

            assertThat(tracker.getFailureCount("user-001", null)).isEqualTo(2);
            assertThat(tracker.getFailureCount("user-002", null)).isEqualTo(1);
        }

        @Test
        @DisplayName("不同租户的相同用户 ID 应独立计数")
        void shouldCountSeparatelyForDifferentTenants() {
            tracker.recordFailure("user-001", "tenant-001", "192.168.1.1");
            tracker.recordFailure("user-001", "tenant-002", "192.168.1.1");

            assertThat(tracker.getFailureCount("user-001", "tenant-001")).isEqualTo(1);
            assertThat(tracker.getFailureCount("user-001", "tenant-002")).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("isLocked 方法")
    class IsLockedTests {

        @Test
        @DisplayName("未达到最大失败次数应不锁定")
        void shouldNotLockBeforeMaxFailures() {
            tracker.recordFailure("user-001", null, "192.168.1.1");
            tracker.recordFailure("user-001", null, "192.168.1.2");

            assertThat(tracker.isLocked("user-001", null)).isFalse();
        }

        @Test
        @DisplayName("达到最大失败次数应锁定")
        void shouldLockAfterMaxFailures() {
            tracker.recordFailure("user-001", null, "192.168.1.1");
            tracker.recordFailure("user-001", null, "192.168.1.2");
            tracker.recordFailure("user-001", null, "192.168.1.3");

            assertThat(tracker.isLocked("user-001", null)).isTrue();
        }

        @Test
        @DisplayName("无失败记录应不锁定")
        void shouldNotLockWhenNoFailures() {
            assertThat(tracker.isLocked("user-001", null)).isFalse();
        }

        @Test
        @DisplayName("锁定后继续失败应保持锁定")
        void shouldRemainLockedAfterMoreFailures() {
            tracker.recordFailure("user-001", null, "192.168.1.1");
            tracker.recordFailure("user-001", null, "192.168.1.2");
            tracker.recordFailure("user-001", null, "192.168.1.3");
            tracker.recordFailure("user-001", null, "192.168.1.4");

            assertThat(tracker.isLocked("user-001", null)).isTrue();
            assertThat(tracker.getFailureCount("user-001", null)).isEqualTo(4);
        }
    }

    @Nested
    @DisplayName("getLockedUntil 方法")
    class GetLockedUntilTests {

        @Test
        @DisplayName("未锁定时应返回 null")
        void shouldReturnNullWhenNotLocked() {
            tracker.recordFailure("user-001", null, "192.168.1.1");

            assertThat(tracker.getLockedUntil("user-001", null)).isNull();
        }

        @Test
        @DisplayName("锁定后应返回锁定截止时间")
        void shouldReturnLockedUntilTime() {
            tracker.recordFailure("user-001", null, "192.168.1.1");
            tracker.recordFailure("user-001", null, "192.168.1.2");
            tracker.recordFailure("user-001", null, "192.168.1.3");

            LocalDateTime lockedUntil = tracker.getLockedUntil("user-001", null);

            assertThat(lockedUntil).isNotNull();
            assertThat(lockedUntil).isAfter(LocalDateTime.now());
        }

        @Test
        @DisplayName("无记录时应返回 null")
        void shouldReturnNullWhenNoRecord() {
            assertThat(tracker.getLockedUntil("user-001", null)).isNull();
        }
    }

    @Nested
    @DisplayName("unlock 方法")
    class UnlockTests {

        @Test
        @DisplayName("解锁后应不再锁定")
        void shouldUnlock() {
            tracker.recordFailure("user-001", null, "192.168.1.1");
            tracker.recordFailure("user-001", null, "192.168.1.2");
            tracker.recordFailure("user-001", null, "192.168.1.3");
            assertThat(tracker.isLocked("user-001", null)).isTrue();

            tracker.unlock("user-001", null);

            assertThat(tracker.isLocked("user-001", null)).isFalse();
        }

        @Test
        @DisplayName("解锁后失败计数应保留")
        void shouldKeepFailureCountAfterUnlock() {
            tracker.recordFailure("user-001", null, "192.168.1.1");
            tracker.recordFailure("user-001", null, "192.168.1.2");
            tracker.recordFailure("user-001", null, "192.168.1.3");

            tracker.unlock("user-001", null);

            assertThat(tracker.getFailureCount("user-001", null)).isEqualTo(3);
        }

        @Test
        @DisplayName("解锁不存在的用户应无效果")
        void shouldDoNothingForNonExistentUser() {
            tracker.unlock("non-existent", null);

            // 无异常即通过
        }
    }

    @Nested
    @DisplayName("reset 方法")
    class ResetTests {

        @Test
        @DisplayName("重置后失败计数应归零")
        void shouldResetFailureCount() {
            tracker.recordFailure("user-001", null, "192.168.1.1");
            tracker.recordFailure("user-001", null, "192.168.1.2");

            tracker.reset("user-001", null);

            assertThat(tracker.getFailureCount("user-001", null)).isZero();
        }

        @Test
        @DisplayName("重置后应不再锁定")
        void shouldNotBeLockedAfterReset() {
            tracker.recordFailure("user-001", null, "192.168.1.1");
            tracker.recordFailure("user-001", null, "192.168.1.2");
            tracker.recordFailure("user-001", null, "192.168.1.3");

            tracker.reset("user-001", null);

            assertThat(tracker.isLocked("user-001", null)).isFalse();
        }

        @Test
        @DisplayName("重置不存在的用户应无效果")
        void shouldDoNothingForNonExistentUser() {
            tracker.reset("non-existent", null);

            // 无异常即通过
        }
    }

    @Nested
    @DisplayName("多租户隔离")
    class TenantIsolationTests {

        @Test
        @DisplayName("一个租户的锁定不应影响其他租户")
        void shouldIsolateLockByTenant() {
            // tenant-001 的用户达到锁定阈值
            tracker.recordFailure("user-001", "tenant-001", "192.168.1.1");
            tracker.recordFailure("user-001", "tenant-001", "192.168.1.2");
            tracker.recordFailure("user-001", "tenant-001", "192.168.1.3");

            // tenant-002 的同一用户只有 1 次失败
            tracker.recordFailure("user-001", "tenant-002", "192.168.1.1");

            assertThat(tracker.isLocked("user-001", "tenant-001")).isTrue();
            assertThat(tracker.isLocked("user-001", "tenant-002")).isFalse();
        }

        @Test
        @DisplayName("解锁一个租户不应影响其他租户")
        void shouldIsolateUnlockByTenant() {
            tracker.recordFailure("user-001", "tenant-001", "192.168.1.1");
            tracker.recordFailure("user-001", "tenant-001", "192.168.1.2");
            tracker.recordFailure("user-001", "tenant-001", "192.168.1.3");
            tracker.recordFailure("user-001", "tenant-002", "192.168.1.1");
            tracker.recordFailure("user-001", "tenant-002", "192.168.1.2");
            tracker.recordFailure("user-001", "tenant-002", "192.168.1.3");

            tracker.unlock("user-001", "tenant-001");

            assertThat(tracker.isLocked("user-001", "tenant-001")).isFalse();
            assertThat(tracker.isLocked("user-001", "tenant-002")).isTrue();
        }
    }

    @Nested
    @DisplayName("配置参数")
    class ConfigurationTests {

        @Test
        @DisplayName("不同的最大失败次数配置应生效")
        void shouldRespectMaxFailuresConfig() {
            DefaultLoginFailureTracker customTracker = new DefaultLoginFailureTracker(5, Duration.ofMinutes(10));

            customTracker.recordFailure("user-001", null, "192.168.1.1");
            customTracker.recordFailure("user-001", null, "192.168.1.2");
            customTracker.recordFailure("user-001", null, "192.168.1.3");

            // 3 次不应锁定（配置为 5 次）
            assertThat(customTracker.isLocked("user-001", null)).isFalse();

            customTracker.recordFailure("user-001", null, "192.168.1.4");
            customTracker.recordFailure("user-001", null, "192.168.1.5");

            // 5 次应锁定
            assertThat(customTracker.isLocked("user-001", null)).isTrue();
        }
    }
}
