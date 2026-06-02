package io.github.afgprojects.framework.apt.entity;

import java.util.List;

/**
 * 实体特性检测器
 * <p>
 * 负责检测实体类是否支持特定特性：
 * <ul>
 *   <li>软删除（softDeletable）- 检测 deleted 或 deletedAt 字段</li>
 *   <li>多租户（tenantAware）- 检测 tenantId 字段</li>
 *   <li>审计（auditable）- 检测 createdAt 和 updatedAt 字段</li>
 *   <li>版本化（versioned）- 检测 version 字段</li>
 * </ul>
 */
class EntityFeatureDetector {

    /**
     * 特性检测结果
     */
    record FeatureDetectionResult(
        boolean softDeletable,
        boolean timestampSoftDeletable,
        boolean tenantAware,
        boolean auditable,
        boolean versioned,
        boolean dataScopeAware
    ) {
        /**
         * 执行所有特性检测（单次遍历优化）
         *
         * @param fields 字段信息列表
         * @return 特性检测结果
         */
        static FeatureDetectionResult detect(List<FieldMetadataGenerator.FieldInfo> fields) {
            boolean hasDeleted = false;
            boolean hasDeletedAt = false;
            boolean hasTenantId = false;
            boolean hasCreatedAt = false;
            boolean hasUpdatedAt = false;
            boolean hasVersion = false;

            // 单次遍历检测所有特性
            for (FieldMetadataGenerator.FieldInfo field : fields) {
                String propertyName = field.propertyName();
                switch (propertyName) {
                    case "deleted" -> hasDeleted = true;
                    case "deletedAt" -> hasDeletedAt = true;
                    case "tenantId" -> hasTenantId = true;
                    case "createdAt" -> hasCreatedAt = true;
                    case "updatedAt" -> hasUpdatedAt = true;
                    case "version" -> hasVersion = true;
                    default -> { /* 其他字段不参与特性检测 */ }
                }
            }

            return new FeatureDetectionResult(
                hasDeleted,                   // softDeletable (Boolean deleted)
                hasDeletedAt,                 // timestampSoftDeletable (Instant deletedAt)
                hasTenantId,                  // tenantAware
                hasCreatedAt && hasUpdatedAt,  // auditable
                hasVersion,                   // versioned
                false                         // dataScopeAware (需要注解配置，默认 false)
            );
        }
    }
}