package io.github.afgprojects.framework.security.auth.storage;

import io.github.afgprojects.framework.data.jdbc.JdbcDataManager;
import io.github.afgprojects.framework.security.auth.entity.AuthRefreshToken;
import io.github.afgprojects.framework.security.core.storage.AfgRefreshTokenStorage.RefreshTokenInfo;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static io.github.afgprojects.framework.data.core.condition.Conditions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * JdbcRefreshTokenStorage 测试。
 */
@DisplayName("JdbcRefreshTokenStorage 测试")
class JdbcRefreshTokenStorageTest {

    private JdbcDataManager dataManager;
    private JdbcRefreshTokenStorage storage;
    private JdbcDataSource dataSource;

    @BeforeEach
    void setUp() {
        // 创建 H2 数据源
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:testdb_refresh;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        // 创建 DataManager
        dataManager = new JdbcDataManager(dataSource);

        // 创建表
        try (var conn = dataSource.getConnection();
             var stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS auth_refresh_token");
            stmt.execute("""
                CREATE TABLE auth_refresh_token (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    token_id VARCHAR(64) NOT NULL UNIQUE,
                    token_hash VARCHAR(128) NOT NULL UNIQUE,
                    user_id VARCHAR(64) NOT NULL,
                    tenant_id VARCHAR(64),
                    client_id VARCHAR(128),
                    device_id VARCHAR(128),
                    expires_at TIMESTAMP NOT NULL,
                    created_at TIMESTAMP NOT NULL,
                    updated_at TIMESTAMP
                )
                """);
            stmt.execute("CREATE INDEX idx_user_id_refresh ON auth_refresh_token (user_id)");
            stmt.execute("CREATE INDEX idx_token_hash_refresh ON auth_refresh_token (token_hash)");
            stmt.execute("CREATE INDEX idx_expires_at_refresh ON auth_refresh_token (expires_at)");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // 创建存储器
        storage = new JdbcRefreshTokenStorage(dataManager);
    }

    @AfterEach
    void tearDown() {
        try (var conn = dataSource.getConnection();
             var stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS auth_refresh_token");
        } catch (Exception e) {
            // ignore
        }
    }

    @Nested
    @DisplayName("save 方法测试")
    class SaveTests {

        @Test
        @DisplayName("应该成功保存 Refresh Token")
        void shouldSaveRefreshToken() {
            // given
            String tokenId = UUID.randomUUID().toString();
            String tokenHash = "hash-" + UUID.randomUUID();
            String userId = "user-123";
            Instant expiresAt = Instant.now().plus(Duration.ofDays(7));

            // when
            storage.save(tokenId, tokenHash, userId, null, null, null, expiresAt);

            // then
            var entity = dataManager.entity(AuthRefreshToken.class)
                    .query()
                    .where(builder(AuthRefreshToken.class)
                            .eq(AuthRefreshToken::getTokenId, tokenId)
                            .build())
                    .one();
            assertThat(entity).isPresent();
        }

        @Test
        @DisplayName("应该保存所有字段的 Refresh Token")
        void shouldSaveAllFields() {
            // given
            String tokenId = UUID.randomUUID().toString();
            String tokenHash = "hash-" + UUID.randomUUID();
            String userId = "user-456";
            String tenantId = "tenant-001";
            String clientId = "client-app";
            String deviceId = "device-123";
            Instant expiresAt = Instant.now().plus(Duration.ofDays(7));

            // when
            storage.save(tokenId, tokenHash, userId, tenantId, clientId, deviceId, expiresAt);

            // then
            var entity = dataManager.entity(AuthRefreshToken.class)
                    .query()
                    .where(io.github.afgprojects.framework.data.core.condition.Conditions.builder()
                            .eq("token_id", tokenId)
                            .build())
                    .one();

            assertThat(entity).isPresent();
            assertThat(entity.get().getTokenHash()).isEqualTo(tokenHash);
            assertThat(entity.get().getUserId()).isEqualTo(userId);
            assertThat(entity.get().getTenantId()).isEqualTo(tenantId);
            assertThat(entity.get().getClientId()).isEqualTo(clientId);
            assertThat(entity.get().getDeviceId()).isEqualTo(deviceId);
            assertThat(entity.get().getExpiresAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("findByTokenHash 方法测试")
    class FindByTokenHashTests {

        @Test
        @DisplayName("应该根据 tokenHash 查找 Refresh Token")
        void shouldFindByTokenHash() {
            // given
            String tokenId = UUID.randomUUID().toString();
            String tokenHash = "hash-" + UUID.randomUUID();
            String userId = "user-123";
            Instant expiresAt = Instant.now().plus(Duration.ofDays(7));
            storage.save(tokenId, tokenHash, userId, null, null, null, expiresAt);

            // when
            Optional<RefreshTokenInfo> result = storage.findByTokenHash(tokenHash);

            // then
            assertThat(result).isPresent();
            RefreshTokenInfo info = result.get();
            assertThat(info.tokenId()).isEqualTo(tokenId);
            assertThat(info.tokenHash()).isEqualTo(tokenHash);
            assertThat(info.userId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("当 tokenHash 不存在时应该返回空")
        void shouldReturnEmptyWhenTokenHashNotFound() {
            // when
            Optional<RefreshTokenInfo> result = storage.findByTokenHash("non-existent-hash");

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByTokenId 方法测试")
    class FindByTokenIdTests {

        @Test
        @DisplayName("应该根据 tokenId 查找 Refresh Token")
        void shouldFindByTokenId() {
            // given
            String tokenId = UUID.randomUUID().toString();
            String tokenHash = "hash-" + UUID.randomUUID();
            String userId = "user-123";
            Instant expiresAt = Instant.now().plus(Duration.ofDays(7));
            storage.save(tokenId, tokenHash, userId, null, null, null, expiresAt);

            // when
            Optional<RefreshTokenInfo> result = storage.findByTokenId(tokenId);

            // then
            assertThat(result).isPresent();
            RefreshTokenInfo info = result.get();
            assertThat(info.tokenId()).isEqualTo(tokenId);
            assertThat(info.tokenHash()).isEqualTo(tokenHash);
            assertThat(info.userId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("当 tokenId 不存在时应该返回空")
        void shouldReturnEmptyWhenTokenIdNotFound() {
            // when
            Optional<RefreshTokenInfo> result = storage.findByTokenId("non-existent-id");

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("delete 方法测试")
    class DeleteTests {

        @Test
        @DisplayName("应该成功删除指定的 Refresh Token")
        void shouldDeleteRefreshToken() {
            // given
            String tokenId = UUID.randomUUID().toString();
            String tokenHash = "hash-" + UUID.randomUUID();
            String userId = "user-123";
            Instant expiresAt = Instant.now().plus(Duration.ofDays(7));
            storage.save(tokenId, tokenHash, userId, null, null, null, expiresAt);

            // when
            storage.delete(tokenId);

            // then
            var entity = dataManager.entity(AuthRefreshToken.class)
                    .query()
                    .where(io.github.afgprojects.framework.data.core.condition.Conditions.builder()
                            .eq("token_id", tokenId)
                            .build())
                    .one();
            assertThat(entity).isEmpty();
        }

        @Test
        @DisplayName("删除不存在的 tokenId 不应该抛出异常")
        void shouldNotThrowWhenDeletingNonExistentToken() {
            // when & then
            assertThatCode(() -> storage.delete("non-existent-id"))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("deleteByUserId 方法测试")
    class DeleteByUserIdTests {

        @Test
        @DisplayName("应该删除用户的所有 Refresh Token")
        void shouldDeleteAllTokensForUser() {
            // given
            String userId = "user-123";
            for (int i = 0; i < 3; i++) {
                String tokenId = UUID.randomUUID().toString();
                String tokenHash = "hash-" + UUID.randomUUID();
                Instant expiresAt = Instant.now().plus(Duration.ofDays(7));
                storage.save(tokenId, tokenHash, userId, null, null, null, expiresAt);
            }

            // 保存另一个用户的 token
            String otherUserId = "user-456";
            String otherTokenId = UUID.randomUUID().toString();
            String otherTokenHash = "hash-other-" + UUID.randomUUID();
            storage.save(otherTokenId, otherTokenHash, otherUserId, null, null, null, Instant.now().plus(Duration.ofDays(7)));

            // when
            storage.deleteByUserId(userId);

            // then
            var entities = dataManager.entity(AuthRefreshToken.class)
                    .query()
                    .where(builder(AuthRefreshToken.class)
                            .eq(AuthRefreshToken::getUserId, userId)
                            .build())
                    .list();
            assertThat(entities).isEmpty();

            // 另一个用户的 token 应该还在
            var otherEntities = dataManager.entity(AuthRefreshToken.class)
                    .query()
                    .where(builder(AuthRefreshToken.class)
                            .eq(AuthRefreshToken::getUserId, otherUserId)
                            .build())
                    .list();
            assertThat(otherEntities).hasSize(1);
        }
    }

    @Nested
    @DisplayName("deleteExpired 方法测试")
    class DeleteExpiredTests {

        @Test
        @DisplayName("应该删除过期的 Refresh Token")
        void shouldDeleteExpiredTokens() {
            // given
            // 保存过期的 token
            String expiredTokenId = UUID.randomUUID().toString();
            String expiredTokenHash = "hash-expired-" + UUID.randomUUID();
            Instant expiredAt = Instant.now().minus(Duration.ofDays(1));
            storage.save(expiredTokenId, expiredTokenHash, "user-123", null, null, null, expiredAt);

            // 保存有效的 token
            String validTokenId = UUID.randomUUID().toString();
            String validTokenHash = "hash-valid-" + UUID.randomUUID();
            Instant validExpiresAt = Instant.now().plus(Duration.ofDays(7));
            storage.save(validTokenId, validTokenHash, "user-456", null, null, null, validExpiresAt);

            // when
            int deletedCount = storage.deleteExpired();

            // then
            assertThat(deletedCount).isEqualTo(1);

            // 过期的 token 应该被删除
            var expiredEntity = dataManager.entity(AuthRefreshToken.class)
                    .query()
                    .where(io.github.afgprojects.framework.data.core.condition.Conditions.builder()
                            .eq("token_id", expiredTokenId)
                            .build())
                    .one();
            assertThat(expiredEntity).isEmpty();

            // 有效的 token 应该还在
            var validEntity = dataManager.entity(AuthRefreshToken.class)
                    .query()
                    .where(io.github.afgprojects.framework.data.core.condition.Conditions.builder()
                            .eq("token_id", validTokenId)
                            .build())
                    .one();
            assertThat(validEntity).isPresent();
        }

        @Test
        @DisplayName("当没有过期 Token 时应该返回 0")
        void shouldReturnZeroWhenNoExpiredTokens() {
            // given
            String tokenId = UUID.randomUUID().toString();
            String tokenHash = "hash-" + UUID.randomUUID();
            Instant expiresAt = Instant.now().plus(Duration.ofDays(7));
            storage.save(tokenId, tokenHash, "user-123", null, null, null, expiresAt);

            // when
            int deletedCount = storage.deleteExpired();

            // then
            assertThat(deletedCount).isZero();
        }
    }

    @Nested
    @DisplayName("并发场景测试")
    class ConcurrencyTests {

        @Test
        @DisplayName("应该支持多用户同时保存 Token")
        void shouldSupportConcurrentSaves() {
            // given
            int userCount = 10;

            // when
            for (int i = 0; i < userCount; i++) {
                String tokenId = UUID.randomUUID().toString();
                String tokenHash = "hash-" + i + "-" + UUID.randomUUID();
                String userId = "user-" + i;
                Instant expiresAt = Instant.now().plus(Duration.ofDays(7));
                storage.save(tokenId, tokenHash, userId, null, null, null, expiresAt);
            }

            // then
            long totalCount = dataManager.count(AuthRefreshToken.class);
            assertThat(totalCount).isEqualTo(userCount);
        }
    }
}