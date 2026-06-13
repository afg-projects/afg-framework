package io.github.afgprojects.framework.security.auth.login.social;

import io.github.afgprojects.framework.commons.exception.BusinessException;
import io.github.afgprojects.framework.commons.exception.CommonErrorCode;
import io.github.afgprojects.framework.security.auth.properties.social.WeComConfig;
import io.github.afgprojects.framework.security.core.authentication.AfgUserDetailsService;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.web.client.RestClient;

/**
 * 企业微信网页授权登录策略。
 *
 * <p>企业微信 OAuth2 授权码模式流程：
 * <ol>
 *   <li>前端跳转到企业微信授权页面</li>
 *   <li>用户授权后回调，携带 code</li>
 *   <li>后端用 corpsecret 获取 access_token（/cgi-bin/gettoken）</li>
 *   <li>用 access_token + code 获取用户身份信息（/cgi-bin/auth/getuserinfo）</li>
 *   <li>映射到系统用户</li>
 * </ol>
 *
 * <p>配置项：
 * <ul>
 *   <li>afg.security.auth-server.social.wecom.corp-id - 企业 ID</li>
 *   <li>afg.security.auth-server.social.wecom.agent-id - 应用 AgentId</li>
 *   <li>afg.security.auth-server.social.wecom.secret - 应用 Secret</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Slf4j
public class WeComLoginStrategy extends AbstractSocialLoginStrategy {

    private static final String GET_TOKEN_URL = "https://qyapi.weixin.qq.com/cgi-bin/gettoken";
    private static final String GET_USER_INFO_URL = "https://qyapi.weixin.qq.com/cgi-bin/auth/getuserinfo";

    private final WeComConfig config;

    /**
     * 构造函数。
     *
     * @param userDetailsService 用户详情服务
     * @param restClient         REST 客户端
     * @param config             企业微信配置
     */
    public WeComLoginStrategy(
            @NonNull AfgUserDetailsService userDetailsService,
            @NonNull RestClient restClient,
            @NonNull WeComConfig config) {
        super(userDetailsService, restClient);
        this.config = config;
    }

    @Override
    public String getLoginType() {
        return "WECOM";
    }

    @Override
    protected String getStrategyName() {
        return "企业微信";
    }

    @Override
    protected SocialTokenResponse exchangeToken(String code, String redirectUri) {
        try {
            // 1. 获取企业 access_token
            String tokenUrl = GET_TOKEN_URL
                    + "?corpid=" + config.getCorpId()
                    + "&corpsecret=" + config.getSecret();

            String tokenResponse = restClient.get()
                    .uri(tokenUrl)
                    .retrieve()
                    .body(String.class);

            log.debug("WeCom gettoken response received");

            String accessToken = extractJsonField(tokenResponse, "access_token");
            if (accessToken == null) {
                String errcode = extractJsonField(tokenResponse, "errcode");
                String errmsg = extractJsonField(tokenResponse, "errmsg");
                log.error("WeCom gettoken failed: errcode={}, errmsg={}", errcode, errmsg);
                throw new BusinessException(CommonErrorCode.UNAUTHORIZED,
                        "企业微信获取 access_token 失败: " + errmsg);
            }

            // 2. 用 access_token + code 获取用户身份信息
            String userInfoUrl = GET_USER_INFO_URL
                    + "?access_token=" + accessToken
                    + "&code=" + code;

            String userInfoResponse = restClient.get()
                    .uri(userInfoUrl)
                    .retrieve()
                    .body(String.class);

            log.debug("WeCom getuserinfo response received");

            String userid = extractJsonField(userInfoResponse, "userid");
            String openid = extractJsonField(userInfoResponse, "openid");

            if (userid == null && openid == null) {
                String errcode = extractJsonField(userInfoResponse, "errcode");
                String errmsg = extractJsonField(userInfoResponse, "errmsg");
                log.error("WeCom getuserinfo failed: errcode={}, errmsg={}", errcode, errmsg);
                throw new BusinessException(CommonErrorCode.UNAUTHORIZED,
                        "企业微信获取用户信息失败: " + errmsg);
            }

            SocialTokenResponse response = new SocialTokenResponse();
            response.setAccessToken(accessToken);
            // 企业微信优先使用 userid（企业内部用户），降级使用 openid（外部联系人）
            response.setOpenId(userid != null ? userid : openid);
            return response;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("WeCom token exchange error", e);
            throw new BusinessException(CommonErrorCode.CLIENT_REQUEST_FAILED, "企业微信授权请求失败", e);
        }
    }

    @Override
    protected SocialUserInfo getUserInfo(SocialTokenResponse tokenResponse) {
        // 企业微信的 getuserinfo 接口已返回用户标识（userid 或 openid）
        // 这里直接构建 SocialUserInfo，详细用户信息可由业务系统按需获取
        return SocialUserInfo.builder()
                .openId(tokenResponse.getOpenId())
                .source("wecom")
                .build();
    }
}
