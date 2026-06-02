package io.github.afgprojects.framework.security.auth.storage;

import io.github.afgprojects.framework.data.jdbc.JdbcDataManager;
import io.github.afgprojects.framework.security.auth.entity.AuthLoginFailure;
import io.github.afgprojects.framework.security.core.storage.AfgLoginFailureStorage.FailureRecord;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static io.github.afgprojects.framework.data.core.condition.Conditions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * JdbcLoginFailureStorage 测试。
 */
@DisplayName("JdbcLoginFailureStorage 测试")
class JdbcLoginFailureStorageTest {

    private JdbcDataManager dataManager;
    private JdbcLoginFailureStorage storage;
    private JdbcDataSource dataSource;

    @BeforeEach
    void setUp() {
        // 创建 H2 数据源
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:testdb_login_failure;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        // 创建 DataManager
        dataManager = new JdbcDataManager(dataSource);

        // 创建表
        try (var conn = dataSource.getConnection();
             var stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS auth_login_failure");
            stmt.execute("""
                CREATE TABLE auth_login_failure (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    user_id VARCHAR(64) NOT NULL UNIQUE,
                    username VARCHAR(128) NOT NULL,
                    tenant_id VARCHAR(64),
                    failure_count INT NOT NULL DEFAULT 0,
                    locked_until TIMESTAMP,
                    last_failure_ip VARCHAR(64),
                    last_failure_time TIMESTAMP,
                    created_at TIMESTAMP NOT NULL,
                    updated_at TIMESTAMP
                )
                """);
            stmt.execute("CREATE INDEX idx_username_failure ON auth_login_failure (username)");
            stmt.execute("CREATE INDEX idx_locked_until_failure ON auth_login_failure (locked_until)");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // 创建存储器（使用较小的锁定阈值便于测试）
        storage = new JdbcLoginFailureStorage(dataManager, 3, 30);
    }

    @AfterEach
    void tearDown() {
        try (var conn = dataSource.getConnection();
             var stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS auth_login_failure");
        } catch (Exception e) {
            // ignore
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
            var entity = dataManager.entity(AuthLoginFailure.class)
                    .query()
                    .where(builder(AuthLoginFailure.class)
                            .eq(AuthLoginFailure::getUserId, userId)
                            .build())
                    .one();
            assertThat(entity).isPresent();
            assertThat(entity.get().getFailureCount()).isEqualTo(1);
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
            var entity = dataManager.entity(AuthLoginFailure.class)
                    .query()
                    .where(builder(AuthLoginFailure.class)
                            .eq(AuthLoginFailure::getUserId, userId)
                            .build())
                    .one();
            assertThat(entity).isPresent();
            assertThat(entity.get().getFailureCount()).isEqualTo(3);
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
            var entity = dataManager.entity(AuthLoginFailure.class)
                    .query()
                    .where(builder(AuthLoginFailure.class)
                            .eq(AuthLoginFailure::getUserId, userId)
                            .build())
                    .one();
            assertThat(entity).isPresent();
            assertThat(entity.get().getLockedUntil()).isNotNull();
            assertThat(entity.get().getLockedUntil()).isAfter(Instant.now());
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
            var entity = dataManager.entity(AuthLoginFailure.class)
                    .query()
                    .where(builder(AuthLoginFailure.class)
                            .eq(AuthLoginFailure::getUserId, userId)
                            .build())
                    .one();
            assertThat(entity).isPresent();
            assertThat(entity.get().getLockedUntil()).isNull();
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
            var entity = dataManager.entity(AuthLoginFailure.class)
                    .query()
                    .where(builder(AuthLoginFailure.class)
                            .eq(AuthLoginFailure::getUserId, userId)
                            .build())
                    .one();
            assertThat(entity).isEmpty();
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
    }
}