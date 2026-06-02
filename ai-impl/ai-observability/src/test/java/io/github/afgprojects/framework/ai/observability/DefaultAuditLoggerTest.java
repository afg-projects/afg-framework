package io.github.afgprojects.framework.ai.observability;

import io.github.afgprojects.framework.ai.core.api.observability.AuditLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DefaultAuditLogger 单元测试
 */
class DefaultAuditLoggerTest {

    private DefaultAuditLogger logger;

    @BeforeEach
    void setUp() {
        logger = new DefaultAuditLogger(100);
    }

    @Test
    @DisplayName("记录审计日志")
    void log() {
        logger.log("user-001", "chat", "gpt-4", "Hello", "Hi there!", AuditLogger.AuditStatus.SUCCESS);

        AuditLogger.AuditLogResult result = logger.query(DefaultAuditQuery.builder().build());

        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getEntries()).hasSize(1);

        AuditLogger.AuditEntry entry = result.getEntries().get(0);
        assertThat(entry.getUserId()).isEqualTo("user-001");
        assertThat(entry.getOperationType()).isEqualTo("chat");
        assertThat(entry.getModelName()).isEqualTo("gpt-4");
        assertThat(entry.getStatus()).isEqualTo(AuditLogger.AuditStatus.SUCCESS);
    }

    @Test
    @DisplayName("记录敏感数据访问")
    void logSensitiveDataAccess() {
        logger.logSensitiveDataAccess("user-001", "document", "doc-123",
                AuditLogger.AccessType.READ, "Read confidential document");

        AuditLogger.AuditLogResult result = logger.query(
                DefaultAuditQuery.builder().operationType("SENSITIVE_DATA_ACCESS").build()
        );

        assertThat(result.getTotal()).isEqualTo(1);

        AuditLogger.AuditEntry entry = result.getEntries().get(0);
        assertThat(entry.getUserId()).isEqualTo("user-001");
        assertThat(entry.getAttributes().get("dataType")).isEqualTo("document");
        assertThat(entry.getAttributes().get("accessType")).isEqualTo("READ");
    }

    @Test
    @DisplayName("记录安全事件")
    void logSecurityEvent() {
        logger.logSecurityEvent(
                AuditLogger.SecurityEventType.AUTH_FAILURE,
                AuditLogger.Severity.HIGH,
                "Multiple failed login attempts",
                java.util.Map.of("ip", "192.168.1.100", "attempts", "5")
        );

        AuditLogger.AuditLogResult result = logger.query(
                DefaultAuditQuery.builder().userId(null).build()
        );

        assertThat(result.getTotal()).isEqualTo(1);

        AuditLogger.AuditEntry entry = result.getEntries().get(0);
        assertThat(entry.getOperationType()).contains("SECURITY_EVENT");
        assertThat(entry.getAttributes().get("severity")).isEqualTo("HIGH");
    }

    @Test
    @DisplayName("按用户 ID 查询")
    void query_byUserId() {
        logger.log("user-001", "chat", "gpt-4", null, null, AuditLogger.AuditStatus.SUCCESS);
        logger.log("user-002", "chat", "gpt-4", null, null, AuditLogger.AuditStatus.SUCCESS);
        logger.log("user-001", "completion", "gpt-3.5", null, null, AuditLogger.AuditStatus.FAILURE);

        AuditLogger.AuditLogResult result = logger.query(
                DefaultAuditQuery.builder().userId("user-001").build()
        );

        assertThat(result.getTotal()).isEqualTo(2);
        assertThat(result.getEntries()).hasSize(2);
    }

    @Test
    @DisplayName("按状态查询")
    void query_byStatus() {
        logger.log("user-001", "chat", "gpt-4", null, null, AuditLogger.AuditStatus.SUCCESS);
        logger.log("user-001", "chat", "gpt-4", null, null, AuditLogger.AuditStatus.FAILURE);
        logger.log("user-001", "chat", "gpt-4", null, null, AuditLogger.AuditStatus.SUCCESS);

        AuditLogger.AuditLogResult result = logger.query(
                DefaultAuditQuery.builder().status(AuditLogger.AuditStatus.FAILURE).build()
        );

        assertThat(result.getTotal()).isEqualTo(1);
    }

    @Test
    @DisplayName("按时间范围查询")
    void query_byTimeRange() {
        Instant now = Instant.now();
        Instant oneHourAgo = now.minusSeconds(3600);
        Instant oneHourLater = now.plusSeconds(3600);

        logger.log("user-001", "chat", "gpt-4", null, null, AuditLogger.AuditStatus.SUCCESS);

        AuditLogger.AuditLogResult result = logger.query(
                DefaultAuditQuery.builder()
                        .startTime(oneHourAgo)
                        .endTime(oneHourLater)
                        .build()
        );

        assertThat(result.getTotal()).isEqualTo(1);
    }

    @Test
    @DisplayName("分页查询")
    void query_pagination() {
        for (int i = 0; i < 25; i++) {
            logger.log("user-001", "chat", "gpt-4", null, null, AuditLogger.AuditStatus.SUCCESS);
        }

        AuditLogger.AuditLogResult page1 = logger.query(
                DefaultAuditQuery.builder().page(0).size(10).build()
        );

        assertThat(page1.getEntries()).hasSize(10);
        assertThat(page1.getTotal()).isEqualTo(25);
        assertThat(page1.hasNext()).isTrue();

        AuditLogger.AuditLogResult page2 = logger.query(
                DefaultAuditQuery.builder().page(1).size(10).build()
        );

        assertThat(page2.getEntries()).hasSize(10);
        assertThat(page2.hasNext()).isTrue();

        AuditLogger.AuditLogResult page3 = logger.query(
                DefaultAuditQuery.builder().page(2).size(10).build()
        );

        assertThat(page3.getEntries()).hasSize(5);
        assertThat(page3.hasNext()).isFalse();
    }

    @Test
    @DisplayName("最大条目数限制")
    void maxEntries() {
        logger = new DefaultAuditLogger(10);

        for (int i = 0; i < 15; i++) {
            logger.log("user-001", "chat", "gpt-4", null, null, AuditLogger.AuditStatus.SUCCESS);
        }

        AuditLogger.AuditLogResult result = logger.query(
                DefaultAuditQuery.builder().build()
        );

        assertThat(result.getTotal()).isEqualTo(10);
    }
}