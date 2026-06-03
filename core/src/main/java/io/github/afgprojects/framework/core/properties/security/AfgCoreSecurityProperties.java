package io.github.afgprojects.framework.core.properties.security;

import lombok.Data;

/**
 * 安全配置。
 */
@Data
public class AfgCoreSecurityProperties {

    /**
     * XSS 防护配置。
     */
    private AfgCoreXssProperties xss = new AfgCoreXssProperties();

    /**
     * SQL 注入防护配置。
     */
    private AfgCoreSqlInjectionProperties sqlInjection = new AfgCoreSqlInjectionProperties();

    /**
     * 敏感数据脱敏配置。
     */
    private AfgCoreSensitiveDataProperties sensitiveData = new AfgCoreSensitiveDataProperties();

    /**
     * 安全头配置。
     */
    private AfgCoreSecurityHeaderProperties securityHeader = new AfgCoreSecurityHeaderProperties();

    /**
     * 签名验证配置。
     */
    private AfgCoreSignatureProperties signature = new AfgCoreSignatureProperties();
}
