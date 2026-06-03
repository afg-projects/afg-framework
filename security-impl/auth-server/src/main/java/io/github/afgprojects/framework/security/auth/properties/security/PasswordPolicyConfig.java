package io.github.afgprojects.framework.security.auth.properties.security;

import lombok.Data;

/**
 * 密码策略配置。
 */
@Data
public class PasswordPolicyConfig {

    /**
     * 密码最小长度。
     */
    private int minLength = 8;

    /**
     * 是否需要大写字母。
     */
    private boolean requireUppercase = true;

    /**
     * 是否需要小写字母。
     */
    private boolean requireLowercase = true;

    /**
     * 是否需要数字。
     */
    private boolean requireDigit = true;

    /**
     * 是否需要特殊字符。
     */
    private boolean requireSpecialChar = true;

    /**
     * 转换为 PasswordPolicy 对象。
     *
     * @return PasswordPolicy 实例
     */
    public io.github.afgprojects.framework.security.auth.security.PasswordPolicy toPasswordPolicy() {
        return new io.github.afgprojects.framework.security.auth.security.PasswordPolicy(
                minLength,
                requireUppercase,
                requireLowercase,
                requireDigit,
                requireSpecialChar);
    }
}
