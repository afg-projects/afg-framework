package io.github.afgprojects.framework.security.auth.properties.social;

import lombok.Data;

/**
 * 钉钉登录配置。
 *
 * @since 1.0.0
 */
@Data
public class DingTalkConfig {

    /**
     * 是否启用钉钉登录。
     * 默认关闭。
     */
    private boolean enabled = false;

    /**
     * 钉钉 AppKey。
     */
    private String appKey;

    /**
     * 钉钉 AppSecret。
     */
    private String appSecret;
}
