package io.github.afgprojects.framework.security.auth.properties.login;

import java.time.Duration;

import lombok.Data;

/**
 * 登录配置。
 */
@Data
public class LoginConfig {

    /**
     * 是否启用登录服务。
     * 默认启用。
     */
    private boolean enabled = true;

    /**
     * 验证码有效期。
     * 默认 5 分钟。
     */
    private Duration captchaTtl = Duration.ofMinutes(5);

    /**
     * 验证码长度。
     * 默认 4 位。
     */
    private int captchaLength = 4;
}
