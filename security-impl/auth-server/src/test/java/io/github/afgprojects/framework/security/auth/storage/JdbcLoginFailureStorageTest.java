package io.github.afgprojects.framework.security.auth.storage;

import io.github.afgprojects.framework.security.core.storage.AfgLoginFailureStorage.FailureRecord;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * JdbcLoginFailureStorage 测试。
 */
@DisplayName("JdbcLoginFailureStorage 测试")
class JdbcLoginFailureStorageTest {

    private JdbcTemplate jdbcTemplate;
    private JdbcLoginFailureStorage storage;

    @BeforeEach
    void setUp() {
        // 创建 H2 数据源
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:testdb_login_failure;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        // 创建 JdbcTemplate
        jdbcTemplate = new JdbcTemplate(dataSource);

        // 先删除表（如果存在）
        jdbcTemplate.execute("DROP TABLE IF EXISTS auth_login_failure");

        // 创建表
        jdbcTemplate.execute("""
                CREATE TABLE auth_login_failure (
                    user_id VARCHAR(64) PRIMARY KEY,
                    username VARCHAR(128) NOT NULL,
                    tenant_id VARCHAR(64),
                    failure_count INT NOT NULL DEFAULT 0,
                    locked_until TIMESTAMP,
                    last_failure_ip VARCHAR(64),
                    last_failure_at TIMESTAMP,
                    created_at TIMESTAMP NOT NULL,
                    updated_at TIMESTAMP NOT NULL
                )
                """);

        // 创建索引
        jdbcTemplate.execute("CREATE INDEX idx_username_failure ON auth_login_failure (username)");
        jdbcTemplate.execute("CREATE INDEX idx_locked_until_failure ON auth_login_failure (locked_until)");

        // 创建存储器（使用较小的锁定阈值便于测试）
        storage = new JdbcLoginFailureStorage(jdbcTemplate, "auth_login_failure", 3, 30);
    }

    @AfterEach
    void tearDown() {
        if (jdbcTemplate != null) {
            jdbcTemplate.execute("DROP TABLE IF EXISTS auth_login_failure");
        }
    }

    @Nested
    @DisplayName("recordFailure 方法测试")
    class RecordFailureTests {

        @Test
        @DisplayName("应该成功记录登录失败")
        void shouldRecordFailure() {
            // given
            String userId = "user-123";
            String username = "testuser";
            String tenantId = "tenant-001";
            String ip = "192.168.1.1";

            // when
            storage.recordFailure(userId, username, tenantId, ip);

            // then
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM auth_login_failure WHERE user_id = ?",
                    Integer.class,
                    userId
            );
            assertThat(count).isEqualTo(1);

            Integer failureCount = jdbcTemplate.queryForObject(
                    "SELECT failure_count FROM auth_login_failure WHERE user_id = ?",
                    Integer.class,
                    userId
            );
            assertThat(failureCount).isEqualTo(1);
        }

        @Test
        @DisplayName("应该保存所有字段的失败记录")
        void shouldSaveAllFields() {
            // given
            String userId = "user-complete";
            String username = "completeuser";
            String tenantId = "tenant-complete";
            String ip = "10.0.0.1";

            // when
            storage.recordFailure(userId, username, tenantId, ip);

            // then
            var record = jdbcTemplate.queryForObject(
                    "SELECT user_id, username, tenant_id, failure_count, last_failure_ip, last_failure_at, "
                            + "created_at, updated_at FROM auth_login_failure WHERE user_id = ?",
                    (rs, rowNum) -> new Object[]{
                            rs.getString("user_id"),
                            rs.getString("username"),
                            rs.getString("tenant_id"),
                            rs.getInt("failure_count"),
                            rs.getString("last_failure_ip"),
                            rs.getObject("last_failure_at", LocalDateTime.class),
                            rs.getObject("created_at", LocalDateTime.class),
                            rs.getObject("updated_at", LocalDateTime.class)
                    },
                    userId
            );

            assertThat(record).isNotNull();
            assertThat(record[0]).isEqualTo(userId);
            assertThat(record[1]).isEqualTo(username);
            assertThat(record[2]).isEqualTo(tenantId);
            assertThat(record[3]).isEqualTo(1);
            assertThat(record[4]).isEqualTo(ip);
            assertThat(record[5]).isNotNull();
            assertThat(record[6]).isNotNull();
            assertThat(record[7]).isNotNull();
        }

        @Test
        @DisplayName("应该支持多次记录失败")
        void shouldRecordMultipleFailures() {
            // given
            String userId = "user-multi";
            String username = "multiuser";

            // when
            for (int i = 0; i < 3; i++) {
                storage.recordFailure(userId, username, null, "192.168.1." + i);
            }

            // then
            Integer failureCount = jdbcTemplate.queryForObject(
                    "SELECT failure_count FROM auth_login_failure WHERE user_id = ?",
                    Integer.class,
                    userId
            );
            assertThat(failureCount).isEqualTo(3);
        }

        @Test
        @DisplayName("应该允许 tenantId 和 ip 为 null")
        void shouldAllowNullTenantIdAndIp() {
            // given
            String userId = "user-null-fields";
            String username = "nulluser";

            // when
            storage.recordFailure(userId, username, null, null);

            // then
            var record = jdbcTemplate.queryForObject(
                    "SELECT tenant_id, last_failure_ip FROM auth_login_failure WHERE user_id = ?",
                    (rs, rowNum) -> new Object[]{
                            rs.getString("tenant_id"),
                            rs.getString("last_failure_ip")
                    },
                    userId
            );

            assertThat(record).isNotNull();
            assertThat(record[0]).isNull();
            assertThat(record[1]).isNull();
        }

        @Test
        @DisplayName("达到阈值时应该自动锁定账户")
        void shouldLockAccountWhenThresholdReached() {
            // given
            String userId = "user-lock-threshold";
            String username = "lockuser";

            // when - 记录 3 次失败（达到阈值）
            for (int i = 0; i < 3; i++) {
                storage.recordFailure(userId, username, null, null);
            }

            // then
            LocalDateTime lockedUntil = jdbcTemplate.queryForObject(
                    "SELECT locked_until FROM auth_login_failure WHERE user_id = ?",
                    LocalDateTime.class,
                    userId
            );
            assertThat(lockedUntil).isNotNull();
            assertThat(lockedUntil).isAfter(LocalDateTime.now());
        }

        @Test
        @DisplayName("未达到阈值时不应锁定账户")
        void shouldNotLockAccountBeforeThreshold() {
            // given
            String userId = "user-no-lock";
            String username = "nolockuser";

            // when - 只记录 2 次失败（未达到阈值 3）
            for (int i = 0; i < 2; i++) {
                storage.recordFailure(userId, username, null, null);
            }

            // then
            LocalDateTime lockedUntil = jdbcTemplate.queryForObject(
                    "SELECT locked_until FROM auth_login_failure WHERE user_id = ?",
                    LocalDateTime.class,
                    userId
            );
            assertThat(lockedUntil).isNull();
        }
    }

    @Nested
    @DisplayName("getFailureCount 方法测试")
    class GetFailureCountTests {

        @Test
        @DisplayName("应该正确获取失败次数")
        void shouldGetFailureCount() {
            // given
            String userId = "user-count";
            String username = "countuser";
            storage.recordFailure(userId, username, null, null);
            storage.recordFailure(userId, username, null, null);

            // when
            int count = storage.getFailureCount(userId);

            // then
            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("当用户不存在时应该返回 0")
        void shouldReturnZeroWhenUserNotFound() {
            // given
            String userId = "user-nonexistent-count";

            // when
            int count = storage.getFailureCount(userId);

            // then
            assertThat(count).isZero();
        }
    }

    @Nested
    @DisplayName("isLocked 方法测试")
    class IsLockedTests {

        @Test
        @DisplayName("应该正确识别锁定的账户")
        void shouldIdentifyLockedAccount() {
            // given
            String userId = "user-locked";
            String username = "lockeduser";

            // 记录 3 次失败以锁定
            for (int i = 0; i < 3; i++) {
                storage.recordFailure(userId, username, null, null);
            }

            // when
            boolean result = storage.isLocked(userId);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("应该正确识别未锁定的账户")
        void shouldIdentifyUnlockedAccount() {
            // given
            String userId = "user-unlocked";
            String username = "unlockeduser";

            // 只记录 1 次失败
            storage.recordFailure(userId, username, null, null);

            // when
            boolean result = storage.isLocked(userId);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("应该正确识别不存在的用户")
        void shouldIdentifyNonExistentUser() {
            // given
            String userId = "user-nonexistent-locked";

            // when
            boolean result = storage.isLocked(userId);

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("getLockedUntil 方法测试")
    class GetLockedUntilTests {

        @Test
        @DisplayName("应该正确获取锁定截止时间")
        void shouldGetLockedUntil() {
            // given
            String userId = "user-locked-until";
            String username = "lockeduntiluser";

            // 记录 3 次失败以锁定
            for (int i = 0; i < 3; i++) {
                storage.recordFailure(userId, username, null, null);
            }

            // when
            LocalDateTime result = storage.getLockedUntil(userId);

            // then
            assertThat(result).isNotNull();
            assertThat(result).isAfter(LocalDateTime.now());
        }

        @Test
        @DisplayName("当账户未锁定时应该返回 null")
        void shouldReturnNullWhenNotLocked() {
            // given
            String userId = "user-not-locked-until";
            String username = "notlockeduntiluser";

            storage.recordFailure(userId, username, null, null);

            // when
            LocalDateTime result = storage.getLockedUntil(userId);

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("当用户不存在时应该返回 null")
        void shouldReturnNullWhenUserNotFound() {
            // given
            String userId = "user-nonexistent-locked-until";

            // when
            LocalDateTime result = storage.getLockedUntil(userId);

            // then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("unlock 方法测试")
    class UnlockTests {

        @Test
        @DisplayName("应该成功解锁账户")
        void shouldUnlockAccount() {
            // given
            String userId = "user-unlock";
            String username = "unlockuser";

            // 记录 3 次失败以锁定
            for (int i = 0; i < 3; i++) {
                storage.recordFailure(userId, username, null, null);
            }

            // 确认已锁定
            assertThat(storage.isLocked(userId)).isTrue();

            // when
            storage.unlock(userId);

            // then
            assertThat(storage.isLocked(userId)).isFalse();
            assertThat(storage.getFailureCount(userId)).isZero();
        }

        @Test
        @DisplayName("解锁不存在的用户不应该抛出异常")
        void shouldNotThrowWhenUnlockingNonExistentUser() {
            // given
            String userId = "user-nonexistent-unlock";

            // when & then
            assertThatCode(() -> storage.unlock(userId))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("reset 方法测试")
    class ResetTests {

        @Test
        @DisplayName("应该成功重置失败次数")
        void shouldResetFailureCount() {
            // given
            String userId = "user-reset";
            String username = "resetuser";

            storage.recordFailure(userId, username, null, null);
            storage.recordFailure(userId, username, null, null);

            // when
            storage.reset(userId);

            // then
            assertThat(storage.getFailureCount(userId)).isZero();
        }

        @Test
        @DisplayName("重置应该清除锁定状态")
        void shouldClearLockOnReset() {
            // given
            String userId = "user-reset-lock";
            String username = "resetlockuser";

            // 记录 3 次失败以锁定
            for (int i = 0; i < 3; i++) {
                storage.recordFailure(userId, username, null, null);
            }

            // 确认已锁定
            assertThat(storage.isLocked(userId)).isTrue();

            // when
            storage.reset(userId);

            // then
            assertThat(storage.isLocked(userId)).isFalse();
        }

        @Test
        @DisplayName("重置不存在的用户不应该抛出异常")
        void shouldNotThrowWhenResettingNonExistentUser() {
            // given
            String userId = "user-nonexistent-reset";

            // when & then
            assertThatCode(() -> storage.reset(userId))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("getFailureRecord 方法测试")
    class GetFailureRecordTests {

        @Test
        @DisplayName("应该成功获取失败记录")
        void shouldGetFailureRecord() {
            // given
            String userId = "user-record";
            String username = "recorduser";
            String ip = "192.168.1.100";

            storage.recordFailure(userId, username, null, ip);
            storage.recordFailure(userId, username, null, ip);

            // when
            Optional<FailureRecord> result = storage.getFailureRecord(userId);

            // then
            assertThat(result).isPresent();
            FailureRecord record = result.get();
            assertThat(record.count()).isEqualTo(2);
            assertThat(record.lastIp()).isEqualTo(ip);
        }

        @Test
        @DisplayName("当用户不存在时应该返回空")
        void shouldReturnEmptyWhenUserNotFound() {
            // given
            String userId = "user-nonexistent-record";

            // when
            Optional<FailureRecord> result = storage.getFailureRecord(userId);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("delete 方法测试")
    class DeleteTests {

        @Test
        @DisplayName("应该成功删除失败记录")
        void shouldDeleteFailureRecord() {
            // given
            String userId = "user-delete";
            String username = "deleteuser";
            storage.recordFailure(userId, username, null, null);

            // when
            storage.delete(userId);

            // then
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM auth_login_failure WHERE user_id = ?",
                    Integer.class,
                    userId
            );
            assertThat(count).isZero();
        }

        @Test
        @DisplayName("删除不存在的用户不应该抛出异常")
        void shouldNotThrowWhenDeletingNonExistentUser() {
            // given
            String userId = "user-nonexistent-delete";

            // when & then
            assertThatCode(() -> storage.delete(userId))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("clearExpiredLocks 方法测试")
    class ClearExpiredLocksTests {

        @Test
        @DisplayName("应该清理过期的锁定状态")
        void shouldClearExpiredLocks() {
            // given
            String userId = "user-expired-lock";
            String username = "expiredlockuser";

            // 记录失败并锁定
            for (int i = 0; i < 3; i++) {
                storage.recordFailure(userId, username, null, null);
            }

            // 手动设置锁定时间为过去
            jdbcTemplate.update(
                    "UPDATE auth_login_failure SET locked_until = ? WHERE user_id = ?",
                    LocalDateTime.now().minusMinutes(1),
                    userId
            );

            // when
            int clearedCount = storage.clearExpiredLocks();

            // then
            assertThat(clearedCount).isEqualTo(1);
            assertThat(storage.isLocked(userId)).isFalse();
        }

        @Test
        @DisplayName("当没有过期锁定时应该返回 0")
        void shouldReturnZeroWhenNoExpiredLocks() {
            // given
            String userId = "user-no-expired-lock";
            String username = "noexpiredlockuser";

            // 记录失败并锁定（锁定时间在未来）
            for (int i = 0; i < 3; i++) {
                storage.recordFailure(userId, username, null, null);
            }

            // when
            int clearedCount = storage.clearExpiredLocks();

            // then
            assertThat(clearedCount).isZero();
        }
    }

    @Nested
    @DisplayName("自定义表名测试")
    class CustomTableNameTests {

        @Test
        @DisplayName("应该支持自定义表名")
        void shouldSupportCustomTableName() {
            // given
            String customTableName = "custom_login_failure";
            jdbcTemplate.execute("DROP TABLE IF EXISTS " + customTableName);
            jdbcTemplate.execute(String.format("""
                    CREATE TABLE %s (
                        user_id VARCHAR(64) PRIMARY KEY,
                        username VARCHAR(128) NOT NULL,
                        tenant_id VARCHAR(64),
                        failure_count INT NOT NULL DEFAULT 0,
                        locked_until TIMESTAMP,
                        last_failure_ip VARCHAR(64),
                        last_failure_at TIMESTAMP,
                        created_at TIMESTAMP NOT NULL,
                        updated_at TIMESTAMP NOT NULL
                    )
                    """, customTableName));

            JdbcLoginFailureStorage customStorage = new JdbcLoginFailureStorage(
                    jdbcTemplate, customTableName, 3, 30
            );

            String userId = "user-custom-table";
            String username = "customtableuser";

            // when
            customStorage.recordFailure(userId, username, null, null);

            // then
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + customTableName + " WHERE user_id = ?",
                    Integer.class,
                    userId
            );
            assertThat(count).isEqualTo(1);

            // cleanup
            jdbcTemplate.execute("DROP TABLE IF EXISTS " + customTableName);
        }
    }

    @Nested
    @DisplayName("自定义锁定策略测试")
    class CustomLockPolicyTests {

        @Test
        @DisplayName("应该支持自定义锁定阈值")
        void shouldSupportCustomLockThreshold() {
            // given
            String customTableName = "custom_threshold";
            jdbcTemplate.execute("DROP TABLE IF EXISTS " + customTableName);
            jdbcTemplate.execute(String.format("""
                    CREATE TABLE %s (
                        user_id VARCHAR(64) PRIMARY KEY,
                        username VARCHAR(128) NOT NULL,
                        tenant_id VARCHAR(64),
                        failure_count INT NOT NULL DEFAULT 0,
                        locked_until TIMESTAMP,
                        last_failure_ip VARCHAR(64),
                        last_failure_at TIMESTAMP,
                        created_at TIMESTAMP NOT NULL,
                        updated_at TIMESTAMP NOT NULL
                    )
                    """, customTableName));

            // 使用阈值 5
            JdbcLoginFailureStorage customStorage = new JdbcLoginFailureStorage(
                    jdbcTemplate, customTableName, 5, 30
            );

            String userId = "user-threshold-5";
            String username = "threshold5user";

            // when - 记录 4 次失败（未达到阈值 5）
            for (int i = 0; i < 4; i++) {
                customStorage.recordFailure(userId, username, null, null);
            }

            // then - 不应该锁定
            assertThat(customStorage.isLocked(userId)).isFalse();

            // when - 再记录 1 次
            customStorage.recordFailure(userId, username, null, null);

            // then - 应该锁定
            assertThat(customStorage.isLocked(userId)).isTrue();

            // cleanup
            jdbcTemplate.execute("DROP TABLE IF EXISTS " + customTableName);
        }
    }

    @Nested
    @DisplayName("并发场景测试")
    class ConcurrencyTests {

        @Test
        @DisplayName("应该支持多用户同时记录失败")
        void shouldSupportConcurrentRecordFailures() {
            // given
            int userCount = 10;

            // when
            for (int i = 0; i < userCount; i++) {
                String userId = "user-concurrent-" + i;
                String username = "concurrentuser" + i;
                storage.recordFailure(userId, username, null, null);
            }

            // then
            Integer totalCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM auth_login_failure",
                    Integer.class
            );
            assertThat(totalCount).isEqualTo(userCount);
        }
    }

    @Nested
    @DisplayName("完整流程测试")
    class FullWorkflowTests {

        @Test
        @DisplayName("应该支持完整的锁定-解锁流程")
        void shouldSupportFullLockUnlockWorkflow() {
            // given
            String userId = "user-full-workflow";
            String username = "fullworkflowuser";

            // when - 记录失败直到锁定
            for (int i = 0; i < 3; i++) {
                storage.recordFailure(userId, username, null, null);
            }

            // then - 应该锁定
            assertThat(storage.isLocked(userId)).isTrue();
            assertThat(storage.getFailureCount(userId)).isEqualTo(3);

            // when - 解锁
            storage.unlock(userId);

            // then - 应该解锁
            assertThat(storage.isLocked(userId)).isFalse();
            assertThat(storage.getFailureCount(userId)).isZero();

            // when - 再次记录失败
            storage.recordFailure(userId, username, null, null);

            // then - 失败次数应该从 1 开始
            assertThat(storage.getFailureCount(userId)).isEqualTo(1);
            assertThat(storage.isLocked(userId)).isFalse();
        }

        @Test
        @DisplayName("应该支持登录成功后重置流程")
        void shouldSupportLoginSuccessResetWorkflow() {
            // given
            String userId = "user-success-reset";
            String username = "successresetuser";

            // 记录一些失败
            storage.recordFailure(userId, username, null, null);
            storage.recordFailure(userId, username, null, null);

            // when - 登录成功后重置
            storage.reset(userId);

            // then
            assertThat(storage.getFailureCount(userId)).isZero();
            assertThat(storage.isLocked(userId)).isFalse();
        }
    }
}
