package io.github.afgprojects.framework.core.web.security.filter;

import org.jspecify.annotations.Nullable;

import io.github.afgprojects.framework.core.web.security.sanitizer.EnhancedInputSanitizer;
import io.github.afgprojects.framework.core.web.security.sanitizer.InputSanitizer;
import io.github.afgprojects.framework.core.web.security.sanitizer.InputSecurityChecker;

/**
 * XSS 安全检查器
 * <p>
 * 优先使用 {@link InputSecurityChecker}（基于 OWASP AntiSamy）进行 XSS 检测，
 * 因为其比基于正则表达式的 {@link InputSanitizer} 更可靠，能够覆盖 HTML 实体编码、
 * 未覆盖标签等绕过向量。
 * <p>
 * 检测优先级：
 * <ol>
 *   <li>{@link InputSecurityChecker} — 如果提供，使用其 containsXss 方法（通常由 AntiSamy 支持）</li>
 *   <li>{@link InputSanitizer} — 回退到基于正则表达式的静态检测</li>
 * </ol>
 * <p>
 * <b>安全警告：</b>建议始终通过 Spring 依赖注入使用此检查器，确保 AntiSamy 生效。
 * 无参构造函数使用正则表达式检测，存在绕过风险。
 *
 * @see InputSecurityChecker
 * @see EnhancedInputSanitizer
 * @see InputSanitizer
 */
public class XssChecker implements SecurityChecker {

    private final @Nullable InputSecurityChecker securityChecker;

    /**
     * 创建 XSS 检查器（回退到正则表达式检测）。
     * <p>
     * <b>安全警告：</b>此构造函数创建的检查器使用基于正则表达式的 {@link InputSanitizer}，
     * 无法覆盖 HTML 实体编码等 XSS 绕过向量。推荐通过 Spring 依赖注入获取已配置
     * {@link InputSecurityChecker} 的实例。
     *
     * @deprecated 推荐使用 {@link #XssChecker(InputSecurityChecker)} 确保使用 AntiSamy 检测
     */
    @Deprecated(since = "1.0.0", forRemoval = false)
    public XssChecker() {
        this.securityChecker = null;
    }

    /**
     * 创建 XSS 检查器，使用增强输入清洗器进行检测。
     *
     * @param sanitizer 增强输入清洗器，如果为 null 则回退到正则表达式检测
     * @deprecated 推荐使用 {@link #XssChecker(InputSecurityChecker)} 以支持接口抽象
     */
    @Deprecated(since = "1.0.0", forRemoval = false)
    public XssChecker(@Nullable EnhancedInputSanitizer sanitizer) {
        this.securityChecker = sanitizer;
    }

    /**
     * 创建 XSS 检查器，使用输入安全检测器进行检测。
     *
     * @param securityChecker 输入安全检测器，如果为 null 则回退到正则表达式检测
     */
    public XssChecker(@Nullable InputSecurityChecker securityChecker) {
        this.securityChecker = securityChecker;
    }

    @Override
    public String getName() {
        return "XSS";
    }

    @Override
    public boolean containsThreat(String input) {
        if (securityChecker != null) {
            return securityChecker.containsXss(input);
        }
        return InputSanitizer.containsXss(input);
    }
}
