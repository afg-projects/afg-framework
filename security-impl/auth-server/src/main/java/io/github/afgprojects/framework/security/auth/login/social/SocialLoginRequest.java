package io.github.afgprojects.framework.security.auth.login.social;

import lombok.Data;

/**
 * 社交登录请求。
 *
 * <p>用于第三方 OAuth2 授权码模式登录，前端在用户授权后携带授权码请求后端完成登录。
 *
 * @since 1.0.0
 */
@Data
public class SocialLoginRequest {

    /**
     * 第三方授权码。
     */
    private String code;

    /**
     * 状态参数（防 CSRF）。
     */
    private String state;

    /**
     * 回调地址。
     */
    private String redirectUri;
}
