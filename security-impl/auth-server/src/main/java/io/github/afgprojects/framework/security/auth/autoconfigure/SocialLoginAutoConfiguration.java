package io.github.afgprojects.framework.security.auth.autoconfigure;

import io.github.afgprojects.framework.security.auth.login.social.DingTalkLoginStrategy;
import io.github.afgprojects.framework.security.auth.login.social.FeishuLoginStrategy;
import io.github.afgprojects.framework.security.auth.login.social.WechatLoginStrategy;
import io.github.afgprojects.framework.security.auth.login.social.WeComLoginStrategy;
import io.github.afgprojects.framework.security.auth.properties.AuthSecurityProperties;
import io.github.afgprojects.framework.security.auth.properties.social.DingTalkConfig;
import io.github.afgprojects.framework.security.auth.properties.social.FeishuConfig;
import io.github.afgprojects.framework.security.auth.properties.social.WechatConfig;
import io.github.afgprojects.framework.security.auth.properties.social.WeComConfig;
import io.github.afgprojects.framework.security.core.authentication.AfgUserDetailsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

/**
 * 社交登录自动配置。
 *
 * <p>根据配置条件注册社交登录策略 Bean：
 * <ul>
 *   <li>微信登录策略 — 当 afg.security.auth-server.social.wechat.enabled=true 时注册</li>
 *   <li>钉钉登录策略 — 当 afg.security.auth-server.social.dingtalk.enabled=true 时注册</li>
 *   <li>飞书登录策略 — 当 afg.security.auth-server.social.feishu.enabled=true 时注册</li>
 *   <li>企业微信登录策略 — 当 afg.security.auth-server.social.wecom.enabled=true 时注册</li>
 * </ul>
 *
 * <p>所有社交登录策略默认关闭，需要配置第三方平台凭据后手动启用。
 *
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration(after = LoginAutoConfiguration.class)
@EnableConfigurationProperties(AuthSecurityProperties.class)
@ConditionalOnProperty(prefix = "afg.security.auth-server.social", name = "enabled", havingValue = "true")
public class SocialLoginAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RestClient socialRestClient() {
        log.info("Initializing RestClient for social login");
        return RestClient.builder().build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "afg.security.auth-server.social.wechat", name = "enabled", havingValue = "true")
    public WechatLoginStrategy wechatLoginStrategy(
            AfgUserDetailsService userDetailsService,
            RestClient socialRestClient,
            AuthSecurityProperties properties) {
        WechatConfig config = properties.getSocial().getWechat();
        log.info("Initializing WechatLoginStrategy with appId={}", config.getAppId());
        return new WechatLoginStrategy(userDetailsService, socialRestClient, config);
    }

    @Bean
    @ConditionalOnProperty(prefix = "afg.security.auth-server.social.dingtalk", name = "enabled", havingValue = "true")
    public DingTalkLoginStrategy dingtalkLoginStrategy(
            AfgUserDetailsService userDetailsService,
            RestClient socialRestClient,
            AuthSecurityProperties properties) {
        DingTalkConfig config = properties.getSocial().getDingtalk();
        log.info("Initializing DingTalkLoginStrategy with appKey={}", config.getAppKey());
        return new DingTalkLoginStrategy(userDetailsService, socialRestClient, config);
    }

    @Bean
    @ConditionalOnProperty(prefix = "afg.security.auth-server.social.feishu", name = "enabled", havingValue = "true")
    public FeishuLoginStrategy feishuLoginStrategy(
            AfgUserDetailsService userDetailsService,
            RestClient socialRestClient,
            AuthSecurityProperties properties) {
        FeishuConfig config = properties.getSocial().getFeishu();
        log.info("Initializing FeishuLoginStrategy with appId={}", config.getAppId());
        return new FeishuLoginStrategy(userDetailsService, socialRestClient, config);
    }

    @Bean
    @ConditionalOnProperty(prefix = "afg.security.auth-server.social.wecom", name = "enabled", havingValue = "true")
    public WeComLoginStrategy wecomLoginStrategy(
            AfgUserDetailsService userDetailsService,
            RestClient socialRestClient,
            AuthSecurityProperties properties) {
        WeComConfig config = properties.getSocial().getWecom();
        log.info("Initializing WeComLoginStrategy with corpId={}", config.getCorpId());
        return new WeComLoginStrategy(userDetailsService, socialRestClient, config);
    }
}
