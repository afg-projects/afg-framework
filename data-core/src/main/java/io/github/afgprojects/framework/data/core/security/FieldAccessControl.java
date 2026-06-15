package io.github.afgprojects.framework.data.core.security;

import java.util.Set;

/**
 * 字段级访问控制 SPI 接口。
 * <p>
 * 提供基于当前安全上下文的字段读写权限检查。用于实现列级访问控制：
 * <ul>
 *   <li>可读字段：出现在 SQL SELECT 中的字段</li>
 *   <li>可写字段：可以出现在 SQL UPDATE SET 中的字段</li>
 * </ul>
 *
 * <h3>工作原理</h3>
 * <p>
 * 在构建 SELECT/UPDATE SQL 时，框架通过此 SPI 获取当前用户可读/可写的字段集合，
 * 将未授权字段从 SQL 语句中排除，实现数据源级别的列级安全控制。
 *
 * <h3>实现示例（Casbin）</h3>
 * <pre>{@code
 * @Component
 * public class CasbinFieldAccessControl implements FieldAccessControl {
 *     private final AfgEnforcer enforcer;
 *
 *     @Override
 *     public Set<String> getReadableFields(Class<?> entityClass) {
 *         return getFieldsByPermission(entityClass, "read");
 *     }
 *
 *     @Override
 *     public Set<String> getWritableFields(Class<?> entityClass) {
 *         return getFieldsByPermission(entityClass, "write");
 *     }
 * }
 * }</pre>
 *
 * @see <a href="field-level-access-control.md">field-level access control research</a>
 */
public interface FieldAccessControl {

    /**
     * 获取当前用户可以读取的字段名集合。
     * <p>
     * 返回的集合包含所有当前用户有权读取的字段名（Java 属性名）。
     * 如果返回空集合或包含所有字段，表示无字段级限制。
     *
     * @param entityClass 实体类
     * @return 可读字段名集合，为空表示所有字段均可读
     */
    Set<String> getReadableFields(Class<?> entityClass);

    /**
     * 获取当前用户可以写入的字段名集合。
     * <p>
     * 返回的集合包含所有当前用户有权修改的字段名（Java 属性名）。
     * 如果返回空集合或包含所有字段，表示无字段级限制。
     *
     * @param entityClass 实体类
     * @return 可写字段名集合，为空表示所有字段均可写
     */
    Set<String> getWritableFields(Class<?> entityClass);
}
