package io.github.afgprojects.framework.security.auth.properties.social;

import lombok.Data;

/**
 * 飞书登录配置。
 *
 * @since 1.0.0
 */
@Data
public class FeishuConfig {

    /**
     * 是否启用飞书登录。
     * 默认关闭。
     */
    private boolean enabled = false;

    /**
     * 飞书 App ID。
     */
    private String appId;

    /**
     * 飞书 App Secret。
     */
    private String appSecret;
}
