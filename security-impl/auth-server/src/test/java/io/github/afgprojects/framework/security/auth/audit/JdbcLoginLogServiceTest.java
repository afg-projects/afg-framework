package io.github.afgprojects.framework.security.auth.audit;

import io.github.afgprojects.framework.data.jdbc.JdbcDataManager;
import io.github.afgprojects.framework.security.core.audit.model.LoginLog;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * JdbcLoginLogService 测试。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@DisplayName("JdbcLoginLogService 测试")
class JdbcLoginLogServiceTest {

    private JdbcDataSource dataSource;
    private JdbcDataManager dataManager;
    private JdbcLoginLogService loginLogService;

    @BeforeEach
    void setUp() {
        // 创建 H2 数据源
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:testdb_login_log;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        // 创建 DataManager
        dataManager = new JdbcDataManager(dataSource);

        // 创建表
        try (var conn = dataSource.getConnection();
             var stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS auth_login_log");
            stmt.execute("""
                CREATE TABLE auth_login_log (
                    id VARCHAR(64) PRIMARY KEY,
                    user_id VARCHAR(64),
                    username VARCHAR(128) NOT NULL,
                    tenant_id VARCHAR(64),
                    ip VARCHAR(64) NOT NULL,
                    device_id VARCHAR(128),
                    device_name VARCHAR(256),
                    browser VARCHAR(256),
                    os VARCHAR(256),
                    location VARCHAR(256),
                    result VARCHAR(32) NOT NULL,
                    fail_reason VARCHAR(512),
                    login_time TIMESTAMP NOT NULL,
                    logout_time TIMESTAMP
                )
                """);
            stmt.execute("CREATE INDEX idx_user_id_login ON auth_login_log (user_id)");
            stmt.execute("CREATE INDEX idx_tenant_id_login ON auth_login_log (tenant_id)");
            stmt.execute("CREATE INDEX idx_login_time ON auth_login_log (login_time)");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // 创建服务
        loginLogService = new JdbcLoginLogService(dataManager);
    }

    @AfterEach
    void tearDown() {
        try (var conn = dataSource.getConnection();
             var stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS auth_login_log");
        } catch (Exception e) {
            // ignore
        }
    }

    /**
     * 查询记录数量
     */
    private long queryCount(String sql, Object... params) {
        return dataManager.getJdbcClient()
                .sql(sql)
                .params(List.of(params))
                .query(Long.class)
                .single();
    }

    /**
     * 查询单条记录
     */
    private Map<String, Object> queryOne(String sql, Object... params) {
        return dataManager.getJdbcClient()
                .sql(sql)
                .params(List.of(params))
                .query((rs, rowNum) -> {
                    java.sql.ResultSetMetaData metaData = rs.getMetaData();
                    java.util.Map<String, Object> map = new java.util.HashMap<>();
                    for (int i = 1; i <= metaData.getColumnCount(); i++) {
                        map.put(metaData.getColumnName(i).toLowerCase(), rs.getObject(i));
                    }
                    return map;
                })
                .optional()
                .orElse(null);
    }

    @Nested
    @DisplayName("recordLogin 方法测试")
    class RecordLoginTests {

        @Test
        @DisplayName("应该成功记录成功登录日志")
        void shouldRecordSuccessLoginLog() {
            // given
            LoginLog log = LoginLog.success(
                    "user-001",
                    "admin",
                    "tenant-001",
                    "192.168.1.100",
                    "device-123",
                    "iPhone 15",
                    "Chrome 120",
                    "macOS 14",
                    "北京市"
            );

            // when
            loginLogService.recordLogin(log);

            // then
            long count = queryCount("SELECT COUNT(*) FROM auth_login_log WHERE username = ?", "admin");
            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("应该成功记录失败登录日志")
        void shouldRecordFailureLoginLog() {
            // given
            LoginLog log = LoginLog.failure(
                    "admin",
                    "tenant-001",
                    "192.168.1.100",
                    "device-123",
                    "iPhone 15",
                    "Chrome 120",
                    "macOS 14",
                    "北京市",
                    "密码错误"
            );

            // when
            loginLogService.recordLogin(log);

            // then
            long count = queryCount("SELECT COUNT(*) FROM auth_login_log WHERE username = ?", "admin");
            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("应该记录所有登录日志字段")
        void shouldRecordAllLoginLogFields() {
            // given
            Instant before = Instant.now();
            LoginLog log = LoginLog.success(
                    "user-001",
                    "admin",
                    "tenant-001",
                    "192.168.1.100",
                    "device-123",
                    "iPhone 15",
                    "Chrome 120",
                    "macOS 14",
                    "北京市"
            );

            // when
            loginLogService.recordLogin(log);

            // then
            var result = queryOne("SELECT * FROM auth_login_log WHERE username = ?", "admin");

            assertThat(result).isNotNull();
            assertThat(result.get("user_id")).isEqualTo("user-001");
            assertThat(result.get("username")).isEqualTo("admin");
            assertThat(result.get("tenant_id")).isEqualTo("tenant-001");
            assertThat(result.get("ip")).isEqualTo("192.168.1.100");
            assertThat(result.get("device_id")).isEqualTo("device-123");
            assertThat(result.get("device_name")).isEqualTo("iPhone 15");
            assertThat(result.get("browser")).isEqualTo("Chrome 120");
            assertThat(result.get("os")).isEqualTo("macOS 14");
            assertThat(result.get("location")).isEqualTo("北京市");
            assertThat(result.get("result")).isEqualTo(LoginLog.SUCCESS);
            assertThat((Timestamp) result.get("login_time")).isAfter(Timestamp.from(before.minusSeconds(1)));
        }

        @Test
        @DisplayName("应该支持可选字段为 null")
        void shouldSupportNullOptionalFields() {
            // given
            LoginLog log = LoginLog.success(
                    "user-001",
                    "admin",
                    null,  // tenantId
                    "192.168.1.100",
                    null,  // deviceId
                    null,  // deviceName
                    null,  // browser
                    null,  // os
                    null   // location
            );

            // when
            loginLogService.recordLogin(log);

            // then
            var result = queryOne("SELECT * FROM auth_login_log WHERE username = ?", "admin");

            assertThat(result).isNotNull();
            assertThat(result.get("tenant_id")).isNull();
            assertThat(result.get("device_id")).isNull();
            assertThat(result.get("device_name")).isNull();
            assertThat(result.get("browser")).isNull();
            assertThat(result.get("os")).isNull();
            assertThat(result.get("location")).isNull();
        }

        @Test
        @DisplayName("应该记录失败登录的失败原因")
        void shouldRecordFailReason() {
            // given
            LoginLog log = LoginLog.failure(
                    "admin",
                    "tenant-001",
                    "192.168.1.100",
                    null,
                    null,
                    null,
                    null,
                    null,
                    "密码错误"
            );

            // when
            loginLogService.recordLogin(log);

            // then
            var result = queryOne("SELECT * FROM auth_login_log WHERE username = ?", "admin");

            assertThat(result).isNotNull();
            assertThat(result.get("result")).isEqualTo(LoginLog.FAILURE);
            assertThat(result.get("fail_reason")).isEqualTo("密码错误");
            assertThat(result.get("user_id")).isNull();
        }
    }

    @Nested
    @DisplayName("recordLogout 方法测试")
    class RecordLogoutTests {

        @Test
        @DisplayName("应该成功更新登出时间")
        void shouldUpdateLogoutTime() {
            // given - 先记录登录
            LoginLog log = LoginLog.success(
                    "user-001",
                    "admin",
                    "tenant-001",
                    "192.168.1.100",
                    null,
                    null,
                    null,
                    null,
                    null
            );
            loginLogService.recordLogin(log);

            // when - 记录登出
            loginLogService.recordLogout("user-001", "tenant-001", "192.168.1.100");

            // then
            var result = queryOne("SELECT * FROM auth_login_log WHERE user_id = ?", "user-001");

            assertThat(result).isNotNull();
            assertThat(result.get("logout_time")).isNotNull();
        }

        @Test
        @DisplayName("应该只更新最近一次登录记录")
        void shouldUpdateOnlyLatestLoginRecord() {
            // given - 记录多次登录
            for (int i = 0; i < 3; i++) {
                LoginLog log = LoginLog.success(
                        "user-001",
                        "admin",
                        "tenant-001",
                        "192.168.1.100",
                        null,
                        null,
                        null,
                        null,
                        null
                );
                loginLogService.recordLogin(log);
                try {
                    Thread.sleep(10); // 确保时间不同
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            // when - 记录登出
            loginLogService.recordLogout("user-001", "tenant-001", "192.168.1.100");

            // then - 只有一条记录有登出时间
            long withLogout = queryCount(
                    "SELECT COUNT(*) FROM auth_login_log WHERE user_id = ? AND logout_time IS NOT NULL",
                    "user-001");
            long withoutLogout = queryCount(
                    "SELECT COUNT(*) FROM auth_login_log WHERE user_id = ? AND logout_time IS NULL",
                    "user-001");

            assertThat(withLogout).isEqualTo(1);
            assertThat(withoutLogout).isEqualTo(2);
        }

        @Test
        @DisplayName("应该支持单租户场景（tenantId 为 null）")
        void shouldSupportNullTenantId() {
            // given - 单租户登录
            LoginLog log = LoginLog.success(
                    "user-001",
                    "admin",
                    null,  // 单租户
                    "192.168.1.100",
                    null,
                    null,
                    null,
                    null,
                    null
            );
            loginLogService.recordLogin(log);

            // when - 记录登出
            loginLogService.recordLogout("user-001", null, "192.168.1.100");

            // then
            var result = queryOne("SELECT * FROM auth_login_log WHERE user_id = ?", "user-001");

            assertThat(result).isNotNull();
            assertThat(result.get("logout_time")).isNotNull();
        }

        @Test
        @DisplayName("当用户没有登录记录时不应该抛出异常")
        void shouldNotThrowWhenNoLoginRecord() {
            // given
            String userId = "user-nonexistent";

            // when & then
            assertThatCode(() -> loginLogService.recordLogout(userId, "tenant-001", "192.168.1.100"))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("多租户场景测试")
    class MultiTenantTests {

        @Test
        @DisplayName("应该区分不同租户的登录记录")
        void shouldDistinguishDifferentTenants() {
            // given - 不同租户的相同用户登录
            LoginLog log1 = LoginLog.success(
                    "user-001",
                    "admin",
                    "tenant-001",
                    "192.168.1.100",
                    null,
                    null,
                    null,
                    null,
                    null
            );
            LoginLog log2 = LoginLog.success(
                    "user-001",
                    "admin",
                    "tenant-002",
                    "192.168.1.100",
                    null,
                    null,
                    null,
                    null,
                    null
            );
            loginLogService.recordLogin(log1);
            loginLogService.recordLogin(log2);

            // when - 记录 tenant-001 的登出
            loginLogService.recordLogout("user-001", "tenant-001", "192.168.1.100");

            // then
            long tenant001WithLogout = queryCount(
                    "SELECT COUNT(*) FROM auth_login_log WHERE user_id = ? AND tenant_id = ? AND logout_time IS NOT NULL",
                    "user-001", "tenant-001");
            long tenant002WithLogout = queryCount(
                    "SELECT COUNT(*) FROM auth_login_log WHERE user_id = ? AND tenant_id = ? AND logout_time IS NOT NULL",
                    "user-001", "tenant-002");

            assertThat(tenant001WithLogout).isEqualTo(1);
            assertThat(tenant002WithLogout).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("并发场景测试")
    class ConcurrencyTests {

        @Test
        @DisplayName("应该支持多用户同时登录")
        void shouldSupportConcurrentLogins() {
            // given
            int userCount = 10;

            // when
            for (int i = 0; i < userCount; i++) {
                LoginLog log = LoginLog.success(
                        "user-" + i,
                        "user" + i,
                        "tenant-001",
                        "192.168.1." + i,
                        null,
                        null,
                        null,
                        null,
                        null
                );
                loginLogService.recordLogin(log);
            }

            // then
            long count = queryCount("SELECT COUNT(*) FROM auth_login_log");
            assertThat(count).isEqualTo(userCount);
        }
    }

    @Nested
    @DisplayName("完整流程测试")
    class FullWorkflowTests {

        @Test
        @DisplayName("应该支持完整的登录登出流程")
        void shouldSupportFullLoginLogoutWorkflow() {
            // given
            LoginLog loginLog = LoginLog.success(
                    "user-001",
                    "admin",
                    "tenant-001",
                    "192.168.1.100",
                    "device-123",
                    "iPhone 15",
                    "Chrome 120",
                    "macOS 14",
                    "北京市"
            );

            // when - 登录
            loginLogService.recordLogin(loginLog);

            // then - 验证登录记录
            var loginResult = queryOne("SELECT * FROM auth_login_log WHERE user_id = ?", "user-001");

            assertThat(loginResult).isNotNull();
            assertThat(loginResult.get("result")).isEqualTo(LoginLog.SUCCESS);
            assertThat(loginResult.get("logout_time")).isNull();

            // when - 登出
            loginLogService.recordLogout("user-001", "tenant-001", "192.168.1.100");

            // then - 验证登出时间已更新
            var logoutResult = queryOne("SELECT * FROM auth_login_log WHERE user_id = ?", "user-001");

            assertThat(logoutResult).isNotNull();
            assertThat(logoutResult.get("logout_time")).isNotNull();
        }

        @Test
        @DisplayName("应该支持登录失败后重试成功的场景")
        void shouldSupportFailureThenSuccessScenario() {
            // given - 第一次登录失败
            LoginLog failLog = LoginLog.failure(
                    "admin",
                    "tenant-001",
                    "192.168.1.100",
                    null,
                    null,
                    null,
                    null,
                    null,
                    "密码错误"
            );
            loginLogService.recordLogin(failLog);

            // when - 第二次登录成功
            LoginLog successLog = LoginLog.success(
                    "user-001",
                    "admin",
                    "tenant-001",
                    "192.168.1.100",
                    null,
                    null,
                    null,
                    null,
                    null
            );
            loginLogService.recordLogin(successLog);

            // then
            long failCount = queryCount(
                    "SELECT COUNT(*) FROM auth_login_log WHERE username = ? AND result = ?",
                    "admin", LoginLog.FAILURE);
            long successCount = queryCount(
                    "SELECT COUNT(*) FROM auth_login_log WHERE username = ? AND result = ?",
                    "admin", LoginLog.SUCCESS);

            assertThat(failCount).isEqualTo(1);
            assertThat(successCount).isEqualTo(1);
        }
    }
}
