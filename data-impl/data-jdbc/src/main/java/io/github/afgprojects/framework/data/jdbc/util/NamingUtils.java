package io.github.afgprojects.framework.data.jdbc.util;

import io.github.afgprojects.framework.data.core.metadata.EntityMetadata;
import org.jspecify.annotations.Nullable;

/**
 * 命名转换工具类
 * <p>
 * 提供数据库命名规范与 Java 命名规范之间的转换方法：
 * <ul>
 *   <li>snake_case (数据库) ↔ camelCase (Java)</li>
 *   <li>实体类名 → 表名推断</li>
 * </ul>
 * <p>
 * 特殊处理阿里规约 boolean 字段：
 * <ul>
 *   <li>列名 is_active → 字段名 active（如果实体有 active 字段）</li>
 *   <li>列名 is_deleted → 字段名 deleted（如果实体有 deleted 字段）</li>
 * </ul>
 */
public final class NamingUtils {

    private NamingUtils() {
        // 工具类禁止实例化
    }

    /**
     * 列名转字段名（snake_case to camelCase）
     * <p>
     * 特殊处理阿里规约 boolean 字段：
     * <ul>
     *   <li>列名 is_active → 字段名 active（如果实体有 active 字段）</li>
     *   <li>列名 is_deleted → 字段名 deleted（如果实体有 deleted 字段）</li>
     * </ul>
     *
     * @param columnName 数据库列名（snake_case）
     * @return Java 字段名（camelCase）
     */
    public static String columnNameToFieldName(String columnName) {
        StringBuilder fieldName = new StringBuilder();
        boolean nextUpper = false;
        for (char c : columnName.toLowerCase().toCharArray()) {
            if (c == '_') {
                nextUpper = true;
            } else {
                fieldName.append(nextUpper ? Character.toUpperCase(c) : c);
                nextUpper = false;
            }
        }
        return fieldName.toString();
    }

    /**
     * 列名转字段名（snake_case to camelCase），支持阿里规约 boolean 字段处理
     * <p>
     * 特殊处理阿里规约 boolean 字段：
     * <ul>
     *   <li>列名 is_active → 字段名 active（如果实体有 active 字段）</li>
     *   <li>列名 is_deleted → 字段名 deleted（如果实体有 deleted 字段）</li>
     * </ul>
     *
     * @param columnName 数据库列名（snake_case）
     * @param metadata   实体元数据（用于检查字段是否存在）
     * @return Java 字段名（camelCase）
     */
    public static String columnNameToFieldName(String columnName, @Nullable EntityMetadata<?> metadata) {
        String result = columnNameToFieldName(columnName);

        // 阿里规约 boolean 字段特殊处理：
        // 如果转换后的字段名以 "is" 开头，且实体有不带 "is" 的对应字段，则使用不带 "is" 的字段名
        // 例如：is_active → active, is_deleted → deleted
        if (metadata != null && result.startsWith("is") && result.length() > 2 && Character.isUpperCase(result.charAt(2))) {
            String strippedName = Character.toLowerCase(result.charAt(2)) + result.substring(3);
            if (metadata.getField(strippedName) != null) {
                return strippedName;
            }
        }

        return result;
    }

    /**
     * 字段名转列名（camelCase to snake_case）
     *
     * @param fieldName Java 字段名（camelCase）
     * @return 数据库列名（snake_case）
     */
    public static String fieldNameToColumnName(String fieldName) {
        StringBuilder columnName = new StringBuilder();
        for (int i = 0; i < fieldName.length(); i++) {
            char c = fieldName.charAt(i);
            if (i > 0 && Character.isUpperCase(c)) {
                columnName.append('_');
            }
            columnName.append(Character.toLowerCase(c));
        }
        return columnName.toString();
    }

    /**
     * 根据实体类推断表名
     * <p>
     * 将类名转换为 snake_case 表名，例如：
     * <ul>
     *   <li>User → user</li>
     *   <li>OrderItem → order_item</li>
     *   <li>SysUserRole → sys_user_role</li>
     * </ul>
     *
     * @param entityClass 实体类
     * @return 推断的表名（snake_case）
     */
    public static String inferTableName(Class<?> entityClass) {
        String className = entityClass.getSimpleName();
        StringBuilder tableName = new StringBuilder();
        for (int i = 0; i < className.length(); i++) {
            char c = className.charAt(i);
            if (i > 0 && Character.isUpperCase(c)) {
                tableName.append('_');
            }
            tableName.append(Character.toLowerCase(c));
        }
        return tableName.toString();
    }
}
