package io.github.afgprojects.framework.core.properties.security;

import java.util.Set;

import lombok.Data;

/**
 * 敏感数据脱敏配置。
 */
@Data
public class AfgCoreSensitiveDataProperties {

    private boolean enabled = true;
    private Set<String> sensitiveFields = Set.of(
            "password", "pwd", "secret", "token", "apiKey", "api_key",
            "creditCard", "credit_card", "idCard", "id_card", "phone", "mobile", "email", "address");
    private char maskChar = '*';
    private int keepPrefix = 3;
    private int keepSuffix = 4;
}
