package io.github.afgprojects.framework.data.core.metadata;

import java.time.LocalDateTime;

/**
 * 通用字段元数据
 * <p>
 * 提供常见字段的预定义元数据实现，避免 APT 为每个实体重复生成相同的内部类。
 * 支持的通用字段包括：
 * <ul>
 *   <li>id - 主键字段（支持多种类型：Long、String、Integer 等）</li>
 *   <li>createdAt - 创建时间（LocalDateTime）</li>
 *   <li>updatedAt - 更新时间（LocalDateTime）</li>
 *   <li>deleted - 软删除标记（Boolean）</li>
 *   <li>deletedAt - 软删除时间（LocalDateTime）</li>
 *   <li>tenantId - 租户ID（String）</li>
 *   <li>version - 乐观锁版本号（Long/Integer）</li>
 *   <li>createBy - 创建人（String）</li>
 *   <li>updateBy - 更新人（String）</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>{@code
 * // APT 生成的代码可以直接引用
 * private static final List<FieldMetadata> FIELDS = List.of(
 *     CommonFieldMetadata.id(Long.class),
 *     CommonFieldMetadata.createdAt(),
 *     CommonFieldMetadata.updatedAt(),
 *     new UserNameFieldMetadata()  // 特定字段仍生成内部类
 * );
 * }</pre>
 */
public final class CommonFieldMetadata {

    private CommonFieldMetadata() {
        // 私有构造函数，防止实例化
    }

    // ==================== 预定义常量 ====================

    /**
     * 创建时间字段元数据（createdAt -> created_at）
     */
    public static final DatabaseFieldMetadata CREATED_AT = commonField("createdAt", "created_at", LocalDateTime.class);

    /**
     * 更新时间字段元数据（updatedAt -> updated_at）
     */
    public static final DatabaseFieldMetadata UPDATED_AT = commonField("updatedAt", "updated_at", LocalDateTime.class);

    /**
     * 软删除标记字段元数据（deleted -> deleted）
     */
    public static final DatabaseFieldMetadata DELETED = commonField("deleted", "deleted", Boolean.class);

    /**
     * 软删除时间字段元数据（deletedAt -> deleted_at）
     */
    public static final DatabaseFieldMetadata DELETED_AT = commonField("deletedAt", "deleted_at", LocalDateTime.class);

    /**
     * 租户ID字段元数据（tenantId -> tenant_id）
     */
    public static final DatabaseFieldMetadata TENANT_ID = commonField("tenantId", "tenant_id", String.class);

    /**
     * 版本号字段元数据（version -> version，Long 类型）
     */
    public static final DatabaseFieldMetadata VERSION_LONG = commonField("version", "version", Long.class);

    /**
     * 版本号字段元数据（version -> version，Integer 类型）
     */
    public static final DatabaseFieldMetadata VERSION_INTEGER = commonField("version", "version", Integer.class);

    /**
     * 创建人字段元数据（createBy -> create_by）
     */
    public static final DatabaseFieldMetadata CREATE_BY = commonField("createBy", "create_by", String.class);

    /**
     * 更新人字段元数据（updateBy -> update_by）
     */
    public static final DatabaseFieldMetadata UPDATE_BY = commonField("updateBy", "update_by", String.class);

    // ==================== 动态创建方法 ====================

    /**
     * 创建主键字段元数据
     * <p>
     * 主键字段类型不固定，需要根据实体定义动态创建
     *
     * @param idType 主键类型
     * @return 主键字段元数据
     */
    public static DatabaseFieldMetadata id(Class<?> idType) {
        return new SimpleFieldMetadata("id", "id", idType, true, true);
    }

    /**
     * 创建版本号字段元数据
     *
     * @param versionType 版本号类型（Long 或 Integer）
     * @return 版本号字段元数据
     */
    public static DatabaseFieldMetadata version(Class<?> versionType) {
        if (versionType == Integer.class || versionType == int.class) {
            return VERSION_INTEGER;
        }
        return VERSION_LONG;
    }

    // ==================== 字段匹配检查方法 ====================

    /**
     * 检查字段是否为通用字段
     *
     * @param propertyName 属性名
     * @param fieldType    字段类型
     * @return 如果是通用字段且类型匹配，返回对应的元数据；否则返回 null
     */
    public static DatabaseFieldMetadata match(String propertyName, Class<?> fieldType) {
        return match(propertyName, fieldType.getName());
    }

    /**
     * 检查字段是否为通用字段
     *
     * @param propertyName 属性名
     * @param fieldType    字段类型全限定名
     * @return 如果是通用字段且类型匹配，返回对应的元数据；否则返回 null
     */
    public static DatabaseFieldMetadata match(String propertyName, String fieldType) {
        // createdAt - 必须是 LocalDateTime
        if ("createdAt".equals(propertyName)) {
            return isLocalDateTime(fieldType) ? CREATED_AT : null;
        }

        // updatedAt - 必须是 LocalDateTime
        if ("updatedAt".equals(propertyName)) {
            return isLocalDateTime(fieldType) ? UPDATED_AT : null;
        }

        // deleted - 必须是 Boolean/boolean
        if ("deleted".equals(propertyName)) {
            return isBoolean(fieldType) ? DELETED : null;
        }

        // deletedAt - 必须是 LocalDateTime
        if ("deletedAt".equals(propertyName)) {
            return isLocalDateTime(fieldType) ? DELETED_AT : null;
        }

        // tenantId - 必须是 String
        if ("tenantId".equals(propertyName)) {
            return isString(fieldType) ? TENANT_ID : null;
        }

        // version - 支持 Long/long 或 Integer/int
        if ("version".equals(propertyName)) {
            if (isInteger(fieldType)) {
                return VERSION_INTEGER;
            }
            if (isLong(fieldType)) {
                return VERSION_LONG;
            }
            return null;
        }

        // createBy - 必须是 String
        if ("createBy".equals(propertyName)) {
            return isString(fieldType) ? CREATE_BY : null;
        }

        // updateBy - 必须是 String
        if ("updateBy".equals(propertyName)) {
            return isString(fieldType) ? UPDATE_BY : null;
        }

        // createdBy（别名）- 必须是 String
        if ("createdBy".equals(propertyName)) {
            return isString(fieldType) ? CREATE_BY : null;
        }

        // updatedBy（别名）- 必须是 String
        if ("updatedBy".equals(propertyName)) {
            return isString(fieldType) ? UPDATE_BY : null;
        }

        return null;
    }

    /**
     * 检查是否为通用字段（仅检查名称，不检查类型）
     *
     * @param propertyName 属性名
     * @return 是否为通用字段名称
     */
    public static boolean isCommonFieldName(String propertyName) {
        return "createdAt".equals(propertyName)
                || "updatedAt".equals(propertyName)
                || "deleted".equals(propertyName)
                || "deletedAt".equals(propertyName)
                || "tenantId".equals(propertyName)
                || "version".equals(propertyName)
                || "createBy".equals(propertyName)
                || "updateBy".equals(propertyName)
                || "createdBy".equals(propertyName)
                || "updatedBy".equals(propertyName);
    }

    // ==================== 类型检查辅助方法 ====================

    private static boolean isLocalDateTime(String fieldType) {
        return "java.time.LocalDateTime".equals(fieldType)
                || "LocalDateTime".equals(fieldType);
    }

    private static boolean isBoolean(String fieldType) {
        return "java.lang.Boolean".equals(fieldType)
                || "Boolean".equals(fieldType)
                || "boolean".equals(fieldType);
    }

    private static boolean isString(String fieldType) {
        return "java.lang.String".equals(fieldType)
                || "String".equals(fieldType);
    }

    private static boolean isLong(String fieldType) {
        return "java.lang.Long".equals(fieldType)
                || "Long".equals(fieldType)
                || "long".equals(fieldType);
    }

    private static boolean isInteger(String fieldType) {
        return "java.lang.Integer".equals(fieldType)
                || "Integer".equals(fieldType)
                || "int".equals(fieldType);
    }

    // ==================== 内部实现类 ====================

    /**
     * 创建通用字段的元数据实例
     *
     * @param propertyName 属性名
     * @param columnName   列名
     * @param fieldType    字段类型
     * @return 通用字段元数据
     */
    private static DatabaseFieldMetadata commonField(String propertyName, String columnName, Class<?> fieldType) {
        return new SimpleFieldMetadata(propertyName, columnName, fieldType, false, false);
    }

    /**
     * 参数化的字段元数据实现
     * <p>
     * 替代原来 9 个重复的内部类，通过构造函数参数化差异部分。
     */
    private record SimpleFieldMetadata(
            String propertyName,
            String columnName,
            Class<?> fieldType,
            boolean id,
            boolean generated
    ) implements DatabaseFieldMetadata {

        @Override
        public String getPropertyName() {
            return propertyName;
        }

        @Override
        public String getColumnName() {
            return columnName;
        }

        @Override
        public Class<?> getFieldType() {
            return fieldType;
        }

        @Override
        public boolean isId() {
            return id;
        }

        @Override
        public boolean isGenerated() {
            return generated;
        }
    }
}
