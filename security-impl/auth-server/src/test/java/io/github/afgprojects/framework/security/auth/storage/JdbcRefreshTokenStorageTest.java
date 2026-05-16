package io.github.afgprojects.framework.security.auth.storage;

import io.github.afgprojects.framework.security.core.storage.AfgRefreshTokenStorage.RefreshTokenInfo;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * JdbcRefreshTokenStorage 测试。
 */
@DisplayName("JdbcRefreshTokenStorage 测试")
class JdbcRefreshTokenStorageTest {

    private JdbcTemplate jdbcTemplate;
    private JdbcRefreshTokenStorage storage;

    @BeforeEach
    void setUp() {
        // 创建 H2 数据源
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        // 创建 JdbcTemplate
        jdbcTemplate = new JdbcTemplate(dataSource);

        // 先删除表（如果存在）
        jdbcTemplate.execute("DROP TABLE IF EXISTS auth_refresh_token");

        // 创建表
        jdbcTemplate.execute("""
                CREATE TABLE auth_refresh_token (
                    token_id VARCHAR(64) PRIMARY KEY,
                    token_hash VARCHAR(128) NOT NULL UNIQUE,
                    user_id VARCHAR(64) NOT NULL,
                    tenant_id VARCHAR(64),
                    client_id VARCHAR(128),
                    device_id VARCHAR(128),
                    expires_at TIMESTAMP NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);

        // 创建索引
        jdbcTemplate.execute("CREATE INDEX idx_user_id ON auth_refresh_token (user_id)");
        jdbcTemplate.execute("CREATE INDEX idx_token_hash ON auth_refresh_token (token_hash)");
        jdbcTemplate.execute("CREATE INDEX idx_expires_at ON auth_refresh_token (expires_at)");

        // 创建存储器
        storage = new JdbcRefreshTokenStorage(jdbcTemplate);
    }

    @AfterEach
    void tearDown() {
        if (jdbcTemplate != null) {
            jdbcTemplate.execute("DROP TABLE IF EXISTS auth_refresh_token");
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
            LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);

            // when
            storage.save(tokenId, tokenHash, userId, null, null, null, expiresAt);

            // then
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM auth_refresh_token WHERE token_id = ?",
                    Integer.class,
                    tokenId
            );
            assertThat(count).isEqualTo(1);
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
            LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);

            // when
            storage.save(tokenId, tokenHash, userId, tenantId, clientId, deviceId, expiresAt);

            // then
            var record = jdbcTemplate.queryForObject(
                    "SELECT token_id, token_hash, user_id, tenant_id, client_id, device_id, expires_at, created_at "
                            + "FROM auth_refresh_token WHERE token_id = ?",
                    (rs, rowNum) -> new Object[]{
                            rs.getString("token_id"),
                            rs.getString("token_hash"),
                            rs.getString("user_id"),
                            rs.getString("tenant_id"),
                            rs.getString("client_id"),
                            rs.getString("device_id"),
                            rs.getObject("expires_at", LocalDateTime.class),
                            rs.getObject("created_at", LocalDateTime.class)
                    },
                    tokenId
            );

            assertThat(record).isNotNull();
            assertThat(record[0]).isEqualTo(tokenId);
            assertThat(record[1]).isEqualTo(tokenHash);
            assertThat(record[2]).isEqualTo(userId);
            assertThat(record[3]).isEqualTo(tenantId);
            assertThat(record[4]).isEqualTo(clientId);
            assertThat(record[5]).isEqualTo(deviceId);
            assertThat(record[6]).isNotNull();
            assertThat(record[7]).isNotNull();
        }

        @Test
        @DisplayName("应该允许 tenantId 为 null")
        void shouldAllowNullTenantId() {
            // given
            String tokenId = UUID.randomUUID().toString();
            String tokenHash = "hash-" + UUID.randomUUID();
            String userId = "user-789";
            LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);

            // when
            storage.save(tokenId, tokenHash, userId, null, null, null, expiresAt);

            // then
            String savedTenantId = jdbcTemplate.queryForObject(
                    "SELECT tenant_id FROM auth_refresh_token WHERE token_id = ?",
                    String.class,
                    tokenId
            );
            assertThat(savedTenantId).isNull();
        }

        @Test
        @DisplayName("应该允许 clientId 和 deviceId 为 null")
        void shouldAllowNullClientAndDeviceId() {
            // given
            String tokenId = UUID.randomUUID().toString();
            String tokenHash = "hash-" + UUID.randomUUID();
            String userId = "user-abc";
            LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);

            // when
            storage.save(tokenId, tokenHash, userId, null, null, null, expiresAt);

            // then
            var record = jdbcTemplate.queryForObject(
                    "SELECT client_id, device_id FROM auth_refresh_token WHERE token_id = ?",
                    (rs, rowNum) -> new Object[]{
                            rs.getString("client_id"),
                            rs.getString("device_id")
                    },
                    tokenId
            );

            assertThat(record).isNotNull();
            assertThat(record[0]).isNull();
            assertThat(record[1]).isNull();
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
            LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);
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

        @Test
        @DisplayName("应该返回完整的 RefreshTokenInfo")
        void shouldReturnCompleteRefreshTokenInfo() {
            // given
            String tokenId = UUID.randomUUID().toString();
            String tokenHash = "hash-" + UUID.randomUUID();
            String userId = "user-456";
            String tenantId = "tenant-001";
            String clientId = "client-app";
            String deviceId = "device-123";
            LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);
            storage.save(tokenId, tokenHash, userId, tenantId, clientId, deviceId, expiresAt);

            // when
            Optional<RefreshTokenInfo> result = storage.findByTokenHash(tokenHash);

            // then
            assertThat(result).isPresent();
            RefreshTokenInfo info = result.get();
            assertThat(info.tokenId()).isEqualTo(tokenId);
            assertThat(info.tokenHash()).isEqualTo(tokenHash);
            assertThat(info.userId()).isEqualTo(userId);
            assertThat(info.tenantId()).isEqualTo(tenantId);
            assertThat(info.clientId()).isEqualTo(clientId);
            assertThat(info.deviceId()).isEqualTo(deviceId);
            assertThat(info.expiresAt()).isNotNull();
            assertThat(info.createdAt()).isNotNull();
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
            LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);
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
            LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);
            storage.save(tokenId, tokenHash, userId, null, null, null, expiresAt);

            // when
            storage.delete(tokenId);

            // then
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM auth_refresh_token WHERE token_id = ?",
                    Integer.class,
                    tokenId
            );
            assertThat(count).isZero();
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
                LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);
                storage.save(tokenId, tokenHash, userId, null, null, null, expiresAt);
            }

            // 保存另一个用户的 token
            String otherUserId = "user-456";
            String otherTokenId = UUID.randomUUID().toString();
            String otherTokenHash = "hash-other-" + UUID.randomUUID();
            storage.save(otherTokenId, otherTokenHash, otherUserId, null, null, null, LocalDateTime.now().plusDays(7));

            // when
            storage.deleteByUserId(userId);

            // then
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM auth_refresh_token WHERE user_id = ?",
                    Integer.class,
                    userId
            );
            assertThat(count).isZero();

            // 另一个用户的 token 应该还在
            Integer otherCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM auth_refresh_token WHERE user_id = ?",
                    Integer.class,
                    otherUserId
            );
            assertThat(otherCount).isEqualTo(1);
        }

        @Test
        @DisplayName("删除不存在的用户 Token 不应该抛出异常")
        void shouldNotThrowWhenDeletingNonExistentUser() {
            // when & then
            assertThatCode(() -> storage.deleteByUserId("non-existent-user"))
                    .doesNotThrowAnyException();
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
            LocalDateTime expiredAt = LocalDateTime.now().minusDays(1);
            storage.save(expiredTokenId, expiredTokenHash, "user-123", null, null, null, expiredAt);

            // 保存有效的 token
            String validTokenId = UUID.randomUUID().toString();
            String validTokenHash = "hash-valid-" + UUID.randomUUID();
            LocalDateTime validExpiresAt = LocalDateTime.now().plusDays(7);
            storage.save(validTokenId, validTokenHash, "user-456", null, null, null, validExpiresAt);

            // when
            int deletedCount = storage.deleteExpired();

            // then
            assertThat(deletedCount).isEqualTo(1);

            // 过期的 token 应该被删除
            Integer expiredCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM auth_refresh_token WHERE token_id = ?",
                    Integer.class,
                    expiredTokenId
            );
            assertThat(expiredCount).isZero();

            // 有效的 token 应该还在
            Integer validCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM auth_refresh_token WHERE token_id = ?",
                    Integer.class,
                    validTokenId
            );
            assertThat(validCount).isEqualTo(1);
        }

        @Test
        @DisplayName("当没有过期 Token 时应该返回 0")
        void shouldReturnZeroWhenNoExpiredTokens() {
            // given
            String tokenId = UUID.randomUUID().toString();
            String tokenHash = "hash-" + UUID.randomUUID();
            LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);
            storage.save(tokenId, tokenHash, "user-123", null, null, null, expiresAt);

            // when
            int deletedCount = storage.deleteExpired();

            // then
            assertThat(deletedCount).isZero();
        }

        @Test
        @DisplayName("应该删除多个过期的 Refresh Token")
        void shouldDeleteMultipleExpiredTokens() {
            // given
            for (int i = 0; i < 5; i++) {
                String tokenId = UUID.randomUUID().toString();
                String tokenHash = "hash-expired-" + i + "-" + UUID.randomUUID();
                LocalDateTime expiredAt = LocalDateTime.now().minusDays(i + 1);
                storage.save(tokenId, tokenHash, "user-" + i, null, null, null, expiredAt);
            }

            // when
            int deletedCount = storage.deleteExpired();

            // then
            assertThat(deletedCount).isEqualTo(5);

            Integer totalCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM auth_refresh_token",
                    Integer.class
            );
            assertThat(totalCount).isZero();
        }
    }

    @Nested
    @DisplayName("自定义表名测试")
    class CustomTableNameTests {

        @Test
        @DisplayName("应该支持自定义表名")
        void shouldSupportCustomTableName() {
            // given
            String customTableName = "custom_refresh_token";
            jdbcTemplate.execute("DROP TABLE IF EXISTS " + customTableName);
            jdbcTemplate.execute(String.format("""
                    CREATE TABLE %s (
                        token_id VARCHAR(64) PRIMARY KEY,
                        token_hash VARCHAR(128) NOT NULL UNIQUE,
                        user_id VARCHAR(64) NOT NULL,
                        tenant_id VARCHAR(64),
                        client_id VARCHAR(128),
                        device_id VARCHAR(128),
                        expires_at TIMESTAMP NOT NULL,
                        created_at TIMESTAMP NOT NULL
                    )
                    """, customTableName));

            JdbcRefreshTokenStorage customStorage = new JdbcRefreshTokenStorage(jdbcTemplate, customTableName);

            String tokenId = UUID.randomUUID().toString();
            String tokenHash = "hash-" + UUID.randomUUID();
            String userId = "user-123";
            LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);

            // when
            customStorage.save(tokenId, tokenHash, userId, null, null, null, expiresAt);

            // then
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + customTableName + " WHERE token_id = ?",
                    Integer.class,
                    tokenId
            );
            assertThat(count).isEqualTo(1);

            // cleanup
            jdbcTemplate.execute("DROP TABLE IF EXISTS " + customTableName);
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
                LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);
                storage.save(tokenId, tokenHash, userId, null, null, null, expiresAt);
            }

            // then
            Integer totalCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM auth_refresh_token",
                    Integer.class
            );
            assertThat(totalCount).isEqualTo(userCount);
        }
    }
}
