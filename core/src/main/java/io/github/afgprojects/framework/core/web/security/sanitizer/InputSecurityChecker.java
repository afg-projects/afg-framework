package io.github.afgprojects.framework.core.web.security.sanitizer;

import org.jspecify.annotations.Nullable;

/**
 * 输入安全检测接口。
 * <p>
 * 定义 XSS 检测和 HTML 清洗的标准 API，使 {@link XssChecker} 和安全过滤器
 * 可以基于接口工作而非硬依赖 {@link EnhancedInputSanitizer} 实现类。
 * <p>
 * 当 AntiSamy 不在 classpath 上时，{@link NoOpInputSanitizer} 作为降级实现，
 * 不提供任何检测能力（所有方法返回 false/原样返回）。
 *
 * @see EnhancedInputSanitizer
 * @see NoOpInputSanitizer
 */
public interface InputSecurityChecker {

    /**
     * 检测是否包含 XSS 攻击
     *
     * @param input 输入字符串
     * @return 如果检测到 XSS 攻击返回 true
     */
    boolean containsXss(@Nullable String input);

    /**
     * 清洗 HTML 输入
     *
     * @param input 输入字符串
     * @return 清洗后的安全字符串
     */
    @Nullable String sanitizeHtml(@Nullable String input);
}