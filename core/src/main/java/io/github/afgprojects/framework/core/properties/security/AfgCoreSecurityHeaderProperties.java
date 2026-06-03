package io.github.afgprojects.framework.core.properties.security;

import lombok.Data;

/**
 * 安全头配置。
 */
@Data
public class AfgCoreSecurityHeaderProperties {

    private String contentSecurityPolicy = "default-src 'self'";
    private String strictTransportSecurity = "max-age=31536000; includeSubDomains";
    private String frameOptions = "DENY";
    private String contentTypeOptions = "nosniff";
    private String xssProtection = "1; mode=block";
}
