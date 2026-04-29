package io.github.afgprojects.framework.data.sql.scope;

import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * 数据权限上下文提供者接口
 * <p>
 * 用于在 SQL 改写时提供当前用户的权限上下文数据。
 * 实现类可以从 Spring Security、ThreadLocal 或其他上下文中获取数据。
 * <p>
 * 使用示例：
 * <pre>
 * // 基于 Spring Security 的实现
 * public class SpringSecurityDataScopeContextProvider implements DataScopeContextProvider {
 *     &#64;Override
 *     public Long getCurrentUserId() {
 *         Authentication auth = SecurityContextHolder.getContext().getAuthentication();
 *         if (auth != null && auth.getPrincipal() instanceof UserDetails) {
 *             return ((CustomUserDetails) auth.getPrincipal()).getUserId();
 *         }
 *         return null;
 *     }
 * }
 * </pre>
 */
@FunctionalInterface
public interface DataScopeContextProvider {

    /**
     * 获取当前数据权限上下文
     *
     * @return 数据权限上下文，如果无法获取则返回 null
     */
    @Nullable
    DataScopeUserContext provide();

    /**
     * 获取默认的空实现
     *
     * @return 返回一个返回空上下文的提供者
     */
    static DataScopeContextProvider empty() {
        return () -> null;
    }
}
