package io.github.afgprojects.framework.ai.core.controller;

import io.github.afgprojects.framework.ai.core.api.security.PiiDetector.MaskingStrategy;
import io.github.afgprojects.framework.ai.core.api.security.PiiDetector.PiiDetectionResult;
import io.github.afgprojects.framework.ai.core.api.security.PiiDetector.PiiMaskingResult;
import io.github.afgprojects.framework.ai.core.api.security.PiiDetector.PiiType;
import io.github.afgprojects.framework.ai.core.entity.security.AuditLogEntity;
import io.github.afgprojects.framework.ai.core.service.SecurityManagementService;
import io.github.afgprojects.framework.commons.model.PageData;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * AI 安全管理控制器
 *
 * <p>提供审计日志查询/统计/清理、PII 检测/脱敏接口。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@RestController
@RequestMapping("/security")
@RequiredArgsConstructor
public class AiSecurityController {

    private final SecurityManagementService securityManagementService;

    // ==================== 审计日志 ====================

    /**
     * 查询审计日志
     */
    @GetMapping("/audit-logs")
    public ResponseEntity<PageData<AuditLogEntity>> listAuditLogs(
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageData<AuditLogEntity> logs = securityManagementService.listAuditLogs(
            operation, level, userId, startTime, endTime, page, size);
        return ResponseEntity.ok(logs);
    }

    /**
     * 统计审计日志数量
     */
    @GetMapping("/audit-logs/count")
    public ResponseEntity<Long> countAuditLogs(
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) String level) {
        return ResponseEntity.ok(securityManagementService.countAuditLogs(operation, level));
    }

    /**
     * 清理审计日志
     */
    @DeleteMapping("/audit-logs/cleanup")
    public ResponseEntity<Long> cleanupAuditLogs(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant before) {
        return ResponseEntity.ok(securityManagementService.cleanupAuditLogs(before));
    }

    // ==================== PII 检测与脱敏 ====================

    /**
     * 检测文本中的 PII
     */
    @PostMapping("/pii/detect")
    public ResponseEntity<PiiDetectionResult> detectPii(@RequestBody Map<String, Object> request) {
        String text = (String) request.get("text");
        @SuppressWarnings("unchecked")
        List<String> typeNames = (List<String>) request.get("piiTypes");

        List<PiiType> piiTypes = null;
        if (typeNames != null && !typeNames.isEmpty()) {
            piiTypes = typeNames.stream()
                .map(name -> PiiType.valueOf(name))
                .toList();
        }

        return ResponseEntity.ok(securityManagementService.detectPii(text, piiTypes));
    }

    /**
     * 脱敏文本
     */
    @PostMapping("/pii/mask")
    public ResponseEntity<PiiMaskingResult> maskPii(@RequestBody Map<String, Object> request) {
        String text = (String) request.get("text");
        @SuppressWarnings("unchecked")
        List<String> typeNames = (List<String>) request.get("piiTypes");
        String strategyName = (String) request.get("maskingStrategy");

        List<PiiType> piiTypes = null;
        if (typeNames != null && !typeNames.isEmpty()) {
            piiTypes = typeNames.stream()
                .map(name -> PiiType.valueOf(name))
                .toList();
        }

        MaskingStrategy strategy = null;
        if (strategyName != null && !strategyName.isEmpty()) {
            strategy = MaskingStrategy.valueOf(strategyName);
        }

        return ResponseEntity.ok(securityManagementService.maskPii(text, piiTypes, strategy));
    }
}
