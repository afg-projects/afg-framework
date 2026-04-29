package io.github.afgprojects.framework.data.sql.scope;

/**
 * 数据权限占位符常量
 * <p>
 * 定义数据权限 SQL 改写时使用的占位符。
 * 这些占位符会在运行时被替换为实际的值。
 * <p>
 * 使用示例：
 * <pre>
 * // 在自定义条件中使用占位符
 * &#64;DataScope(
 *     table = "orders",
 *     column = "user_id",
 *     scopeType = DataScopeType.CUSTOM,
 *     customCondition = "user_id = #{currentUserId} AND status = 1"
 * )
 * public List&lt;Order&gt; listOrders() { ... }
 * </pre>
 */
public final class DataScopePlaceholders {

    private DataScopePlaceholders() {}

    /**
     * 当前用户ID
     * <p>
     * 用于 SELF 类型数据权限，替换为当前登录用户的ID。
     */
    public static final String CURRENT_USER_ID = "#{currentUserId}";

    /**
     * 当前用户部门ID
     * <p>
     * 用于 DEPT 类型数据权限，替换为当前用户所属部门的ID。
     */
    public static final String CURRENT_DEPT_ID = "#{currentDeptId}";

    /**
     * 当前用户部门ID列表
     * <p>
     * 用于 DEPT 类型（多部门场景），替换为用户所属部门ID列表。
     */
    public static final String CURRENT_USER_DEPT_IDS = "#{currentUserDeptIds}";

    /**
     * 当前用户部门及子部门ID列表
     * <p>
     * 用于 DEPT_AND_CHILD 类型数据权限，替换为用户可访问的所有部门ID（包含子部门）。
     */
    public static final String CURRENT_USER_DEPT_AND_CHILD_IDS = "#{currentUserDeptAndChildIds}";

    /**
     * 当前租户ID
     * <p>
     * 用于多租户场景，替换为当前用户的租户ID。
     */
    public static final String CURRENT_TENANT_ID = "#{currentTenantId}";
}