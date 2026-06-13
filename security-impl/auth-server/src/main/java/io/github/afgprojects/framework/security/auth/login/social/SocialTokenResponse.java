package io.github.afgprojects.framework.security.auth.login.social;

import lombok.Data;

/**
 * 社交登录 Token 响应。
 *
 * <p>各社交平台换取 access_token 后的统一响应模型，包含 access_token 和 openId。
 * 不同平台在 token 交换时同时返回 openId，需要统一缓存。
 *
 * @since 1.0.0
 */
@Data
public class SocialTokenResponse {

    /**
     * 访问令牌。
     */
    private String accessToken;

    /**
     * 第三方用户唯一标识（openid）。
     */
    private String openId;

    /**
     * 联合 ID（微信开放平台 unionid，其他平台可为 null）。
     */
    private String unionId;

    /**
     * Token 有效期（秒）。
     */
    private Integer expiresIn;

    /**
     * 刷新令牌。
     */
    private String refreshToken;
}
