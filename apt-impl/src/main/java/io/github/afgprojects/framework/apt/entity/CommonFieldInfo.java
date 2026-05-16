package io.github.afgprojects.framework.apt.entity;

import io.github.afgprojects.framework.commons.naming.NamingUtils;

import javax.lang.model.element.Element;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

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
     * <p>
     * 注意：在注解处理器环境中，访问 Class<?> 类型属性会抛出 MirroredTypeException，
     * 需要通过捕获异常来获取 TypeMirror。
     *
     * @param annotation   注解实例
     * @param source       字段来源
     * @param typeUtils    Types 工具类
     * @param element      注解所在元素（用于错误报告）
     * @return CommonFieldInfo 实例
     */
    static CommonFieldInfo fromAnnotation(CommonFieldDefinition annotation, FieldSource source,
                                          Types typeUtils, Element element) {
        String columnName = annotation.columnName();
        if (columnName == null || columnName.isEmpty()) {
            // 自动转换为 snake_case
            columnName = NamingUtils.toSnakeCase(annotation.propertyName());
        }

        // 处理 fieldType - 捕获 MirroredTypeException
        String fieldType;
        try {
            // 尝试直接访问（在非注解处理器环境中可能成功）
            fieldType = annotation.fieldType().getName();
        } catch (MirroredTypeException e) {
            // 在注解处理器环境中，通过 TypeMirror 获取类型全限定名
            TypeMirror typeMirror = e.getTypeMirror();
            fieldType = typeUtils.erasure(typeMirror).toString();
        }

        return new CommonFieldInfo(
            annotation.name(),
            annotation.propertyName(),
            columnName,
            fieldType,
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
