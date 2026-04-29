package io.github.afgprojects.framework.data.sql.scope;

import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.Set;

/**
 * 数据权限用户上下文
 * <p>
 * 存储当前用户的权限相关信息，用于数据权限 SQL 改写时的占位符解析。
 * <p>
 * 包含以下信息：
 * <ul>
 *   <li>用户ID - 用于 SELF 类型权限控制</li>
 *   <li>部门ID - 用于 DEPT 类型权限控制</li>
 *   <li>可访问部门ID列表 - 用于 DEPT_AND_CHILD 类型权限控制</li>
 *   <li>租户ID - 用于多租户隔离</li>
 * </ul>
 */
public final class DataScopeUserContext {

    /**
     * 当前用户 ID
     */
    private final @Nullable Long userId;

    /**
     * 当前用户所属部门 ID
     */
    private final @Nullable Long deptId;

    /**
     * 当前用户可访问的部门 ID 集合（包含子部门）
     */
    private final Set<Long> accessibleDeptIds;

    /**
     * 当前租户 ID
     */
    private final @Nullable Long tenantId;

    /**
     * 是否拥有全部数据权限（管理员）
     */
    private final boolean allDataPermission;

    private DataScopeUserContext(
            @Nullable Long userId,
            @Nullable Long deptId,
            Set<Long> accessibleDeptIds,
            @Nullable Long tenantId,
            boolean allDataPermission) {
        this.userId = userId;
        this.deptId = deptId;
        this.accessibleDeptIds = accessibleDeptIds != null ? accessibleDeptIds : Collections.emptySet();
        this.tenantId = tenantId;
        this.allDataPermission = allDataPermission;
    }

    /**
     * 获取当前用户 ID
     *
     * @return 用户 ID，如果未登录则返回 null
     */
    public @Nullable Long getUserId() {
        return userId;
    }

    /**
     * 获取当前用户所属部门 ID
     *
     * @return 部门 ID，如果没有部门则返回 null
     */
    public @Nullable Long getDeptId() {
        return deptId;
    }

    /**
     * 获取当前用户可访问的部门 ID 集合
     * <p>
     * 包含本部门及子部门，用于 DEPT_AND_CHILD 类型
     *
     * @return 部门 ID 集合，不可修改
     */
    public Set<Long> getAccessibleDeptIds() {
        return Collections.unmodifiableSet(accessibleDeptIds);
    }

    /**
     * 获取当前租户 ID
     *
     * @return 租户 ID，如果没有租户则返回 null
     */
    public @Nullable Long getTenantId() {
        return tenantId;
    }

    /**
     * 是否拥有全部数据权限
     *
     * @return 是否是管理员等拥有全部权限的角色
     */
    public boolean isAllDataPermission() {
        return allDataPermission;
    }

    /**
     * 创建空的上下文
     *
     * @return 空的用户上下文
     */
    public static DataScopeUserContext empty() {
        return new DataScopeUserContext(null, null, Collections.emptySet(), null, false);
    }

    /**
     * 创建构建器
     *
     * @return 构建器实例
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 构建器
     */
    public static final class Builder {
        private Long userId;
        private Long deptId;
        private Set<Long> accessibleDeptIds = Collections.emptySet();
        private Long tenantId;
        private boolean allDataPermission = false;

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder deptId(Long deptId) {
            this.deptId = deptId;
            return this;
        }

        public Builder accessibleDeptIds(Set<Long> accessibleDeptIds) {
            this.accessibleDeptIds = accessibleDeptIds;
            return this;
        }

        public Builder tenantId(Long tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder allDataPermission(boolean allDataPermission) {
            this.allDataPermission = allDataPermission;
            return this;
        }

        public DataScopeUserContext build() {
            return new DataScopeUserContext(userId, deptId, accessibleDeptIds, tenantId, allDataPermission);
        }
    }
}