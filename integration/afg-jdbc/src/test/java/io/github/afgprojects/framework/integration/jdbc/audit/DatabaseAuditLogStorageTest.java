package io.github.afgprojects.framework.integration.jdbc.audit;

import java.time.LocalDateTime;
import java.util.UUID;

import io.github.afgprojects.framework.core.audit.AuditLog;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DatabaseAuditLogStorage 测试
 */
class DatabaseAuditLogStorageTest {

    private JdbcTemplate jdbcTemplate;
    private DatabaseAuditLogStorage storage;

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
        jdbcTemplate.execute("DROP TABLE IF EXISTS audit_log");

        // 创建表
        jdbcTemplate.execute("""
                CREATE TABLE audit_log (
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

        // 创建配置
        DatabaseAuditLogProperties properties = new DatabaseAuditLogProperties();
        properties.setTableName("audit_log");
        properties.setAsyncEnabled(false); // 测试时使用同步模式

        // 创建存储器
        storage = new DatabaseAuditLogStorage(jdbcTemplate, properties);
    }

    @AfterEach
    void tearDown() {
        if (storage != null) {
            storage.shutdown();
        }
    }

    @Test
    @DisplayName("保存审计日志成功")
    void testSaveAuditLog() {
        // 创建审计日志
        AuditLog auditLog = createAuditLog();

        // 保存
        storage.save(auditLog);

        // 验证数据已保存
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE id = ?",
                Integer.class,
                auditLog.id());

        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("保存带所有字段的审计日志")
    void testSaveAuditLogWithAllFields() {
        AuditLog auditLog = AuditLog.successBuilder()
                .id(UUID.randomUUID().toString())
                .userId(100L)
                .username("admin")
                .tenantId(1L)
                .operation("CREATE_USER")
                .module("user")
                .target("user:123")
                .className("UserService")
                .methodName("createUser")
                .args("{\"name\":\"John\"}")
                .oldValue("{\"name\":\"John\"}")
                .newValue("{\"name\":\"Jane\"}")
                .traceId("trace-123")
                .requestId("req-456")
                .clientIp("192.168.1.1")
                .timestamp(LocalDateTime.now())
                .durationMs(100L)
                .build();

        storage.save(auditLog);

        // 验证所有字段
        var record = jdbcTemplate.queryForObject(
                """
                SELECT trace_id, request_id, user_id, username, tenant_id, module, operation,
                       target, class_name, method_name, args, old_value, new_value, result,
                       error_message, client_ip, duration_ms
                FROM audit_log WHERE id = ?
                """,
                (rs, rowNum) -> new Object[]{
                        rs.getString("trace_id"),
                        rs.getString("request_id"),
                        rs.getLong("user_id"),
                        rs.getString("username"),
                        rs.getLong("tenant_id"),
                        rs.getString("module"),
                        rs.getString("operation"),
                        rs.getString("target"),
                        rs.getString("class_name"),
                        rs.getString("method_name"),
                        rs.getString("args"),
                        rs.getString("old_value"),
                        rs.getString("new_value"),
                        rs.getString("result"),
                        rs.getString("error_message"),
                        rs.getString("client_ip"),
                        rs.getLong("duration_ms")
                },
                auditLog.id());

        assertThat(record).isNotNull();
        assertThat(record[0]).isEqualTo("trace-123");
        assertThat(record[1]).isEqualTo("req-456");
        assertThat(record[2]).isEqualTo(100L);
        assertThat(record[3]).isEqualTo("admin");
        assertThat(record[4]).isEqualTo(1L);
        assertThat(record[5]).isEqualTo("user");
        assertThat(record[6]).isEqualTo("CREATE_USER");
        assertThat(record[7]).isEqualTo("user:123");
        assertThat(record[8]).isEqualTo("UserService");
        assertThat(record[9]).isEqualTo("createUser");
        assertThat(record[10]).isEqualTo("{\"name\":\"John\"}");
        assertThat(record[13]).isEqualTo("SUCCESS");
        assertThat(record[16]).isEqualTo(100L);
    }

    @Test
    @DisplayName("保存失败日志")
    void testSaveFailureAuditLog() {
        AuditLog auditLog = AuditLog.failureBuilder()
                .id(UUID.randomUUID().toString())
                .operation("DELETE_USER")
                .module("user")
                .className("UserService")
                .methodName("deleteUser")
                .errorMessage("User not found")
                .timestamp(LocalDateTime.now())
                .durationMs(50L)
                .build();

        storage.save(auditLog);

        // 验证失败标记
        String result = jdbcTemplate.queryForObject(
                "SELECT result FROM audit_log WHERE id = ?",
                String.class,
                auditLog.id());

        assertThat(result).isEqualTo("FAILURE");

        String errorMessage = jdbcTemplate.queryForObject(
                "SELECT error_message FROM audit_log WHERE id = ?",
                String.class,
                auditLog.id());

        assertThat(errorMessage).isEqualTo("User not found");
    }

    @Test
    @DisplayName("异步批量保存审计日志")
    void testAsyncBatchSave() throws InterruptedException {
        // 创建异步配置
        DatabaseAuditLogProperties properties = new DatabaseAuditLogProperties();
        properties.setTableName("audit_log");
        properties.setAsyncEnabled(true);
        properties.setBatchSize(5);
        properties.setFlushIntervalMs(1000);
        properties.setQueueCapacity(100);

        DatabaseAuditLogStorage asyncStorage = new DatabaseAuditLogStorage(jdbcTemplate, properties);

        // 保存多条日志
        for (int i = 0; i < 10; i++) {
            AuditLog auditLog = AuditLog.successBuilder()
                    .id(UUID.randomUUID().toString())
                    .operation("TEST_" + i)
                    .module("test")
                    .className("TestService")
                    .methodName("test")
                    .timestamp(LocalDateTime.now())
                    .durationMs(i)
                    .build();
            asyncStorage.save(auditLog);
        }

        // 等待刷新
        Thread.sleep(2000);

        // 验证数据已保存
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_log",
                Integer.class);

        assertThat(count).isEqualTo(10);

        asyncStorage.shutdown();
    }

    @Test
    @DisplayName("获取队列大小")
    void testGetQueueSize() {
        DatabaseAuditLogProperties properties = new DatabaseAuditLogProperties();
        properties.setTableName("audit_log");
        properties.setAsyncEnabled(true);
        properties.setBatchSize(100);
        properties.setFlushIntervalMs(5000);
        properties.setQueueCapacity(100);

        DatabaseAuditLogStorage asyncStorage = new DatabaseAuditLogStorage(jdbcTemplate, properties);

        // 同步模式下队列大小为 0
        assertThat(asyncStorage.getQueueSize()).isEqualTo(0);

        asyncStorage.shutdown();
    }

    private AuditLog createAuditLog() {
        return AuditLog.successBuilder()
                .id(UUID.randomUUID().toString())
                .operation("CREATE")
                .module("test")
                .className("TestClass")
                .methodName("testMethod")
                .timestamp(LocalDateTime.now())
                .durationMs(100L)
                .build();
    }
}