package io.github.afgprojects.framework.ai.core.service;

import io.github.afgprojects.framework.ai.core.api.observability.AuditLogger;
import io.github.afgprojects.framework.ai.core.api.security.PiiDetector;
import io.github.afgprojects.framework.ai.core.api.security.PiiDetector.PiiContext;
import io.github.afgprojects.framework.ai.core.api.security.PiiDetector.PiiDetectionResult;
import io.github.afgprojects.framework.ai.core.api.security.PiiDetector.PiiMaskingResult;
import io.github.afgprojects.framework.ai.core.api.security.PiiDetector.PiiType;
import io.github.afgprojects.framework.ai.core.api.security.PiiDetector.MaskingStrategy;
import io.github.afgprojects.framework.ai.core.entity.security.AuditLogEntity;
import io.github.afgprojects.framework.commons.model.PageData;
import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.data.core.condition.Conditions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * 安全管理服务
 *
 * <p>提供审计日志查询/统计/清理、PII 检测/脱敏功能。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityManagementService {

    private final DataManager dataManager;
    private final AuditLogger auditLogger;
    private final PiiDetector piiDetector;

    /**
     * 查询审计日志（带过滤条件）
     *
     * @param operation 操作类型（可选）
     * @param level     日志级别（可选）
     * @param userId    用户ID（可选）
     * @param startTime 开始时间（可选）
     * @param endTime   结束时间（可选）
     * @param page      页码
     * @param size      每页大小
     * @return 审计日志列表
     */
    public PageData<AuditLogEntity> listAuditLogs(String operation, String level,
                                                String userId, Instant startTime, Instant endTime,
                                                int page, int size) {
        var builder = Conditions.builder(AuditLogEntity.class);

        if (operation != null && !operation.isEmpty()) {
            builder.eq(AuditLogEntity::getOperation, operation);
        }
        if (level != null && !level.isEmpty()) {
            builder.eq(AuditLogEntity::getLevel, level);
        }
        if (userId != null && !userId.isEmpty()) {
            builder.eq(AuditLogEntity::getUserId, userId);
        }
        if (startTime != null) {
            builder.ge(AuditLogEntity::getCreatedAt, startTime);
        }
        if (endTime != null) {
            builder.le(AuditLogEntity::getCreatedAt, endTime);
        }

        return dataManager.entity(AuditLogEntity.class)
            .query()
            .where(builder.build())
            .orderByDesc(AuditLogEntity::getCreatedAt)
            .page(io.github.afgprojects.framework.data.core.page.PageRequest.of(page, size));
    }

    /**
     * 统计审计日志数量
     *
     * @param operation 操作类型（可选）
     * @param level     日志级别（可选）
     * @return 日志数量
     */
    public long countAuditLogs(String operation, String level) {
        var builder = Conditions.builder(AuditLogEntity.class);

        if (operation != null && !operation.isEmpty()) {
            builder.eq(AuditLogEntity::getOperation, operation);
        }
        if (level != null && !level.isEmpty()) {
            builder.eq(AuditLogEntity::getLevel, level);
        }

        return dataManager.entity(AuditLogEntity.class)
            .query()
            .where(builder.build())
            .count();
    }

    /**
     * 清理指定时间之前的审计日志
     *
     * @param before 截止时间
     * @return 清理的记录数
     */
    @Transactional
    public long cleanupAuditLogs(Instant before) {
        var condition = Conditions.builder(AuditLogEntity.class)
            .le(AuditLogEntity::getCreatedAt, before)
            .build();

        long count = dataManager.entity(AuditLogEntity.class)
            .query()
            .where(condition)
            .count();

        dataManager.entity(AuditLogEntity.class)
            .deleteByCondition(condition);

        log.info("Cleaned up audit logs before {}: count={}", before, count);
        return count;
    }

    /**
     * 检测文本中的 PII
     *
     * @param text      原始文本
     * @param piiTypes  要检测的 PII 类型（可选，为空则检测所有类型）
     * @return 检测结果
     */
    public PiiDetectionResult detectPii(String text, List<PiiType> piiTypes) {
        PiiContext context = new SimplePiiContext(piiTypes);
        return piiDetector.detect(text, context);
    }

    /**
     * 脱敏文本
     *
     * @param text            原始文本
     * @param piiTypes        要脱敏的 PII 类型（可选）
     * @param maskingStrategy 脱敏策略（可选，默认 PARTIAL_MASK）
     * @return 脱敏结果
     */
    public PiiMaskingResult maskPii(String text, List<PiiType> piiTypes, MaskingStrategy maskingStrategy) {
        PiiContext context = new SimplePiiContext(piiTypes, maskingStrategy);
        if (piiTypes != null && !piiTypes.isEmpty()) {
            return piiDetector.mask(text, piiTypes, context);
        }
        return piiDetector.mask(text, context);
    }

    /**
     * 简单 PII 上下文实现
     */
    private record SimplePiiContext(
        List<PiiType> detectTypes,
        MaskingStrategy maskingStrategy,
        double minConfidence
    ) implements PiiContext {

        SimplePiiContext(List<PiiType> detectTypes) {
            this(detectTypes, MaskingStrategy.PARTIAL_MASK, 0.7);
        }

        SimplePiiContext(List<PiiType> detectTypes, MaskingStrategy maskingStrategy) {
            this(detectTypes, maskingStrategy != null ? maskingStrategy : MaskingStrategy.PARTIAL_MASK, 0.7);
        }

        @Override
        public String getUserId() {
            return null;
        }

        @Override
        public String getTenantId() {
            return null;
        }

        @Override
        public List<PiiType> getDetectTypes() {
            return detectTypes != null ? detectTypes : List.of();
        }

        @Override
        public MaskingStrategy getMaskingStrategy() {
            return maskingStrategy;
        }

        @Override
        public double getMinConfidence() {
            return minConfidence;
        }
    }
}
