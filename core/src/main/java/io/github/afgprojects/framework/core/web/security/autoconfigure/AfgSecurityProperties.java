package io.github.afgprojects.framework.core.web.security.autoconfigure;

import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * AFG 安全配置属性
 */
@Data
@ConfigurationProperties(prefix = "afg.security")
public class AfgSecurityProperties {

    /**
     * XSS 防护配置
     */
    private XssConfig xss = new XssConfig();

    /**
     * SQL 注入防护配置
     */
    private SqlInjectionConfig sqlInjection = new SqlInjectionConfig();

    /**
     * 敏感数据脱敏配置
     */
    private SensitiveDataConfig sensitiveData = new SensitiveDataConfig();

    /**
     * 安全头配置
     */
    private SecurityHeaderConfig securityHeader = new SecurityHeaderConfig();

    /**
     * XSS 防护配置
     */
    @Data
    public static class XssConfig {
        /**
         * 是否启用 XSS 防护
         */
        private boolean enabled = true;

        /**
         * 是否启用富文本模式（允许部分安全 HTML）
         */
        private boolean richTextMode;

        /**
         * 允许的 HTML 标签（富文本模式）
         */
        private Set<String> allowedTags = Set.of(
                "p", "br", "b", "i", "u", "strong", "em", "h1", "h2", "h3", "h4", "h5", "h6", "ul", "ol", "li", "a",
                "img", "span", "div");

        /**
         * 允许的 HTML 属性（富文本模式）
         */
        private Set<String> allowedAttributes = Set.of("href", "src", "alt", "title", "class", "id", "target");
    }

    /**
     * SQL 注入防护配置
     */
    @Data
    public static class SqlInjectionConfig {
        /**
         * 是否启用 SQL 注入检测
         */
        private boolean enabled = true;

        /**
         * 是否拒绝检测到的请求
         */
        private boolean rejectOnDetection = true;

        /**
         * 是否记录检测日志
         */
        private boolean logDetection = true;
    }

    /**
     * 敏感数据脱敏配置
     */
    @Data
    public static class SensitiveDataConfig {
        /**
         * 是否启用敏感数据脱敏
         */
        private boolean enabled = true;

        /**
         * 敏感字段名模式
         */
        private Set<String> sensitiveFields = Set.of(
                "password",
                "pwd",
                "secret",
                "token",
                "apiKey",
                "api_key",
                "creditCard",
                "credit_card",
                "idCard",
                "id_card",
                "phone",
                "mobile",
                "email",
                "address");

        /**
         * 脱敏字符
         */
        private char maskChar = '*';

        /**
         * 保留前几位
         */
        private int keepPrefix = 3;

        /**
         * 保留后几位
         */
        private int keepSuffix = 4;
    }

    /**
     * 安全头配置
     */
    @Data
    public static class SecurityHeaderConfig {
        /**
         * Content-Security-Policy
         */
        private String contentSecurityPolicy = "default-src 'self'";

        /**
         * Strict-Transport-Security
         */
        private String strictTransportSecurity = "max-age=31536000; includeSubDomains";

        /**
         * X-Frame-Options
         */
        private String frameOptions = "DENY";

        /**
         * X-Content-Type-Options
         */
        private String contentTypeOptions = "nosniff";

        /**
         * X-XSS-Protection
         */
        private String xssProtection = "1; mode=block";
    }
}
