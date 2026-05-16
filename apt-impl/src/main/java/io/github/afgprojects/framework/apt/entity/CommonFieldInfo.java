package io.github.afgprojects.framework.apt.entity;

import io.github.afgprojects.framework.commons.naming.NamingUtils;

/**
 * 通用字段信息
 * <p>
 * 存储注册字段的完整信息，用于 APT 生成代码。
 *
 * @param name         注册名称，如 "ORG_ID"
 * @param propertyName 属性名，如 "orgId"
 * @param columnName   列名，如 "org_id"
 * @param fieldType    字段类型全限定名，如 "java.lang.String"
 * @param isId         是否主键
 * @param isGenerated  是否自动生成
 * @param source       字段来源
 */
record CommonFieldInfo(
    String name,
    String propertyName,
    String columnName,
    String fieldType,
    boolean isId,
    boolean isGenerated,
    FieldSource source
) {

    /**
     * 创建查找键
     * <p>
     * 格式：propertyName:fieldType，用于精确匹配。
     *
     * @return 查找键
     */
    String lookupKey() {
        return propertyName + ":" + fieldType;
    }

    /**
     * 从注解创建 CommonFieldInfo
     *
     * @param annotation 注解实例
     * @param source     字段来源
     * @return CommonFieldInfo 实例
     */
    static CommonFieldInfo fromAnnotation(CommonFieldDefinition annotation, FieldSource source) {
        String columnName = annotation.columnName();
        if (columnName == null || columnName.isEmpty()) {
            // 自动转换为 snake_case
            columnName = NamingUtils.toSnakeCase(annotation.propertyName());
        }

        return new CommonFieldInfo(
            annotation.name(),
            annotation.propertyName(),
            columnName,
            annotation.fieldType().getName(),
            annotation.isId(),
            annotation.isGenerated(),
            source
        );
    }

    /**
     * 从 JSON 配置创建 CommonFieldInfo
     *
     * @param name         注册名称
     * @param propertyName  属性名
     * @param columnName    列名（可为空，自动转换）
     * @param fieldType    字段类型全限定名
     * @param isId         是否主键
     * @param isGenerated  是否自动生成
     * @return CommonFieldInfo 实例
     */
    static CommonFieldInfo fromConfig(
        String name,
        String propertyName,
        String columnName,
        String fieldType,
        boolean isId,
        boolean isGenerated
    ) {
        if (columnName == null || columnName.isEmpty()) {
            columnName = NamingUtils.toSnakeCase(propertyName);
        }

        return new CommonFieldInfo(
            name,
            propertyName,
            columnName,
            fieldType,
            isId,
            isGenerated,
            FieldSource.CONFIG
        );
    }
}
