package io.github.afgprojects.framework.security.auth.login.social;

import io.github.afgprojects.framework.commons.exception.BusinessException;
import io.github.afgprojects.framework.commons.exception.CommonErrorCode;
import io.github.afgprojects.framework.security.auth.properties.social.WechatConfig;
import io.github.afgprojects.framework.security.core.authentication.AfgUserDetailsService;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.web.client.RestClient;

/**
 * 微信网页授权登录策略。
 *
 * <p>微信 OAuth2 授权码模式流程：
 * <ol>
 *   <li>前端跳转到微信授权页面（构造授权 URL 由前端完成）</li>
 *   <li>用户授权后回调，携带 code</li>
 *   <li>后端用 code 换取 access_token 和 openid（/sns/oauth2/access_token）</li>
 *   <li>用 access_token + openid 获取用户信息（/sns/userinfo）</li>
 *   <li>映射到系统用户</li>
 * </ol>
 *
 * <p>配置项：
 * <ul>
 *   <li>afg.security.auth-server.social.wechat.app-id - 微信 AppID</li>
 *   <li>afg.security.auth-server.social.wechat.app-secret - 微信 AppSecret</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Slf4j
public class WechatLoginStrategy extends AbstractSocialLoginStrategy {

    private static final String TOKEN_URL = "https://api.weixin.qq.com/sns/oauth2/access_token";
    private static final String USER_INFO_URL = "https://api.weixin.qq.com/sns/userinfo";

    private final WechatConfig config;

    /**
     * 构造函数。
     *
     * @param userDetailsService 用户详情服务
     * @param restClient         REST 客户端
     * @param config             微信配置
     */
    public WechatLoginStrategy(
            @NonNull AfgUserDetailsService userDetailsService,
            @NonNull RestClient restClient,
            @NonNull WechatConfig config) {
        super(userDetailsService, restClient);
        this.config = config;
    }

    @Override
    public String getLoginType() {
        return "WECHAT";
    }

    @Override
    protected String getStrategyName() {
        return "微信";
    }

    @Override
    protected SocialTokenResponse exchangeToken(String code, String redirectUri) {
        try {
            String url = TOKEN_URL
                    + "?appid=" + config.getAppId()
                    + "&secret=" + config.getAppSecret()
                    + "&code=" + code
                    + "&grant_type=authorization_code";

            String response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);

            log.debug("Wechat token exchange response received");

            String accessToken = extractJsonField(response, "access_token");
            if (accessToken == null) {
                String errcode = extractJsonField(response, "errcode");
                String errmsg = extractJsonField(response, "errmsg");
                log.error("Wechat token exchange failed: errcode={}, errmsg={}", errcode, errmsg);
                throw new BusinessException(CommonErrorCode.UNAUTHORIZED,
                        "微信授权失败: " + errmsg);
            }

            SocialTokenResponse tokenResponse = new SocialTokenResponse();
            tokenResponse.setAccessToken(accessToken);
            tokenResponse.setOpenId(extractJsonField(response, "openid"));
            tokenResponse.setUnionId(extractJsonField(response, "unionid"));

            String expiresInStr = extractJsonField(response, "expires_in");
            if (expiresInStr != null) {
                tokenResponse.setExpiresIn(Integer.parseInt(expiresInStr));
            }

            return tokenResponse;
        } catch (BusinessException e) {
            throw e;
        } catch (NumberFormatException e) {
            log.warn("Failed to parse expires_in from wechat token response", e);
            return null;
        } catch (Exception e) {
            log.error("Wechat token exchange error", e);
            throw new BusinessException(CommonErrorCode.CLIENT_REQUEST_FAILED, "微信授权请求失败", e);
        }
    }

    @Override
    protected SocialUserInfo getUserInfo(SocialTokenResponse tokenResponse) {
        try {
            String url = USER_INFO_URL
                    + "?access_token=" + tokenResponse.getAccessToken()
                    + "&openid=" + tokenResponse.getOpenId();

            String response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);

            log.debug("Wechat user info response received");

            String openid = extractJsonField(response, "openid");
            String nickname = extractJsonField(response, "nickname");
            String headimgurl = extractJsonField(response, "headimgurl");
            String unionid = extractJsonField(response, "unionid");

            return SocialUserInfo.builder()
                    .openId(openid != null ? openid : tokenResponse.getOpenId())
                    .unionId(unionid != null ? unionid : tokenResponse.getUnionId())
                    .nickname(nickname)
                    .avatar(headimgurl)
                    .source("wechat")
                    .build();
        } catch (Exception e) {
            log.error("Wechat get user info error", e);
            throw new BusinessException(CommonErrorCode.CLIENT_REQUEST_FAILED, "获取微信用户信息失败", e);
        }
    }
}
