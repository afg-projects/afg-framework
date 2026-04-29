package io.github.afgprojects.framework.data.sql.scope;

import org.jspecify.annotations.Nullable;

import java.util.Set;
import java.util.function.Supplier;

/**
 * DataScopeContextProvider 工厂类
 * <p>
 * 提供便捷的方法创建常见的数据权限上下文提供者。
 * <p>
 * 使用示例：
 * <pre>
 * // 从 ThreadLocal 获取上下文
 * DataScopeContextProvider provider = DataScopeContextProviders.fromThreadLocal(
 *     () -> DataScopeUserContext.builder()
 *         .userId(UserContextHolder.getUserId())
 *         .deptId(UserContextHolder.getDeptId())
 *         .build()
 * );
 *
 * // 从 Spring Security 获取上下文
 * DataScopeContextProvider provider = DataScopeContextProviders.fromSpringSecurity(
 *     () -> SecurityContextHolder.getContext().getAuthentication()
 * );
 * </pre>
 */
public final class DataScopeContextProviders {

    private DataScopeContextProviders() {}

    /**
     * 创建空提供者
     *
     * @return 始终返回 null 的提供者
     */
    public static DataScopeContextProvider empty() {
        return DataScopeContextProvider.empty();
    }

    /**
     * 从 Supplier 创建提供者
     *
     * @param supplier 用户上下文供应器
     * @return 数据权限上下文提供者
     */
    public static DataScopeContextProvider fromSupplier(Supplier<DataScopeUserContext> supplier) {
        return supplier::get;
    }

    /**
     * 创建固定上下文的提供者
     * <p>
     * 适用于测试或特定场景，始终返回相同的上下文。
     *
     * @param context 固定的用户上下文
     * @return 数据权限上下文提供者
     */
    public static DataScopeContextProvider fixed(DataScopeUserContext context) {
        return () -> context;
    }

    /**
     * 创建具有管理员权限的提供者
     * <p>
     * 适用于系统操作或管理员场景。
     *
     * @param userId 用户ID
     * @return 拥有全部权限的上下文提供者
     */
    public static DataScopeContextProvider admin(Long userId) {
        DataScopeUserContext context = DataScopeUserContext.builder()
                .userId(userId)
                .allDataPermission(true)
                .build();
        return () -> context;
    }

    /**
     * 创建普通用户上下文的提供者
     *
     * @param userId 用户ID
     * @param deptId 部门ID
     * @return 数据权限上下文提供者
     */
    public static DataScopeContextProvider user(Long userId, @Nullable Long deptId) {
        DataScopeUserContext context = DataScopeUserContext.builder()
                .userId(userId)
                .deptId(deptId)
                .build();
        return () -> context;
    }

    /**
     * 创建具有部门权限的用户上下文提供者
     *
     * @param userId            用户ID
     * @param deptId            部门ID
     * @param accessibleDeptIds 可访问的部门ID集合
     * @return 数据权限上下文提供者
     */
    public static DataScopeContextProvider userWithDepts(
            Long userId,
            @Nullable Long deptId,
            @Nullable Set<Long> accessibleDeptIds) {
        DataScopeUserContext context = DataScopeUserContext.builder()
                .userId(userId)
                .deptId(deptId)
                .accessibleDeptIds(accessibleDeptIds != null ? accessibleDeptIds : Set.of())
                .build();
        return () -> context;
    }

    /**
     * 创建带租户的用户上下文提供者
     *
     * @param userId   用户ID
     * @param deptId   部门ID
     * @param tenantId 租户ID
     * @return 数据权限上下文提供者
     */
    public static DataScopeContextProvider userWithTenant(
            Long userId,
            @Nullable Long deptId,
            @Nullable Long tenantId) {
        DataScopeUserContext context = DataScopeUserContext.builder()
                .userId(userId)
                .deptId(deptId)
                .tenantId(tenantId)
                .build();
        return () -> context;
    }

    /**
     * 创建完整的用户上下文提供者
     *
     * @param userId            用户ID
     * @param deptId            部门ID
     * @param accessibleDeptIds 可访问的部门ID集合
     * @param tenantId          租户ID
     * @param allDataPermission 是否拥有全部数据权限
     * @return 数据权限上下文提供者
     */
    public static DataScopeContextProvider full(
            Long userId,
            @Nullable Long deptId,
            @Nullable Set<Long> accessibleDeptIds,
            @Nullable Long tenantId,
            boolean allDataPermission) {
        DataScopeUserContext context = DataScopeUserContext.builder()
                .userId(userId)
                .deptId(deptId)
                .accessibleDeptIds(accessibleDeptIds != null ? accessibleDeptIds : Set.of())
                .tenantId(tenantId)
                .allDataPermission(allDataPermission)
                .build();
        return () -> context;
    }

    /**
     * 从上下文持有者创建提供者
     * <p>
     * 适用于与 Spring 等框架集成的场景，可以从框架的上下文持有者获取用户信息。
     *
     * @param contextHolder 上下文持有者
     * @return 数据权限上下文提供者
     */
    public static DataScopeContextProvider fromContextHolder(ContextHolder contextHolder) {
        return () -> {
            Long userId = contextHolder.getUserId();
            Long deptId = contextHolder.getDeptId();
            Set<Long> deptIds = contextHolder.getAccessibleDeptIds();
            Long tenantId = contextHolder.getTenantId();

            if (userId == null && deptId == null && tenantId == null) {
                return null;
            }

            return DataScopeUserContext.builder()
                    .userId(userId)
                    .deptId(deptId)
                    .accessibleDeptIds(deptIds != null ? deptIds : Set.of())
                    .tenantId(tenantId)
                    .build();
        };
    }

    /**
     * 上下文持有者接口
     * <p>
     * 用于从框架特定的上下文持有者获取用户信息。
     */
    @FunctionalInterface
    public interface ContextHolder {
        /**
         * 获取用户ID
         */
        @Nullable Long getUserId();

        /**
         * 获取部门ID
         */
        default @Nullable Long getDeptId() {
            return null;
        }

        /**
         * 获取可访问的部门ID集合
         */
        default @Nullable Set<Long> getAccessibleDeptIds() {
            return null;
        }

        /**
         * 获取租户ID
         */
        default @Nullable Long getTenantId() {
            return null;
        }
    }
}