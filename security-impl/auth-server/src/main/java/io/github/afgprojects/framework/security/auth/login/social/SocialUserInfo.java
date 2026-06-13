package io.github.afgprojects.framework.security.auth.login.social;

import lombok.Builder;
import lombok.Data;

/**
 * 社交登录用户信息。
 *
 * <p>从第三方平台获取的用户信息统一映射模型，各平台策略实现负责将平台特定的用户信息转换为此结构。
 *
 * @since 1.0.0
 */
@Data
@Builder
public class SocialUserInfo {

    /**
     * 第三方用户唯一标识（openid）。
     */
    private String openId;

    /**
     * 联合 ID（微信开放平台 unionid，其他平台可为 null）。
     */
    private String unionId;

    /**
     * 用户昵称。
     */
    private String nickname;

    /**
     * 用户头像 URL。
     */
    private String avatar;

    /**
     * 用户邮箱（部分平台提供）。
     */
    private String email;

    /**
     * 用户手机号（部分平台提供）。
     */
    private String phone;

    /**
     * 来源平台标识。
     *
     * <p>可选值：wechat / dingtalk / feishu / wecom
     */
    private String source;
}
