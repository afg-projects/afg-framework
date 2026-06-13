package io.github.afgprojects.framework.security.auth.properties.social;

import lombok.Data;

/**
 * 社交登录配置。
 *
 * <p>配置示例：
 * <pre>
 * afg:
 *   security:
 *     auth-server:
 *       social:
 *         enabled: true
 *         wechat:
 *           app-id: wx1234567890
 *           app-secret: my-wechat-secret
 *         dingtalk:
 *           app-key: ding1234567890
 *           app-secret: my-dingtalk-secret
 *         feishu:
 *           app-id: cli_1234567890
 *           app-secret: my-feishu-secret
 *         wecom:
 *           corp-id: ww1234567890
 *           agent-id: 1000001
 *           secret: my-wecom-secret
 * </pre>
 *
 * @since 1.0.0
 */
@Data
public class SocialConfig {

    /**
     * 是否启用社交登录。
     * 默认关闭，需要配置第三方平台凭据后手动启用。
     */
    private boolean enabled = false;

    /**
     * 微信登录配置。
     */
    private WechatConfig wechat = new WechatConfig();

    /**
     * 钉钉登录配置。
     */
    private DingTalkConfig dingtalk = new DingTalkConfig();

    /**
     * 飞书登录配置。
     */
    private FeishuConfig feishu = new FeishuConfig();

    /**
     * 企业微信登录配置。
     */
    private WeComConfig wecom = new WeComConfig();
}
