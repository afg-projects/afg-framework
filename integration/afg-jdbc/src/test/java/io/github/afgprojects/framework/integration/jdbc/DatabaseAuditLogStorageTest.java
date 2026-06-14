package io.github.afgprojects.framework.integration.jdbc;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import org.springframework.jdbc.core.JdbcTemplate;

import io.github.afgprojects.framework.core.audit.AuditLog;
import io.github.afgprojects.framework.integration.jdbc.audit.DatabaseAuditLogProperties;
import io.github.afgprojects.framework.integration.jdbc.audit.DatabaseAuditLogStorage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * DatabaseAuditLogStorage 集成测试
 *
 * <p>基于真实 PostgreSQL 容器测试数据库审计日志存储
 */
@Testcontainers
@DisplayName("DatabaseAuditLogStorage 数据库审计日志测试")
class DatabaseAuditLogStorageTest {

    private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:16-alpine");

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(POSTGRES_IMAGE)
            .withDatabaseName("afg_test")
            .withUsername("test")
            .withPassword("test");

    private JdbcTemplate jdbcTemplate;
    private DatabaseAuditLogStorage auditLogStorage;

    @BeforeAll
    static void verifyContainer() {
        assertThat(POSTGRES.isRunning()).isTrue();
    }

    @BeforeEach
    void setUp() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(POSTGRES.getJdbcUrl());
        hikariConfig.setUsername(POSTGRES.getUsername());
        hikariConfig.setPassword(POSTGRES.getPassword());
        DataSource dataSource = new HikariDataSource(hikariConfig);
        jdbcTemplate = new JdbcTemplate(dataSource);

        // 创建审计日志表
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS audit_log (
                id VARCHAR(36) PRIMARY KEY,
                trace_id VARCHAR(64),
                request_id VARCHAR(64),
                user_id BIGINT,
                username VARCHAR(128),
                tenant_id BIGINT,
                module VARCHAR(128),
                operation VARCHAR(256),
                target VARCHAR(512),
                class_name VARCHAR(256),
                method_name VARCHAR(128),
                args TEXT,
                old_value TEXT,
                new_value TEXT,
                result VARCHAR(16),
                error_message TEXT,
                client_ip VARCHAR(64),
                timestamp TIMESTAMP,
                duration_ms BIGINT
            )
        """);

        // 使用同步模式（asyncEnabled=false），方便测试验证
        DatabaseAuditLogProperties properties = new DatabaseAuditLogProperties();
        properties.setAsyncEnabled(false);
        properties.setTableName("audit_log");

        auditLogStorage = new DatabaseAuditLogStorage(jdbcTemplate, properties);
    }

    @Nested
    @DisplayName("save 操作（同步模式）")
    class SaveSync {

        @Test
        @DisplayName("save 应成功保存审计日志到数据库")
        void shouldSaveAuditLog() {
            AuditLog log = createAuditLog("save-test-1");

            assertThatCode(() -> auditLogStorage.save(log)).doesNotThrowAnyException();

            // 验证数据已写入
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM audit_log WHERE id = ?", Integer.class, "save-test-1");
            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("save 应正确存储所有字段")
        void shouldStoreAllFields() {
            AuditLog log = AuditLog.successBuilder()
                    .id("field-test-1")
                    .userId(123L)
                    .username("testuser")
                    .tenantId(1L)
                    .operation("CREATE")
                    .module("user")
                    .target("User:456")
                    .className("UserService")
                    .methodName("createUser")
                    .args("{\"name\":\"test\"}")
                    .oldValue(null)
                    .newValue("{\"name\":\"test\"}")
                    .traceId("trace-abc")
                    .requestId("req-123")
                    .clientIp("127.0.0.1")
                    .durationMs(150)
                    .build();

            auditLogStorage.save(log);

            // 验证关键字段
            String username = jdbcTemplate.queryForObject(
                    "SELECT username FROM audit_log WHERE id = ?", String.class, "field-test-1");
            assertThat(username).isEqualTo("testuser");

            String operation = jdbcTemplate.queryForObject(
                    "SELECT operation FROM audit_log WHERE id = ?", String.class, "field-test-1");
            assertThat(operation).isEqualTo("CREATE");

            Long userId = jdbcTemplate.queryForObject(
                    "SELECT user_id FROM audit_log WHERE id = ?", Long.class, "field-test-1");
            assertThat(userId).isEqualTo(123L);

            String result = jdbcTemplate.queryForObject(
                    "SELECT result FROM audit_log WHERE id = ?", String.class, "field-test-1");
            assertThat(result).isEqualTo("SUCCESS");
        }

        @Test
        @DisplayName("save 应支持 FAILURE 结果的审计日志")
        void shouldSaveFailureAuditLog() {
            AuditLog log = AuditLog.failureBuilder()
                    .id("failure-test-1")
                    .operation("DELETE")
                    .module("order")
                    .className("OrderService")
                    .methodName("deleteOrder")
                    .errorMessage("Order not found")
                    .build();

            auditLogStorage.save(log);

            String result = jdbcTemplate.queryForObject(
                    "SELECT result FROM audit_log WHERE id = ?", String.class, "failure-test-1");
            assertThat(result).isEqualTo("FAILURE");

            String errorMsg = jdbcTemplate.queryForObject(
                    "SELECT error_message FROM audit_log WHERE id = ?", String.class, "failure-test-1");
            assertThat(errorMsg).isEqualTo("Order not found");
        }

        @Test
        @DisplayName("save 多条审计日志应全部写入数据库")
        void shouldSaveMultipleLogs() {
            for (int i = 0; i < 5; i++) {
                auditLogStorage.save(createAuditLog("multi-test-" + i));
            }

            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM audit_log WHERE id LIKE 'multi-test-%'", Integer.class);
            assertThat(count).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("shutdown 操作")
    class Shutdown {

        @Test
        @DisplayName("shutdown 后应不再保存新的审计日志")
        void shouldNotSaveAfterShutdown() {
            // 先用同步模式保存一条
            auditLogStorage.save(createAuditLog("before-shutdown"));

            // shutdown
            auditLogStorage.shutdown();

            // shutdown 后再保存
            auditLogStorage.save(createAuditLog("after-shutdown"));

            // 只有 before-shutdown 应该在数据库中
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM audit_log", Integer.class);
            assertThat(count).isEqualTo(1);
        }
    }

    private AuditLog createAuditLog(String id) {
        return AuditLog.successBuilder()
                .id(id)
                .operation("TEST")
                .module("test")
                .className("TestClass")
                .methodName("testMethod")
                .build();
    }

    @AfterEach
    void cleanup() {
        try {
            jdbcTemplate.execute("DROP TABLE IF EXISTS audit_log");
        } catch (Exception ignored) {
            // ignore cleanup errors
        }
    }
}
