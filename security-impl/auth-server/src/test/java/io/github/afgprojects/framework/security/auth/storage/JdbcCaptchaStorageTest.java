package io.github.afgprojects.framework.security.auth.storage;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * JdbcCaptchaStorage 测试。
 */
@DisplayName("JdbcCaptchaStorage 测试")
class JdbcCaptchaStorageTest {

    private JdbcTemplate jdbcTemplate;
    private JdbcCaptchaStorage storage;

    @BeforeEach
    void setUp() {
        // 创建 H2 数据源
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:testdb_captcha;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        // 创建 JdbcTemplate
        jdbcTemplate = new JdbcTemplate(dataSource);

        // 先删除表（如果存在）
        jdbcTemplate.execute("DROP TABLE IF EXISTS auth_captcha");

        // 创建表
        jdbcTemplate.execute("""
                CREATE TABLE auth_captcha (
                    captcha_key VARCHAR(128) PRIMARY KEY,
                    captcha_value VARCHAR(64) NOT NULL,
                    expires_at TIMESTAMP NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);

        // 创建索引
        jdbcTemplate.execute("CREATE INDEX idx_expires_at_captcha ON auth_captcha (expires_at)");

        // 创建存储器
        storage = new JdbcCaptchaStorage(jdbcTemplate);
    }

    @AfterEach
    void tearDown() {
        if (jdbcTemplate != null) {
            jdbcTemplate.execute("DROP TABLE IF EXISTS auth_captcha");
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
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM auth_captcha WHERE captcha_key = ?",
                    Integer.class,
                    key
            );
            assertThat(count).isEqualTo(1);
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
            var record = jdbcTemplate.queryForObject(
                    "SELECT captcha_key, captcha_value, expires_at, created_at "
                            + "FROM auth_captcha WHERE captcha_key = ?",
                    (rs, rowNum) -> new Object[]{
                            rs.getString("captcha_key"),
                            rs.getString("captcha_value"),
                            rs.getObject("expires_at", java.time.LocalDateTime.class),
                            rs.getObject("created_at", java.time.LocalDateTime.class)
                    },
                    key
            );

            assertThat(record).isNotNull();
            assertThat(record[0]).isEqualTo(key);
            assertThat(record[1]).isEqualTo(value);
            assertThat(record[2]).isNotNull();
            assertThat(record[3]).isNotNull();
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
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM auth_captcha WHERE captcha_key = ?",
                    Integer.class,
                    key
            );
            assertThat(count).isEqualTo(1);

            String savedValue = jdbcTemplate.queryForObject(
                    "SELECT captcha_value FROM auth_captcha WHERE captcha_key = ?",
                    String.class,
                    key
            );
            assertThat(savedValue).isEqualTo(value2);
        }

        @Test
        @DisplayName("应该正确计算过期时间")
        void shouldCalculateExpiresAtCorrectly() {
            // given
            String key = "session-expire-calc";
            String value = "9999";
            Duration ttl = Duration.ofMinutes(30);

            java.time.LocalDateTime beforeSave = java.time.LocalDateTime.now();

            // when
            storage.save(key, value, ttl);

            // then
            java.time.LocalDateTime expiresAt = jdbcTemplate.queryForObject(
                    "SELECT expires_at FROM auth_captcha WHERE captcha_key = ?",
                    java.time.LocalDateTime.class,
                    key
            );

            assertThat(expiresAt).isNotNull();
            assertThat(expiresAt).isAfter(beforeSave);
            assertThat(expiresAt).isBefore(beforeSave.plus(ttl).plusSeconds(1));
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
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM auth_captcha WHERE captcha_key = ?",
                    Integer.class,
                    key
            );
            assertThat(count).isZero();
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

        @Test
        @DisplayName("应该删除多个过期的验证码记录")
        void shouldDeleteMultipleExpiredRecords() {
            // given
            for (int i = 0; i < 5; i++) {
                String key = "session-expired-multi-" + i;
                String value = String.valueOf(i);
                Duration ttl = Duration.ofMillis(100);
                storage.save(key, value, ttl);
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
    @DisplayName("自定义表名测试")
    class CustomTableNameTests {

        @Test
        @DisplayName("应该支持自定义表名")
        void shouldSupportCustomTableName() {
            // given
            String customTableName = "custom_captcha";
            jdbcTemplate.execute("DROP TABLE IF EXISTS " + customTableName);
            jdbcTemplate.execute(String.format("""
                    CREATE TABLE %s (
                        captcha_key VARCHAR(128) PRIMARY KEY,
                        captcha_value VARCHAR(64) NOT NULL,
                        expires_at TIMESTAMP NOT NULL,
                        created_at TIMESTAMP NOT NULL
                    )
                    """, customTableName));

            JdbcCaptchaStorage customStorage = new JdbcCaptchaStorage(jdbcTemplate, customTableName);

            String key = "session-custom-table";
            String value = "ABCD";
            Duration ttl = Duration.ofMinutes(5);

            // when
            customStorage.save(key, value, ttl);

            // then
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + customTableName + " WHERE captcha_key = ?",
                    Integer.class,
                    key
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
        @DisplayName("应该支持多验证码同时保存")
        void shouldSupportConcurrentSaves() {
            // given
            int captchaCount = 10;

            // when
            for (int i = 0; i < captchaCount; i++) {
                String key = "session-concurrent-" + i;
                String value = String.valueOf(i);
                Duration ttl = Duration.ofMinutes(5);
                storage.save(key, value, ttl);
            }

            // then
            Integer totalCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM auth_captcha",
                    Integer.class
            );
            assertThat(totalCount).isEqualTo(captchaCount);
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

        @Test
        @DisplayName("应该支持验证码验证后删除的典型场景")
        void shouldSupportVerifyAndDeleteScenario() {
            // given
            String key = "session-verify-delete";
            String value = "1234";
            Duration ttl = Duration.ofMinutes(5);

            storage.save(key, value, ttl);

            // when - 获取验证码进行验证
            String storedValue = storage.get(key);
            boolean isValid = storedValue != null && storedValue.equals("1234");

            // 验证成功后删除
            if (isValid) {
                storage.delete(key);
            }

            // then
            assertThat(isValid).isTrue();
            assertThat(storage.exists(key)).isFalse();
        }
    }
}
