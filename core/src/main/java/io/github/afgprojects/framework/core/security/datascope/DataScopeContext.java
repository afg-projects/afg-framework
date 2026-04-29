package io.github.afgprojects.framework.core.security.datascope;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import lombok.Builder;
import lombok.Data;

/**
 * 数据权限上下文
 * <p>
 * 存储当前请求的数据权限信息，包括用户所属部门、
 * 部门层级结构、自定义权限条件等。
 * <p>
 * 数据权限上下文通常在用户登录时初始化，并在请求期间通过
 * {@link DataScopeContextHolder} 进行访问。
 */
@Data
@Builder
public class DataScopeContext {

    /**
     * 当前用户 ID
     */
    private Long userId;

    /**
     * 当前用户所属部门 ID
     */
    private @Nullable Long deptId;

    /**
     * 当前用户可访问的部门 ID 集合（包含子部门）
     * <p>
     * 用于 DEPT_AND_CHILD 类型的权限判断
     */
    @Builder.Default
    private Set<Long> accessibleDeptIds = new HashSet<>();

    /**
     * 自定义权限条件
     * <p>
     * 用于 CUSTOM 类型的权限控制，可直接附加到 SQL WHERE 子句
     */
    private @Nullable String customCondition;

    /**
     * 是否拥有全部数据权限
     * <p>
     * 管理员或特殊角色可能拥有全部数据权限
     */
    @Builder.Default
    private boolean allDataPermission = false;

    /**
     * 是否忽略数据权限
     * <p>
     * 系统操作或特殊场景下可临时忽略数据权限
     */
    @Builder.Default
    private boolean ignoreDataScope = false;

    /**
     * 获取不可修改的部门 ID 集合
     *
     * @return 部门 ID 集合
     */
    public Set<Long> getAccessibleDeptIds() {
        return Collections.unmodifiableSet(accessibleDeptIds);
    }

    /**
     * 添加可访问部门 ID
     *
     * @param deptId 部门 ID
     */
    public void addAccessibleDeptId(Long deptId) {
        accessibleDeptIds.add(deptId);
    }

    /**
     * 添加多个可访问部门 ID
     *
     * @param deptIds 部门 ID 集合
     */
    public void addAccessibleDeptIds(Set<Long> deptIds) {
        accessibleDeptIds.addAll(deptIds);
    }

    /**
     * 判断是否有部门权限
     *
     * @param deptId 部门 ID
     * @return 是否有权限
     */
    public boolean hasDeptPermission(Long deptId) {
        if (allDataPermission || ignoreDataScope) {
            return true;
        }
        if (this.deptId != null && this.deptId.equals(deptId)) {
            return true;
        }
        return accessibleDeptIds.contains(deptId);
    }

    /**
     * 判断是否是自己的数据
     *
     * @param userId 用户 ID
     * @return 是否是自己的数据
     */
    public boolean isSelfData(Long userId) {
        if (allDataPermission || ignoreDataScope) {
            return true;
        }
        return this.userId != null && this.userId.equals(userId);
    }

    /**
     * 创建空的上下文
     *
     * @return 空的数据权限上下文
     */
    public static DataScopeContext empty() {
        return DataScopeContext.builder().build();
    }

    /**
     * 创建拥有全部数据权限的上下文（管理员上下文）
     *
     * @param userId 用户 ID
     * @return 管理员数据权限上下文
     */
    public static DataScopeContext allPermission(Long userId) {
        return DataScopeContext.builder()
                .userId(userId)
                .allDataPermission(true)
                .build();
    }
}