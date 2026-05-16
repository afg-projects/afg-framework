package io.github.afgprojects.framework.commons.naming;

/**
 * 命名转换工具类
 * <p>
 * 提供常用的命名风格转换方法：
 * <ul>
 *   <li>camelCase ↔ snake_case</li>
 *   <li>首字母大写/小写</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>{@code
 * // camelCase → snake_case
 * NamingUtils.toSnakeCase("userName")     // "user_name"
 * NamingUtils.toSnakeCase("isActive")     // "is_active"
 *
 * // snake_case → camelCase
 * NamingUtils.toCamelCase("user_name")    // "userName"
 * NamingUtils.toCamelCase("is_active")    // "isActive"
 *
 * // 首字母大写
 * NamingUtils.capitalize("userName")      // "UserName"
 *
 * // 首字母小写
 * NamingUtils.uncapitalize("UserName")    // "userName"
 * }</pre>
 */
public final class NamingUtils {

    private NamingUtils() {
        // 工具类禁止实例化
    }

    /**
     * camelCase 转 snake_case
     * <p>
     * 示例：
     * <ul>
     *   <li>"userName" → "user_name"</li>
     *   <li>"isActive" → "is_active"</li>
     *   <li>"id" → "id"</li>
     *   <li>"URL" → "u_r_l"</li>
     * </ul>
     *
     * @param name camelCase 格式的名称
     * @return snake_case 格式的名称
     */
    public static String toSnakeCase(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (i > 0 && Character.isUpperCase(c)) {
                result.append('_');
            }
            result.append(Character.toLowerCase(c));
        }
        return result.toString();
    }

    /**
     * snake_case 转 camelCase
     * <p>
     * 示例：
     * <ul>
     *   <li>"user_name" → "userName"</li>
     *   <li>"is_active" → "isActive"</li>
     *   <li>"id" → "id"</li>
     * </ul>
     *
     * @param name snake_case 格式的名称
     * @return camelCase 格式的名称
     */
    public static String toCamelCase(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        StringBuilder result = new StringBuilder();
        boolean nextUpper = false;
        for (char c : name.toLowerCase().toCharArray()) {
            if (c == '_') {
                nextUpper = true;
            } else {
                result.append(nextUpper ? Character.toUpperCase(c) : c);
                nextUpper = false;
            }
        }
        return result.toString();
    }

    /**
     * 首字母大写
     * <p>
     * 示例：
     * <ul>
     *   <li>"userName" → "UserName"</li>
     *   <li>"id" → "Id"</li>
     * </ul>
     *
     * @param name 名称
     * @return 首字母大写的名称
     */
    public static String capitalize(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    /**
     * 首字母小写
     * <p>
     * 示例：
     * <ul>
     *   <li>"UserName" → "userName"</li>
     *   <li>"Id" → "id"</li>
     * </ul>
     *
     * @param name 名称
     * @return 首字母小写的名称
     */
    public static String uncapitalize(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }
}
