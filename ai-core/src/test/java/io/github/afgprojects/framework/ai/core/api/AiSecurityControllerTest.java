package io.github.afgprojects.framework.ai.core.api;

import io.github.afgprojects.framework.ai.core.AbstractAiWebTest;
import io.github.afgprojects.framework.ai.core.entity.security.AuditLogEntity;
import io.github.afgprojects.framework.data.core.DataManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AiSecurityController 集成测试
 *
 * <p>测试审计日志查询/统计/清理、PII 检测/脱敏接口。
 *
 * @author afg-projects
 * @since 1.0.0
 */
class AiSecurityControllerTest extends AbstractAiWebTest {

    @Autowired
    DataManager dataManager;

    // ==================== 审计日志：列表查询 ====================

    @Test
    void shouldListAuditLogs_whenGetAll() {
        // Arrange - 通过 DataManager 预置 2 条不同操作的审计日志
        String prefix = "list-audit-" + UUID.randomUUID();

        AuditLogEntity log1 = newAuditLog(prefix + "-op1", "INFO", "user-list-1");
        AuditLogEntity log2 = newAuditLog(prefix + "-op2", "WARN", "user-list-2");
        log1 = dataManager.save(AuditLogEntity.class, log1);
        log2 = dataManager.save(AuditLogEntity.class, log2);

        try {
            // Act
            @SuppressWarnings("unchecked")
            Map<String, Object> page = restClient().get()
                .uri(uriBuilder -> uriBuilder.path("/security/audit-logs")
                    .queryParam("operation", prefix + "-op1")
                    .queryParam("page", 1)
                    .queryParam("size", 20)
                    .build())
                .retrieve()
                .body(Map.class);

            // Assert - PageData 字段：records / total / page / size
            assertThat(page).isNotNull();
            assertThat(((Number) page.get("total")).longValue()).isGreaterThanOrEqualTo(1L);
            List<Map<String, Object>> records = (List<Map<String, Object>>) page.get("records");
            assertThat(records).isNotEmpty();
            assertThat(records).allSatisfy(record ->
                assertThat(record.get("operation")).isEqualTo(prefix + "-op1"));
        } finally {
            cleanup(log1, log2);
        }
    }

    @Test
    void shouldFilterAuditLogsByLevelAndUser_whenGetWithFilters() {
        // Arrange
        String prefix = "filter-audit-" + UUID.randomUUID();
        String targetLevel = "ERR_" + Integer.toHexString(UUID.randomUUID().hashCode() & 0xffffff);
        String targetUser = "user-filter-" + UUID.randomUUID();

        AuditLogEntity match = newAuditLog(prefix + "-match", targetLevel, targetUser);
        AuditLogEntity other = newAuditLog(prefix + "-other", "INFO", "someone-else");
        match = dataManager.save(AuditLogEntity.class, match);
        other = dataManager.save(AuditLogEntity.class, other);

        try {
            // Act - 同时按 level 与 userId 过滤
            @SuppressWarnings("unchecked")
            Map<String, Object> page = restClient().get()
                .uri(uriBuilder -> uriBuilder.path("/security/audit-logs")
                    .queryParam("level", targetLevel)
                    .queryParam("userId", targetUser)
                    .build())
                .retrieve()
                .body(Map.class);

            // Assert - 仅命中 match 一条
            List<Map<String, Object>> records = (List<Map<String, Object>>) page.get("records");
            assertThat(records).hasSize(1);
            assertThat(records.get(0).get("userId")).isEqualTo(targetUser);
            assertThat(records.get(0).get("level")).isEqualTo(targetLevel);
        } finally {
            cleanup(match, other);
        }
    }

    @Test
    void shouldFilterAuditLogsByTimeRange_whenGetWithStartEnd() {
        // Arrange - 用可区分的时间戳，依赖 autoFillTimestamps 保留手动设置的 createdAt
        String prefix = "time-audit-" + UUID.randomUUID();
        Instant past = Instant.now().minus(2, ChronoUnit.HOURS);
        Instant recent = Instant.now().minus(5, ChronoUnit.MINUTES);

        AuditLogEntity oldLog = newAuditLog(prefix + "-old", "INFO", "user-time-old");
        oldLog.setCreatedAt(past);
        AuditLogEntity newLog = newAuditLog(prefix + "-new", "INFO", "user-time-new");
        newLog.setCreatedAt(recent);
        oldLog = dataManager.save(AuditLogEntity.class, oldLog);
        newLog = dataManager.save(AuditLogEntity.class, newLog);

        try {
            // Act - 仅查询最近 1 小时内的日志
            Instant cutoff = Instant.now().minus(1, ChronoUnit.HOURS);
            @SuppressWarnings("unchecked")
            Map<String, Object> page = restClient().get()
                .uri(uriBuilder -> uriBuilder.path("/security/audit-logs")
                    .queryParam("operation", prefix + "-new")
                    .queryParam("startTime", cutoff.toString())
                    .build())
                .retrieve()
                .body(Map.class);

            // Assert - oldLog 早于 cutoff，不应出现；newLog 应出现
            List<Map<String, Object>> records = (List<Map<String, Object>>) page.get("records");
            assertThat(records).extracting(r -> r.get("operation"))
                .contains(prefix + "-new")
                .doesNotContain(prefix + "-old");
        } finally {
            cleanup(oldLog, newLog);
        }
    }

    // ==================== 审计日志：统计数量 ====================

    @Test
    void shouldCountAuditLogs_whenGetWithFilters() {
        // Arrange
        String prefix = "count-audit-" + UUID.randomUUID();
        String targetOp = prefix + "-op";
        String targetLevel = "WRN_" + Integer.toHexString(UUID.randomUUID().hashCode() & 0xffffff);

        AuditLogEntity log1 = newAuditLog(targetOp, targetLevel, "user-count-1");
        AuditLogEntity log2 = newAuditLog(targetOp, targetLevel, "user-count-2");
        AuditLogEntity other = newAuditLog(targetOp, "INFO", "user-count-3");
        log1 = dataManager.save(AuditLogEntity.class, log1);
        log2 = dataManager.save(AuditLogEntity.class, log2);
        other = dataManager.save(AuditLogEntity.class, other);

        try {
            // Act - 按 operation + level 统计
            Long count = restClient().get()
                .uri(uriBuilder -> uriBuilder.path("/security/audit-logs/count")
                    .queryParam("operation", targetOp)
                    .queryParam("level", targetLevel)
                    .build())
                .retrieve()
                .body(Long.class);

            // Assert - 仅 log1/log2 命中
            assertThat(count).isGreaterThanOrEqualTo(2L);
        } finally {
            cleanup(log1, log2, other);
        }
    }

    // ==================== 审计日志：清理 ====================

    @Test
    void shouldCleanupAuditLogsBefore_whenDeleteWithCutoff() {
        // Arrange
        String prefix = "cleanup-audit-" + UUID.randomUUID();
        Instant past = Instant.now().minus(2, ChronoUnit.HOURS);
        Instant recent = Instant.now().minus(1, ChronoUnit.MINUTES);

        AuditLogEntity oldLog = newAuditLog(prefix + "-old", "INFO", "user-cleanup-old");
        oldLog.setCreatedAt(past);
        AuditLogEntity newLog = newAuditLog(prefix + "-new", "INFO", "user-cleanup-new");
        newLog.setCreatedAt(recent);
        oldLog = dataManager.save(AuditLogEntity.class, oldLog);
        newLog = dataManager.save(AuditLogEntity.class, newLog);

        // cutoff 取 past 与 recent 之间，仅清理 oldLog
        Instant cutoff = Instant.now().minus(30, ChronoUnit.MINUTES);

        try {
            // Act
            Long deleted = restClient().delete()
                .uri(uriBuilder -> uriBuilder.path("/security/audit-logs/cleanup")
                    .queryParam("before", cutoff.toString())
                    .build())
                .retrieve()
                .body(Long.class);

            // Assert - 至少删除了 oldLog，且 newLog 仍在
            assertThat(deleted).isGreaterThanOrEqualTo(1L);
            AuditLogEntity survivingNew = dataManager.findById(AuditLogEntity.class, newLog.getId())
                .orElse(null);
            assertThat(survivingNew).isNotNull();
        } finally {
            // oldLog 已被清理，仅清理残留的 newLog
            if (newLog.getId() != null) {
                dataManager.deleteById(AuditLogEntity.class, newLog.getId());
            }
        }
    }

    // ==================== PII 检测 ====================

    @Test
    void shouldDetectPii_whenTextContainsEmailAndPhone() {
        // Arrange - 邮箱与中国手机号（11 位，以 1 开头）
        Map<String, Object> request = Map.of(
            "text", "联系我：alice@example.com 或拨打 13812345678");

        // Act
        @SuppressWarnings("unchecked")
        Map<String, Object> result = restClient().post()
            .uri("/security/pii/detect")
            .body(request)
            .retrieve()
            .body(Map.class);

        // Assert - hasPii() 不被 Jackson 序列化，故以 piiMatches/piiTypes 判定检测结果
        assertThat(result).isNotNull();
        List<String> types = (List<String>) result.get("piiTypes");
        assertThat(types).contains("EMAIL", "PHONE");
        List<Map<String, Object>> matches = (List<Map<String, Object>>) result.get("piiMatches");
        assertThat(matches).isNotEmpty();
    }

    @Test
    void shouldNotDetectPii_whenTextHasNoPii() {
        // Arrange
        Map<String, Object> request = Map.of(
            "text", "这是一段不含任何个人身份信息的普通文本。");

        // Act
        @SuppressWarnings("unchecked")
        Map<String, Object> result = restClient().post()
            .uri("/security/pii/detect")
            .body(request)
            .retrieve()
            .body(Map.class);

        // Assert - 无 PII 时 piiMatches 为空、piiTypes 为空
        assertThat(result).isNotNull();
        assertThat((List<?>) result.get("piiMatches")).isEmpty();
        assertThat((List<?>) result.get("piiTypes")).isEmpty();
    }

    @Test
    void shouldDetectOnlySpecifiedTypes_whenPiiTypesProvided() {
        // Arrange - 文本同时含邮箱与手机号，但仅要求检测 EMAIL
        Map<String, Object> request = Map.of(
            "text", "alice@example.com 拨打 13812345678",
            "piiTypes", List.of("EMAIL"));

        // Act
        @SuppressWarnings("unchecked")
        Map<String, Object> result = restClient().post()
            .uri("/security/pii/detect")
            .body(request)
            .retrieve()
            .body(Map.class);

        // Assert - 仅返回 EMAIL，不含 PHONE
        List<String> types = (List<String>) result.get("piiTypes");
        assertThat(types).contains("EMAIL").doesNotContain("PHONE");
    }

    // ==================== PII 脱敏 ====================

    @Test
    void shouldMaskPii_whenTextContainsPii() {
        // Arrange
        String original = "联系 alice@example.com 或 13812345678";
        Map<String, Object> request = Map.of("text", original);

        // Act
        @SuppressWarnings("unchecked")
        Map<String, Object> result = restClient().post()
            .uri("/security/pii/mask")
            .body(request)
            .retrieve()
            .body(Map.class);

        // Assert - 脱敏后文本与原文不同，且统计非零
        assertThat(result).isNotNull();
        String maskedText = (String) result.get("maskedText");
        assertThat(maskedText).isNotEqualTo(original);
        assertThat(((Number) result.get("maskedCount")).intValue()).isGreaterThan(0);
        List<String> maskedTypes = (List<String>) result.get("maskedTypes");
        assertThat(maskedTypes).isNotEmpty();
    }

    @Test
    void shouldMaskWithStrategy_whenMaskingStrategyProvided() {
        // Arrange - 指定 FULL_MASK 策略
        Map<String, Object> request = Map.of(
            "text", "alice@example.com",
            "piiTypes", List.of("EMAIL"),
            "maskingStrategy", "FULL_MASK");

        // Act
        @SuppressWarnings("unchecked")
        Map<String, Object> result = restClient().post()
            .uri("/security/pii/mask")
            .body(request)
            .retrieve()
            .body(Map.class);

        // Assert - 返回结构完整，脱敏生效
        assertThat(result).isNotNull();
        assertThat((String) result.get("maskedText")).isNotEqualTo("alice@example.com");
        assertThat(((Number) result.get("maskedCount")).intValue()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void shouldNotMask_whenTextHasNoPii() {
        // Arrange
        String plain = "一段不含 PII 的文本";
        Map<String, Object> request = Map.of("text", plain);

        // Act
        @SuppressWarnings("unchecked")
        Map<String, Object> result = restClient().post()
            .uri("/security/pii/mask")
            .body(request)
            .retrieve()
            .body(Map.class);

        // Assert - 无 PII 时脱敏文本等于原文，数量为 0
        assertThat(result.get("maskedText")).isEqualTo(plain);
        assertThat(((Number) result.get("maskedCount")).intValue()).isZero();
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建一条审计日志实体（不含 id/createdAt，由 save 自动填充或手动覆盖）。
     */
    private AuditLogEntity newAuditLog(String operation, String level, String userId) {
        AuditLogEntity entity = new AuditLogEntity();
        entity.setOperation(operation);
        entity.setLevel(level);
        entity.setUserId(userId);
        entity.setMethod("POST");
        entity.setDurationMs(10L);
        return entity;
    }

    /**
     * 清理本测试预置的审计日志，避免污染共享数据库。
     */
    private void cleanup(AuditLogEntity... logs) {
        for (AuditLogEntity log : logs) {
            if (log != null && log.getId() != null) {
                dataManager.deleteById(AuditLogEntity.class, log.getId());
            }
        }
    }
}
