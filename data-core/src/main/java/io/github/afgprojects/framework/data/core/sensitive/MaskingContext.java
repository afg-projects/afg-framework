package io.github.afgprojects.framework.data.core.sensitive;

import org.jspecify.annotations.Nullable;

/**
 * 脱敏上下文 SPI 接口。
 * <p>
 * 决定当前上下文下指定字段是否应该被脱敏。支持按角色差异化脱敏：
 * 管理员角色可以查看原始值，普通用户只能看到脱敏后的值。
 *
 * <h3>默认行为</h3>
 * <p>
 * 框架提供 {@code NoOpMaskingContext}（始终返回 true，即始终脱敏）和
 * {@code SecurityMaskingContext}（基于 Spring Security 角色判断）两种默认实现。
 *
 * <h3>自定义实现</h3>
 * <pre>{@code
 * @Component
 * public class CustomMaskingContext implements MaskingContext {
 *     @Override
 *     public boolean shouldMask(String fieldName, String sensitiveType) {
 *         // 管理员可查看原始值
 *         return !SecurityContextHolder.getContext()
 *             .getAuthentication().getAuthorities()
 *             .contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
 *     }
 * }
 * }</pre>
 *
 * @see MaskingStrategy
 * @see io.github.afgprojects.framework.apt.entity.SensitiveField
 */
public interface MaskingContext {

    /**
     * 决定当前上下文中是否应该对字段进行脱敏。
     *
     * @param fieldName     字段名（Java 属性名）
     * @param sensitiveType 敏感数据类型（来自 {@code SensitiveType} 枚举名称）
     * @return true 表示需要脱敏，false 表示保留原始值
     */
    boolean shouldMask(String fieldName, String sensitiveType);
}
