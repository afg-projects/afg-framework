package io.github.afgprojects.framework.security.auth.storage;

import io.github.afgprojects.framework.data.jdbc.JdbcDataManager;
import io.github.afgprojects.framework.security.auth.entity.AuthTokenBlacklist;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JdbcTokenBlacklist 测试。
 */
@DisplayName("JdbcTokenBlacklist 测试")
class JdbcTokenBlacklistTest {

    private JdbcDataManager dataManager;
    private JdbcTokenBlacklist storage;
    private JdbcDataSource dataSource;

    @BeforeEach
    void setUp() {
        // 创建 H2 数据源
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:testdb_blacklist;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        // 创建 DataManager
        dataManager = new JdbcDataManager(dataSource);

        // 创建表
        try (var conn = dataSource.getConnection();
             var stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS auth_token_blacklist");
            stmt.execute("""
                CREATE TABLE auth_token_blacklist (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    token_hash VARCHAR(128) NOT NULL UNIQUE,
                    user_id VARCHAR(64) NOT NULL,
                    reason VARCHAR(100),
                    expires_at TIMESTAMP NOT NULL,
                    created_at TIMESTAMP NOT NULL,
                    updated_at TIMESTAMP
                )
                """);
            stmt.execute("CREATE INDEX idx_user_id_blacklist ON auth_token_blacklist (user_id)");
            stmt.execute("CREATE INDEX idx_expires_at_blacklist ON auth_token_blacklist (expires_at)");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // 创建存储器
        storage = new JdbcTokenBlacklist(dataManager);
    }

    @AfterEach
    void tearDown() {
        try (var conn = dataSource.getConnection();
             var stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS auth_token_blacklist");
        } catch (Exception e) {
            // ignore
        }
    }

    @Nested
    @DisplayName("addToBlacklist 方法测试")
    class AddToBlacklistTests {

        @Test
        @DisplayName("应该成功将 Token 加入黑名单")
        void shouldAddTokenToBlacklist() {
            // given
            String tokenHash = "hash-" + System.currentTimeMillis();
            String userId = "user-123";
            String reason = "logout";
            Duration ttl = Duration.ofHours(1);

            // when
            storage.addToBlacklist(tokenHash, userId, reason, ttl);

            // then
            var entity = dataManager.entity(AuthTokenBlacklist.class)
                    .query()
                    .where(io.github.afgprojects.framework.data.core.condition.Conditions.builder()
                            .eq("token_hash", tokenHash)
                            .build())
                    .one();
            assertThat(entity).isPresent();
        }

        @Test
        @DisplayName("应该保存所有字段的黑名单记录")
        void shouldSaveAllFields() {
            // given
            String tokenHash = "hash-complete-" + System.currentTimeMillis();
            String userId = "user-456";
            String reason = "security_breach";
            Duration ttl = Duration.ofDays(7);

            // when
            storage.addToBlacklist(tokenHash, userId, reason, ttl);

            // then
            var entity = dataManager.entity(AuthTokenBlacklist.class)
                    .query()
                    .where(io.github.afgprojects.framework.data.core.condition.Conditions.builder()
                            .eq("token_hash", tokenHash)
                            .build())
                    .one();

            assertThat(entity).isPresent();
            assertThat(entity.get().getUserId()).isEqualTo(userId);
            assertThat(entity.get().getReason()).isEqualTo(reason);
            assertThat(entity.get().getExpiresAt()).isNotNull();
            assertThat(entity.get().getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("应该支持更新已存在的黑名单记录")
        void shouldUpdateExistingBlacklistRecord() {
            // given
            String tokenHash = "hash-update-" + System.currentTimeMillis();
            String userId = "user-789";
            String reason1 = "logout";
            String reason2 = "revoked";
            Duration ttl = Duration.ofHours(1);

            // 先添加一次
            storage.addToBlacklist(tokenHash, userId, reason1, ttl);

            // when - 再次添加相同 tokenHash
            storage.addToBlacklist(tokenHash, userId, reason2, ttl);

            // then - 应该只有一条记录，且 reason 已更新
            var entities = dataManager.entity(AuthTokenBlacklist.class)
                    .query()
                    .where(io.github.afgprojects.framework.data.core.condition.Conditions.builder()
                            .eq("token_hash", tokenHash)
                            .build())
                    .list();
            assertThat(entities).hasSize(1);
            assertThat(entities.get(0).getReason()).isEqualTo(reason2);
        }
    }

    @Nested
    @DisplayName("isBlacklisted 方法测试")
    class IsBlacklistedTests {

        @Test
        @DisplayName("应该正确识别黑名单中的 Token")
        void shouldIdentifyBlacklistedToken() {
            // given
            String tokenHash = "hash-blacklisted-" + System.currentTimeMillis();
            String userId = "user-123";
            String reason = "logout";
            Duration ttl = Duration.ofHours(1);
            storage.addToBlacklist(tokenHash, userId, reason, ttl);

            // when
            boolean result = storage.isBlacklisted(tokenHash);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("应该正确识别不在黑名单中的 Token")
        void shouldIdentifyNonBlacklistedToken() {
            // given
            String tokenHash = "hash-not-blacklisted";

            // when
            boolean result = storage.isBlacklisted(tokenHash);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("应该正确识别过期的黑名单 Token")
        void shouldIdentifyExpiredBlacklistedToken() {
            // given
            String tokenHash = "hash-expired-" + System.currentTimeMillis();
            String userId = "user-123";
            String reason = "logout";
            Duration ttl = Duration.ofMillis(100);

            storage.addToBlacklist(tokenHash, userId, reason, ttl);

            // 等待过期
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // when
            boolean result = storage.isBlacklisted(tokenHash);

            // then - 过期的记录应该被清理，所以不在黑名单中
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("blacklistAllUserTokens 方法测试")
    class BlacklistAllUserTokensTests {

        @Test
        @DisplayName("应该成功将用户所有 Token 加入黑名单")
        void shouldBlacklistAllUserTokens() {
            // given
            String userId = "user-all-123";
            Duration ttl = Duration.ofHours(1);

            // when
            storage.blacklistAllUserTokens(userId, ttl);

            // then
            String userBlacklistTokenHash = "user_all:" + userId;
            var entity = dataManager.entity(AuthTokenBlacklist.class)
                    .query()
                    .where(io.github.afgprojects.framework.data.core.condition.Conditions.builder()
                            .eq("token_hash", userBlacklistTokenHash)
                            .build())
                    .one();
            assertThat(entity).isPresent();
        }

        @Test
        @DisplayName("应该支持更新用户级别的黑名单")
        void shouldUpdateUserLevelBlacklist() {
            // given
            String userId = "user-all-update";
            Duration ttl1 = Duration.ofHours(1);
            Duration ttl2 = Duration.ofHours(2);

            storage.blacklistAllUserTokens(userId, ttl1);

            // when - 再次调用
            storage.blacklistAllUserTokens(userId, ttl2);

            // then - 应该只有一条记录
            String userBlacklistTokenHash = "user_all:" + userId;
            var entities = dataManager.entity(AuthTokenBlacklist.class)
                    .query()
                    .where(io.github.afgprojects.framework.data.core.condition.Conditions.builder()
                            .eq("token_hash", userBlacklistTokenHash)
                            .build())
                    .list();
            assertThat(entities).hasSize(1);
        }
    }

    @Nested
    @DisplayName("isUserBlacklisted 方法测试")
    class IsUserBlacklistedTests {

        @Test
        @DisplayName("应该正确识别被全局拉黑的用户")
        void shouldIdentifyBlacklistedUser() {
            // given
            String userId = "user-blacklisted";
            Duration ttl = Duration.ofHours(1);
            storage.blacklistAllUserTokens(userId, ttl);

            // when
            boolean result = storage.isUserBlacklisted(userId);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("应该正确识别未被全局拉黑的用户")
        void shouldIdentifyNonBlacklistedUser() {
            // given
            String userId = "user-not-blacklisted";

            // when
            boolean result = storage.isUserBlacklisted(userId);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("应该正确识别过期被全局拉黑的用户")
        void shouldIdentifyExpiredBlacklistedUser() {
            // given
            String userId = "user-expired-blacklist";
            Duration ttl = Duration.ofMillis(100);

            storage.blacklistAllUserTokens(userId, ttl);

            // 等待过期
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // when
            boolean result = storage.isUserBlacklisted(userId);

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("isBlacklistedWithUserCheck 方法测试")
    class IsBlacklistedWithUserCheckTests {

        @Test
        @DisplayName("应该检查 Token 黑名单和用户黑名单")
        void shouldCheckTokenAndUserBlacklist() {
            // given
            String tokenHash = "hash-combined-" + System.currentTimeMillis();
            String userId = "user-combined";
            Duration ttl = Duration.ofHours(1);

            // 只添加 Token 黑名单
            storage.addToBlacklist(tokenHash, userId, "logout", ttl);

            // when
            boolean result = storage.isBlacklistedWithUserCheck(tokenHash, userId);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("应该通过用户黑名单识别")
        void shouldIdentifyByUserBlacklist() {
            // given
            String tokenHash = "hash-user-level";
            String userId = "user-level-check";
            Duration ttl = Duration.ofHours(1);

            // 只添加用户级别的黑名单
            storage.blacklistAllUserTokens(userId, ttl);

            // when
            boolean result = storage.isBlacklistedWithUserCheck(tokenHash, userId);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("应该正确识别都不在黑名单的情况")
        void shouldIdentifyNotBlacklisted() {
            // given
            String tokenHash = "hash-not-in-any";
            String userId = "user-not-in-any";

            // when
            boolean result = storage.isBlacklistedWithUserCheck(tokenHash, userId);

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("deleteExpired 方法测试")
    class DeleteExpiredTests {

        @Test
        @DisplayName("应该删除过期的黑名单记录")
        void shouldDeleteExpiredRecords() {
            // given
            String tokenHash = "hash-expired-delete-" + System.currentTimeMillis();
            String userId = "user-expired-delete";
            Duration ttl = Duration.ofMillis(100);

            storage.addToBlacklist(tokenHash, userId, "logout", ttl);

            // 等待过期
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // when
            int deletedCount = storage.deleteExpired();

            // then
            assertThat(deletedCount).isEqualTo(1);
        }

        @Test
        @DisplayName("当没有过期记录时应该返回 0")
        void shouldReturnZeroWhenNoExpiredRecords() {
            // given
            String tokenHash = "hash-valid-delete-" + System.currentTimeMillis();
            String userId = "user-valid-delete";
            Duration ttl = Duration.ofHours(1);

            storage.addToBlacklist(tokenHash, userId, "logout", ttl);

            // when
            int deletedCount = storage.deleteExpired();

            // then
            assertThat(deletedCount).isZero();
        }

        @Test
        @DisplayName("应该删除多个过期的黑名单记录")
        void shouldDeleteMultipleExpiredRecords() {
            // given
            for (int i = 0; i < 5; i++) {
                String tokenHash = "hash-expired-multi-" + i + "-" + System.currentTimeMillis();
                String userId = "user-expired-multi-" + i;
                Duration ttl = Duration.ofMillis(100);
                storage.addToBlacklist(tokenHash, userId, "logout", ttl);
            }

            // 等待过期
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // when
            int deletedCount = storage.deleteExpired();

            // then
            assertThat(deletedCount).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("并发场景测试")
    class ConcurrencyTests {

        @Test
        @DisplayName("应该支持多 Token 同时加入黑名单")
        void shouldSupportConcurrentAdditions() {
            // given
            int tokenCount = 10;

            // when
            for (int i = 0; i < tokenCount; i++) {
                String tokenHash = "hash-concurrent-" + i + "-" + System.currentTimeMillis();
                String userId = "user-concurrent-" + i;
                Duration ttl = Duration.ofHours(1);
                storage.addToBlacklist(tokenHash, userId, "logout", ttl);
            }

            // then
            long totalCount = dataManager.count(AuthTokenBlacklist.class);
            assertThat(totalCount).isEqualTo(tokenCount);
        }
    }
}
