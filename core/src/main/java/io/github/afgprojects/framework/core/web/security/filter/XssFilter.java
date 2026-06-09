package io.github.afgprojects.framework.core.web.security.filter;

import org.jspecify.annotations.Nullable;

import io.github.afgprojects.framework.core.web.security.sanitizer.EnhancedInputSanitizer;
import io.github.afgprojects.framework.core.web.security.sanitizer.InputSecurityChecker;

/**
 * XSS 防护过滤器
 * <p>
 * 使用 {@link XssChecker} 检测请求参数中的 XSS 攻击。
 * 优先使用基于 OWASP AntiSamy 的 {@link InputSecurityChecker} 进行检测，
 * 比基于正则表达式的默认检测更可靠。
 * <p>
 * <b>安全警告：</b>无参构造函数使用正则表达式检测，存在绕过风险。
 * 推荐通过 Spring 依赖注入获取已配置 {@link InputSecurityChecker} 的实例。
 *
 * @see XssChecker
 * @see InputSecurityChecker
 * @see EnhancedInputSanitizer
 */
public class XssFilter extends AbstractSecurityFilter {

    private final XssChecker checker;

    /**
     * 创建 XSS 过滤器（回退到正则表达式检测）。
     * <p>
     * <b>安全警告：</b>此构造函数创建的过滤器使用基于正则表达式的检测，
     * 无法覆盖 HTML 实体编码等 XSS 绕过向量。推荐通过 Spring 依赖注入获取
     * 已配置 {@link InputSecurityChecker} 的实例。
     *
     * @deprecated 推荐使用 {@link #XssFilter(InputSecurityChecker)} 确保使用 AntiSamy 检测
     */
    @Deprecated(since = "1.0.0", forRemoval = false)
    public XssFilter() {
        this.checker = new XssChecker();
    }

    /**
     * 创建 XSS 过滤器，使用增强输入清洗器进行检测。
     *
     * @param sanitizer 增强输入清洗器，如果为 null 则回退到正则表达式检测
     * @deprecated 推荐使用 {@link #XssFilter(InputSecurityChecker)} 以支持接口抽象
     */
    @Deprecated(since = "1.0.0", forRemoval = false)
    public XssFilter(@Nullable EnhancedInputSanitizer sanitizer) {
        this.checker = new XssChecker(sanitizer);
    }

    /**
     * 创建 XSS 过滤器，使用输入安全检测器进行检测。
     *
     * @param securityChecker 输入安全检测器，如果为 null 则回退到正则表达式检测
     */
    public XssFilter(@Nullable InputSecurityChecker securityChecker) {
        this.checker = new XssChecker(securityChecker);
    }

    @Override
    protected SecurityChecker getChecker() {
        return checker;
    }
}
