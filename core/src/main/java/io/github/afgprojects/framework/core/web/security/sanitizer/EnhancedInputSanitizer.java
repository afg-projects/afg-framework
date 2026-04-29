package io.github.afgprojects.framework.core.web.security.sanitizer;

import java.util.Set;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.owasp.validator.html.AntiSamy;
import org.owasp.validator.html.CleanResults;
import org.owasp.validator.html.Policy;
import org.owasp.validator.html.PolicyException;
import org.owasp.validator.html.ScanException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.afgprojects.framework.core.web.security.autoconfigure.AfgSecurityProperties;

/**
 * 增强的输入清洗工具类
 * <p>
 * 使用 OWASP AntiSamy 进行 XSS 防护，提供比正则表达式更可靠的 HTML 清洗能力。
 * <p>
 * <strong>安全说明：</strong>
 * <ul>
 *   <li>XSS 防护：使用 AntiSamy 进行 HTML 清洗，支持严格模式和富文本模式</li>
 *   <li>SQL 注入检测：仅作为辅助检测层，核心防护应依赖参数化查询</li>
 * </ul>
 *
 * @see <a href="https://owasp.org/www-project-antisamy/">OWASP AntiSamy</a>
 */
public class EnhancedInputSanitizer {

    private static final Logger log = LoggerFactory.getLogger(EnhancedInputSanitizer.class);

    /**
     * AntiSamy 实例（线程安全）
     */
    private final AntiSamy antiSamy;

    /**
     * 严格策略（移除所有 HTML）- 使用 slashdot 策略
     */
    private final Policy strictPolicy;

    /**
     * 富文本策略（允许安全标签）- 使用 tinymce 策略
     */
    private final Policy richTextPolicy;

    private final AfgSecurityProperties.XssConfig xssConfig;
    private final AfgSecurityProperties.SqlInjectionConfig sqlConfig;

    /**
     * 创建增强输入清洗器
     *
     * @param properties 安全配置属性
     */
    public EnhancedInputSanitizer(@NonNull AfgSecurityProperties properties) {
        this.xssConfig = properties.getXss();
        this.sqlConfig = properties.getSqlInjection();

        try {
            this.antiSamy = new AntiSamy();
            // 使用 AntiSamy 内置策略
            // slashdot 策略非常严格，只允许极少数标签
            this.strictPolicy = Policy.getInstance(getClass().getResourceAsStream("/antisamy-slashdot.xml"));
            // tinymce 策略允许富文本编辑器常用的标签
            this.richTextPolicy = Policy.getInstance(getClass().getResourceAsStream("/antisamy-tinymce.xml"));
        } catch (PolicyException e) {
            throw new IllegalStateException("Failed to load AntiSamy policy", e);
        }
    }

    /**
     * 检测是否包含 XSS 攻击
     * <p>
     * 通过比较清洗前后的内容判断是否存在恶意内容
     *
     * @param input 输入字符串
     * @return 如果检测到 XSS 攻击返回 true
     */
    public boolean containsXss(@Nullable String input) {
        if (input == null || input.isBlank()) {
            return false;
        }

        try {
            CleanResults results = antiSamy.scan(input, strictPolicy);
            // 如果清洗后内容与原内容不同，说明存在危险内容
            return !results.getCleanHTML().equals(input);
        } catch (ScanException | PolicyException e) {
            // 扫描出错时保守处理，认为可能存在风险
            log.debug("XSS scan error: {}", e.getMessage());
            return true;
        }
    }

    /**
     * 清洗 HTML 输入
     * <p>
     * 根据配置选择严格模式或富文本模式：
     * <ul>
     *   <li>严格模式：移除所有 HTML 标签</li>
     *   <li>富文本模式：保留安全的 HTML 标签</li>
     * </ul>
     *
     * @param input 输入字符串
     * @return 清洗后的安全字符串
     */
    public @Nullable String sanitizeHtml(@Nullable String input) {
        if (input == null || input.isBlank()) {
            return input;
        }

        try {
            Policy policy = xssConfig.isRichTextMode() ? richTextPolicy : strictPolicy;
            CleanResults results = antiSamy.scan(input, policy);

            // 记录清洗信息（仅 DEBUG 级别）
            if (log.isDebugEnabled() && !results.getErrorMessages().isEmpty()) {
                log.debug("HTML sanitized: {} error(s) found", results.getErrorMessages().size());
            }

            return results.getCleanHTML();
        } catch (ScanException | PolicyException e) {
            log.warn("HTML sanitization failed, returning empty string for safety: {}", e.getMessage());
            return "";
        }
    }

    /**
     * 检测是否包含 SQL 注入
     * <p>
     * <strong>警告：</strong>此检测仅作为辅助安全层，不可替代参数化查询！
     * 正则表达式无法覆盖所有 SQL 注入场景，核心防护应使用 PreparedStatement 或 ORM 框架。
     *
     * @param input 输入字符串
     * @return 如果检测到可能的 SQL 注入返回 true
     */
    public boolean containsSqlInjection(@Nullable String input) {
        if (input == null || input.isBlank()) {
            return false;
        }
        return InputSanitizer.containsSqlInjection(input);
    }

    /**
     * 清洗 SQL 输入
     * <p>
     * <strong>警告：</strong>此方法仅用于日志等非执行场景，不可用于参数清洗！
     * SQL 查询必须使用参数化查询（PreparedStatement）或 ORM 框架。
     *
     * @param input 输入字符串
     * @return 转义后的字符串
     * @deprecated 此方法仅用于日志输出，不要用于 SQL 参数处理
     */
    @Deprecated(since = "1.0.0", forRemoval = false)
    public @Nullable String sanitizeSql(@Nullable String input) {
        if (input == null || input.isBlank()) {
            return input;
        }
        return input.replace("'", "''")
                .replace(";", "")
                .replace("--", "")
                .replace("/*", "")
                .replace("*/", "");
    }

    /**
     * 综合安全检查
     *
     * @param input 输入字符串
     * @return 如果安全返回 null，否则返回威胁类型（"XSS" 或 "SQL_INJECTION"）
     */
    public @Nullable String checkSecurity(@Nullable String input) {
        if (input == null || input.isBlank()) {
            return null;
        }

        if (xssConfig.isEnabled() && containsXss(input)) {
            return "XSS";
        }

        if (sqlConfig.isEnabled() && containsSqlInjection(input)) {
            return "SQL_INJECTION";
        }

        return null;
    }

    /**
     * 综合清洗
     * <p>
     * 对输入进行 XSS 清洗。SQL 注入防护应使用参数化查询。
     *
     * @param input 输入字符串
     * @return 清洗后的安全字符串
     */
    public @Nullable String sanitize(@Nullable String input) {
        if (input == null || input.isBlank()) {
            return input;
        }

        if (xssConfig.isEnabled()) {
            return sanitizeHtml(input);
        }

        return input;
    }

    /**
     * 获取清洗错误信息（用于调试）
     *
     * @param input 输入字符串
     * @return 清洗过程中的错误信息列表
     */
    public java.util.@NonNull List<String> getScanErrors(@Nullable String input) {
        if (input == null || input.isBlank()) {
            return java.util.List.of();
        }

        try {
            Policy policy = xssConfig.isRichTextMode() ? richTextPolicy : strictPolicy;
            CleanResults results = antiSamy.scan(input, policy);
            return results.getErrorMessages().stream()
                    .map(Object::toString)
                    .toList();
        } catch (ScanException | PolicyException e) {
            return java.util.List.of("Scan error: " + e.getMessage());
        }
    }
}
