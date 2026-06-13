package io.github.afgprojects.framework.security.auth.properties.social;

import lombok.Data;

/**
 * 微信登录配置。
 *
 * @since 1.0.0
 */
@Data
public class WechatConfig {

    /**
     * 是否启用微信登录。
     * 默认关闭。
     */
    private boolean enabled = false;

    /**
     * 微信 AppID。
     */
    private String appId;

    /**
     * 微信 AppSecret。
     */
    private String appSecret;
}
