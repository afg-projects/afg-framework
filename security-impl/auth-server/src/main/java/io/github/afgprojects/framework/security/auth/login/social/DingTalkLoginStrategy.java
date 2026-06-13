package io.github.afgprojects.framework.security.auth.login.social;

import io.github.afgprojects.framework.commons.exception.BusinessException;
import io.github.afgprojects.framework.commons.exception.CommonErrorCode;
import io.github.afgprojects.framework.security.auth.properties.social.DingTalkConfig;
import io.github.afgprojects.framework.security.core.authentication.AfgUserDetailsService;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.web.client.RestClient;

/**
 * 钉钉扫码登录策略。
 *
 * <p>钉钉 OAuth2 授权码模式流程：
 * <ol>
 *   <li>前端跳转到钉钉扫码登录页面</li>
 *   <li>用户扫码授权后回调，携带 authCode</li>
 *   <li>后端用 authCode 换取用户的 access_token（/sns/gettoken + /sns/getuserinfo_bycode）</li>
 *   <li>映射到系统用户</li>
 * </ol>
 *
 * <p>配置项：
 * <ul>
 *   <li>afg.security.auth-server.social.dingtalk.app-key - 钉钉 AppKey</li>
 *   <li>afg.security.auth-server.social.dingtalk.app-secret - 钉钉 AppSecret</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Slf4j
public class DingTalkLoginStrategy extends AbstractSocialLoginStrategy {

    private static final String GET_TOKEN_URL = "https://oapi.dingtalk.com/gettoken";
    private static final String GET_USER_INFO_URL = "https://oapi.dingtalk.com/sns/getuserinfo_bycode";

    private final DingTalkConfig config;

    /**
     * 构造函数。
     *
     * @param userDetailsService 用户详情服务
     * @param restClient         REST 客户端
     * @param config             钉钉配置
     */
    public DingTalkLoginStrategy(
            @NonNull AfgUserDetailsService userDetailsService,
            @NonNull RestClient restClient,
            @NonNull DingTalkConfig config) {
        super(userDetailsService, restClient);
        this.config = config;
    }

    @Override
    public String getLoginType() {
        return "DINGTALK";
    }

    @Override
    protected String getStrategyName() {
        return "钉钉";
    }

    @Override
    protected SocialTokenResponse exchangeToken(String code, String redirectUri) {
        try {
            // 1. 获取企业 access_token
            String tokenUrl = GET_TOKEN_URL
                    + "?appkey=" + config.getAppKey()
                    + "&appsecret=" + config.getAppSecret();

            String tokenResponse = restClient.get()
                    .uri(tokenUrl)
                    .retrieve()
                    .body(String.class);

            log.debug("DingTalk gettoken response received");

            String accessToken = extractJsonField(tokenResponse, "access_token");
            if (accessToken == null) {
                String errcode = extractJsonField(tokenResponse, "errcode");
                String errmsg = extractJsonField(tokenResponse, "errmsg");
                log.error("DingTalk gettoken failed: errcode={}, errmsg={}", errcode, errmsg);
                throw new BusinessException(CommonErrorCode.UNAUTHORIZED,
                        "钉钉授权失败: " + errmsg);
            }

            // 2. 用 access_token + authCode 获取用户信息
            String userInfoUrl = GET_USER_INFO_URL
                    + "?access_token=" + accessToken
                    + "&authCode=" + code;

            String userInfoResponse = restClient.get()
                    .uri(userInfoUrl)
                    .retrieve()
                    .body(String.class);

            log.debug("DingTalk getuserinfo_bycode response received");

            // 钉钉返回的是 user_info 结构，包含 openid
            String openid = extractJsonField(userInfoResponse, "openid");
            if (openid == null) {
                // 尝试从嵌套结构中获取
                String unionid = extractJsonField(userInfoResponse, "unionid");
                openid = unionid;
            }

            SocialTokenResponse response = new SocialTokenResponse();
            response.setAccessToken(accessToken);
            response.setOpenId(openid);
            return response;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("DingTalk token exchange error", e);
            throw new BusinessException(CommonErrorCode.CLIENT_REQUEST_FAILED, "钉钉授权请求失败", e);
        }
    }

    @Override
    protected SocialUserInfo getUserInfo(SocialTokenResponse tokenResponse) {
        // 钉钉的 getuserinfo_bycode 接口已返回用户信息
        // 这里直接构建 SocialUserInfo
        return SocialUserInfo.builder()
                .openId(tokenResponse.getOpenId())
                .source("dingtalk")
                .build();
    }
}
