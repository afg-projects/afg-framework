package io.github.afgprojects.framework.security.auth.properties.social;

import lombok.Data;

/**
 * 企业微信登录配置。
 *
 * @since 1.0.0
 */
@Data
public class WeComConfig {

    /**
     * 是否启用企业微信登录。
     * 默认关闭。
     */
    private boolean enabled = false;

    /**
     * 企业 ID（corpid）。
     */
    private String corpId;

    /**
     * 应用 AgentId。
     */
    private String agentId;

    /**
     * 应用 Secret。
     */
    private String secret;
}
