package io.github.afgprojects.framework.apt.entity;

import java.util.List;

/**
 * 实体特性检测器
 * <p>
 * 负责检测实体类是否支持特定特性：
 * <ul>
 *   <li>软删除（softDeletable）- 检测 deleted 或 deletedAt 字段</li>
 *   <li>多租户（tenantAware）- 检测 tenantId 字段</li>
 *   <li>时间戳（timestamped）- 检测 createdAt 和 updatedAt 字段</li>
 *   <li>审计（auditable）- 检测 createBy 和 updateBy 字段</li>
 *   <li>版本化（versioned）- 检测 version 字段</li>
 *   <li>数据权限感知（dataScopeAware）- 检测 deptId 字段</li>
 *   <li>加密字段（encrypted）- 检测 @EncryptedField 注解</li>
 *   <li>敏感字段（sensitive）- 检测 @SensitiveField 注解</li>
 *   <li>树形结构（treeable）- 检测 parentId 和 path 字段</li>
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
        boolean timestamped,
        boolean auditable,
        boolean versioned,
        boolean dataScopeAware,
        boolean encrypted,
        boolean sensitive,
        boolean treeable
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
            boolean hasCreateBy = false;
            boolean hasUpdateBy = false;
            boolean hasVersion = false;
            boolean hasEncrypted = false;
            boolean hasSensitive = false;
            boolean hasParentId = false;
            boolean hasPath = false;

            // 单次遍历检测所有特性
            for (FieldMetadataGenerator.FieldInfo field : fields) {
                String propertyName = field.propertyName();
                switch (propertyName) {
                    case "deleted" -> hasDeleted = true;
                    case "deletedAt" -> hasDeletedAt = true;
                    case "tenantId" -> hasTenantId = true;
                    case "createdAt" -> hasCreatedAt = true;
                    case "updatedAt" -> hasUpdatedAt = true;
                    case "createBy" -> hasCreateBy = true;
                    case "updateBy" -> hasUpdateBy = true;
                    case "version" -> hasVersion = true;
                    case "parentId" -> hasParentId = true;
                    case "path" -> hasPath = true;
                    default -> { /* 其他字段不参与特性检测 */ }
                }
                if (field.isEncrypted()) {
                    hasEncrypted = true;
                }
                if (field.isSensitive()) {
                    hasSensitive = true;
                }
            }

            return new FeatureDetectionResult(
                hasDeleted,                       // softDeletable (Boolean deleted)
                hasDeletedAt,                     // timestampSoftDeletable (Instant deletedAt)
                hasTenantId,                      // tenantAware
                hasCreatedAt && hasUpdatedAt,      // timestamped (createdAt/updatedAt from BaseEntity)
                hasCreateBy && hasUpdateBy,        // auditable (createBy/updateBy from FullEntity)
                hasVersion,                       // versioned
                false,                            // dataScopeAware (需要注解配置，默认 false)
                hasEncrypted,                     // encrypted (has @EncryptedField fields)
                hasSensitive,                     // sensitive (has @SensitiveField fields)
                hasParentId && hasPath            // treeable (parentId + path from TreeEntity)
            );
        }
    }
}