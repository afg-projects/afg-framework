package io.github.afgprojects.framework.security.auth.storage;

import io.github.afgprojects.framework.data.jdbc.JdbcDataManager;
import io.github.afgprojects.framework.security.auth.entity.AuthCaptcha;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * JdbcCaptchaStorage 测试。
 */
@DisplayName("JdbcCaptchaStorage 测试")
class JdbcCaptchaStorageTest {

    private JdbcDataManager dataManager;
    private JdbcCaptchaStorage storage;
    private JdbcDataSource dataSource;

    @BeforeEach
    void setUp() {
        // 创建 H2 数据源
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:testdb_captcha;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        // 创建 DataManager
        dataManager = new JdbcDataManager(dataSource);

        // 创建表
        try (var conn = dataSource.getConnection();
             var stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS auth_captcha");
            stmt.execute("""
                CREATE TABLE auth_captcha (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    captcha_key VARCHAR(128) NOT NULL UNIQUE,
                    captcha_value VARCHAR(64) NOT NULL,
                    captcha_type VARCHAR(32),
                    target VARCHAR(256),
                    expires_at TIMESTAMP NOT NULL,
                    created_at TIMESTAMP NOT NULL,
                    updated_at TIMESTAMP
                )
                """);
            stmt.execute("CREATE INDEX idx_expires_at_captcha ON auth_captcha (expires_at)");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // 创建存储器
        storage = new JdbcCaptchaStorage(dataManager);
    }

    @AfterEach
    void tearDown() {
        try (var conn = dataSource.getConnection();
             var stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS auth_captcha");
        } catch (Exception e) {
            // ignore
        }
    }

    @Nested
    @DisplayName("save 方法测试")
    class SaveTests {

        @Test
        @DisplayName("应该成功保存验证码")
        void shouldSaveCaptcha() {
            // given
            String key = "session-123";
            String value = "ABCD";
            Duration ttl = Duration.ofMinutes(5);

            // when
            storage.save(key, value, ttl);

            // then
            var entity = dataManager.entity(AuthCaptcha.class)
                    .query()
                    .where(io.github.afgprojects.framework.data.core.condition.Conditions.builder()
                            .eq("captcha_key", key)
                            .build())
                    .one();
            assertThat(entity).isPresent();
        }

        @Test
        @DisplayName("应该保存所有字段的验证码")
        void shouldSaveAllFields() {
            // given
            String key = "session-complete";
            String value = "1234";
            Duration ttl = Duration.ofMinutes(10);

            // when
            storage.save(key, value, ttl);

            // then
            var entity = dataManager.entity(AuthCaptcha.class)
                    .query()
                    .where(io.github.afgprojects.framework.data.core.condition.Conditions.builder()
                            .eq("captcha_key", key)
                            .build())
                    .one();

            assertThat(entity).isPresent();
            assertThat(entity.get().getCaptchaKey()).isEqualTo(key);
            assertThat(entity.get().getCaptchaValue()).isEqualTo(value);
            assertThat(entity.get().getExpiresAt()).isNotNull();
            assertThat(entity.get().getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("应该支持更新已存在的验证码")
        void shouldUpdateExistingCaptcha() {
            // given
            String key = "session-update";
            String value1 = "1111";
            String value2 = "2222";
            Duration ttl = Duration.ofMinutes(5);

            // 先保存一次
            storage.save(key, value1, ttl);

            // when - 再次保存相同 key
            storage.save(key, value2, ttl);

            // then - 应该只有一条记录，且 value 已更新
            var entities = dataManager.entity(AuthCaptcha.class)
                    .query()
                    .where(io.github.afgprojects.framework.data.core.condition.Conditions.builder()
                            .eq("captcha_key", key)
                            .build())
                    .list();
            assertThat(entities).hasSize(1);
            assertThat(entities.get(0).getCaptchaValue()).isEqualTo(value2);
        }
    }

    @Nested
    @DisplayName("get 方法测试")
    class GetTests {

        @Test
        @DisplayName("应该成功获取验证码")
        void shouldGetCaptcha() {
            // given
            String key = "session-get";
            String value = "ABCD";
            Duration ttl = Duration.ofMinutes(5);
            storage.save(key, value, ttl);

            // when
            String result = storage.get(key);

            // then
            assertThat(result).isEqualTo(value);
        }

        @Test
        @DisplayName("当验证码不存在时应该返回 null")
        void shouldReturnNullWhenCaptchaNotFound() {
            // given
            String key = "session-nonexistent";

            // when
            String result = storage.get(key);

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("当验证码已过期时应该返回 null")
        void shouldReturnNullWhenCaptchaExpired() {
            // given
            String key = "session-expired-get";
            String value = "1234";
            Duration ttl = Duration.ofMillis(100);

            storage.save(key, value, ttl);

            // 等待过期
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // when
            String result = storage.get(key);

            // then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("delete 方法测试")
    class DeleteTests {

        @Test
        @DisplayName("应该成功删除验证码")
        void shouldDeleteCaptcha() {
            // given
            String key = "session-delete";
            String value = "ABCD";
            Duration ttl = Duration.ofMinutes(5);
            storage.save(key, value, ttl);

            // when
            storage.delete(key);

            // then
            var entity = dataManager.entity(AuthCaptcha.class)
                    .query()
                    .where(io.github.afgprojects.framework.data.core.condition.Conditions.builder()
                            .eq("captcha_key", key)
                            .build())
                    .one();
            assertThat(entity).isEmpty();
        }

        @Test
        @DisplayName("删除不存在的验证码不应该抛出异常")
        void shouldNotThrowWhenDeletingNonExistentCaptcha() {
            // given
            String key = "session-nonexistent-delete";

            // when & then
            assertThatCode(() -> storage.delete(key))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("exists 方法测试")
    class ExistsTests {

        @Test
        @DisplayName("应该正确识别存在的验证码")
        void shouldIdentifyExistingCaptcha() {
            // given
            String key = "session-exists";
            String value = "ABCD";
            Duration ttl = Duration.ofMinutes(5);
            storage.save(key, value, ttl);

            // when
            boolean result = storage.exists(key);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("应该正确识别不存在的验证码")
        void shouldIdentifyNonExistingCaptcha() {
            // given
            String key = "session-nonexistent-exists";

            // when
            boolean result = storage.exists(key);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("应该正确识别过期的验证码")
        void shouldIdentifyExpiredCaptcha() {
            // given
            String key = "session-expired-exists";
            String value = "1234";
            Duration ttl = Duration.ofMillis(100);

            storage.save(key, value, ttl);

            // 等待过期
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // when
            boolean result = storage.exists(key);

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("deleteExpired 方法测试")
    class DeleteExpiredTests {

        @Test
        @DisplayName("应该删除过期的验证码记录")
        void shouldDeleteExpiredRecords() {
            // given
            String key = "session-expired-delete-expired";
            String value = "1234";
            Duration ttl = Duration.ofMillis(100);

            storage.save(key, value, ttl);

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
            String key = "session-valid-delete-expired";
            String value = "ABCD";
            Duration ttl = Duration.ofMinutes(5);

            storage.save(key, value, ttl);

            // when
            int deletedCount = storage.deleteExpired();

            // then
            assertThat(deletedCount).isZero();
        }
    }

    @Nested
    @DisplayName("完整流程测试")
    class FullWorkflowTests {

        @Test
        @DisplayName("应该支持完整的验证码生命周期")
        void shouldSupportFullLifecycle() {
            // given
            String key = "session-full-lifecycle";
            String value = "ABCD";
            Duration ttl = Duration.ofMinutes(5);

            // when - 保存
            storage.save(key, value, ttl);

            // then - 存在
            assertThat(storage.exists(key)).isTrue();
            assertThat(storage.get(key)).isEqualTo(value);

            // when - 删除
            storage.delete(key);

            // then - 不存在
            assertThat(storage.exists(key)).isFalse();
            assertThat(storage.get(key)).isNull();
        }
    }
}
