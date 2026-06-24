package io.github.afgprojects.framework.security.core.permission;

import io.github.afgprojects.framework.data.core.scope.DataScope;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * 数据权限服务接口。
 *
 * <p>提供数据范围权限控制服务。
 *
 * <h3>数据权限模型</h3>
 * <p>根据用户的数据范围配置，自动过滤查询结果。
 * <pre>{@code
 * // 获取用户的数据范围
 * DataScope scope = dataScopeService.getDataScope(userId, tenantId);
 *
 * // 获取用户可访问的部门 ID
 * Set<String> deptIds = dataScopeService.getAccessibleDeptIds(userId, tenantId);
 *
 * // 设置用户的数据范围
 * DataScope scope = DataScope.of("sys_user", "dept_id", DataScopeType.DEPT);
 * dataScopeService.setDataScope(userId, tenantId, scope);
 *
 * // 移除用户的数据范围
 * dataScopeService.removeDataScope(userId, tenantId);
 * }</pre>
 *
 * @since 1.0.0
 */
public interface DataScopeService {

    /**
     * 获取用户的数据范围配置。
     *
     * @param userId 用户 ID，永不为 null
     * @param tenantId 租户 ID，可为 null（非多租户场景）
     * @return 数据范围配置，永不为 null
     */
    @NonNull
    DataScope getDataScope(@NonNull String userId, @Nullable String tenantId);

    /**
     * 获取用户可访问的部门 ID 集合。
     *
     * <p>用于 DEPT_AND_CHILD 类型的权限判断。
     *
     * @param userId 用户 ID，永不为 null
     * @param tenantId 租户 ID，可为 null（非多租户场景）
     * @return 可访问的部门 ID 集合，永不为 null（可能为空集合）
     */
    @NonNull
    Set<String> getAccessibleDeptIds(@NonNull String userId, @Nullable String tenantId);

    /**
     * 设置用户的数据范围配置。
     *
     * @param userId 用户 ID，永不为 null
     * @param tenantId 租户 ID，可为 null（非多租户场景）
     * @param scope 数据范围配置，永不为 null
     */
    void setDataScope(@NonNull String userId, @Nullable String tenantId, @NonNull DataScope scope);

    /**
     * 移除用户的数据范围配置。
     *
     * @param userId 用户 ID，永不为 null
     * @param tenantId 租户 ID，可为 null（非多租户场景）
     */
    void removeDataScope(@NonNull String userId, @Nullable String tenantId);
}
