package io.github.afgprojects.framework.core.web.i18n;

import java.io.IOException;
import java.util.Locale;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jspecify.annotations.NonNull;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Locale 解析过滤器
 * 从 HTTP 请求头 Accept-Language 解析语言设置并存入 LocaleContextHolder
 *
 * <p>支持的语言格式：
 * <ul>
 *   <li>zh-CN (简体中文)</li>
 *   <li>zh-TW (繁体中文)</li>
 *   <li>en (英文)</li>
 *   <li>en-US (美式英文)</li>
 * </ul>
 *
 * <p>使用方式：
 * <pre>
 * // 客户端请求时设置 Accept-Language 头
 * curl -H "Accept-Language: en" http://api.example.com/users
 * </pre>
 *
 * @since 1.0.0
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class LocaleFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        try {
            Locale locale = resolveLocale(request);
            LocaleContextHolder.setLocale(locale);
            filterChain.doFilter(request, response);
        } finally {
            LocaleContextHolder.resetLocaleContext();
        }
    }

    /**
     * 从请求解析 Locale
     * 解析 Accept-Language 头，格式如: zh-CN, en-US, en;q=0.9
     *
     * @param request HTTP 请求
     * @return 解析的 Locale，如果未找到则返回默认 Locale
     */
    protected Locale resolveLocale(HttpServletRequest request) {
        String acceptLanguage = request.getHeader("Accept-Language");
        if (acceptLanguage == null || acceptLanguage.isBlank()) {
            return Locale.getDefault();
        }

        // 解析 Accept-Language 头，格式如: zh-CN, en-US;q=0.9
        String[] languages = acceptLanguage.split(",");
        if (languages.length == 0) {
            return Locale.getDefault();
        }

        // 取第一个语言（优先级最高）
        String lang = languages[0].trim();

        // 去除质量因子（;q=0.9）
        int semicolonIndex = lang.indexOf(';');
        if (semicolonIndex > 0) {
            lang = lang.substring(0, semicolonIndex).trim();
        }

        return parseLocale(lang);
    }

    /**
     * 解析语言标签为 Locale
     * 支持格式: zh-CN, zh_CN, zh, en-US, en_US, en
     */
    private Locale parseLocale(String lang) {
        if (lang.isEmpty()) {
            return Locale.getDefault();
        }

        // 统一分隔符为 "-"
        String normalized = lang.replace("_", "-");

        if (normalized.contains("-")) {
            String[] parts = normalized.split("-", 2);
            return new Locale(parts[0], parts[1]);
        }

        return new Locale(normalized);
    }
}
