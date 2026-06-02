package io.github.afgprojects.framework.ai.core.api.tool;

import org.jspecify.annotations.NonNull;

/**
 * 工具上下文提供者接口。
 *
 * <p>用于在工具执行时获取当前的安全上下文。
 * 通常从 Spring Security 上下文或请求上下文中获取。
 *
 * <p>使用示例：
 * <pre>{@code
 * // 从 Spring Security 获取上下文
 * public class SpringSecurityToolContextProvider implements ToolContextProvider {
 *     @Override
 *     public ToolContext provide() {
 *         Authentication auth = SecurityContextHolder.getContext().getAuthentication();
 *         if (auth instanceof AfgAuthentication afgAuth) {
 *             AfgUserDetails user = afgAuth.getUserDetails();
 *             return ToolContext.builder()
 *                 .userId(user.getUserId())
 *                 .userDetails(user)
 *                 .tenantId(user.getTenantId())
 *                 .build();
 *         }
 *         return ToolContext.empty();
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
@FunctionalInterface
public interface ToolContextProvider {

    /**
     * 提供工具上下文。
     *
     * <p>在工具执行前调用，获取当前的安全上下文。
     *
     * @return 工具上下文，永不为 null（未认证时返回空上下文）
     */
    @NonNull
    ToolContext provide();

    /**
     * 创建空上下文提供者。
     *
     * @return 总是返回空上下文的提供者
     */
    static @NonNull ToolContextProvider empty() {
        return ToolContext::empty;
    }

    /**
     * 创建固定上下文提供者。
     *
     * @param context 固定上下文
     * @return 总是返回固定上下文的提供者
     */
    static @NonNull ToolContextProvider fixed(@NonNull ToolContext context) {
        return () -> context;
    }
}
