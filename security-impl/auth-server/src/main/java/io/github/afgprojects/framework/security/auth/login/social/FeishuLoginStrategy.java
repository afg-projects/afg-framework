package io.github.afgprojects.framework.security.auth.login.social;

import io.github.afgprojects.framework.commons.exception.BusinessException;
import io.github.afgprojects.framework.commons.exception.CommonErrorCode;
import io.github.afgprojects.framework.security.auth.properties.social.FeishuConfig;
import io.github.afgprojects.framework.security.core.authentication.AfgUserDetailsService;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.web.client.RestClient;

/**
 * 飞书网页授权登录策略。
 *
 * <p>飞书 OAuth2 授权码模式流程：
 * <ol>
 *   <li>前端跳转到飞书授权页面</li>
 *   <li>用户授权后回调，携带 code</li>
 *   <li>后端用 app_access_token + code 换取用户 access_token（/open-apis/authen/v1/oidc/access_token）</li>
 *   <li>用用户 access_token 获取用户信息（/open-apis/authen/v1/user_info）</li>
 *   <li>映射到系统用户</li>
 * </ol>
 *
 * <p>配置项：
 * <ul>
 *   <li>afg.security.auth-server.social.feishu.app-id - 飞书 App ID</li>
 *   <li>afg.security.auth-server.social.feishu.app-secret - 飞书 App Secret</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Slf4j
public class FeishuLoginStrategy extends AbstractSocialLoginStrategy {

    private static final String APP_ACCESS_TOKEN_URL = "https://open.feishu.cn/open-apis/auth/v3/app_access_token/internal";
    private static final String USER_ACCESS_TOKEN_URL = "https://open.feishu.cn/open-apis/authen/v1/oidc/access_token";
    private static final String USER_INFO_URL = "https://open.feishu.cn/open-apis/authen/v1/user_info";

    private final FeishuConfig config;

    /**
     * 构造函数。
     *
     * @param userDetailsService 用户详情服务
     * @param restClient         REST 客户端
     * @param config             飞书配置
     */
    public FeishuLoginStrategy(
            @NonNull AfgUserDetailsService userDetailsService,
            @NonNull RestClient restClient,
            @NonNull FeishuConfig config) {
        super(userDetailsService, restClient);
        this.config = config;
    }

    @Override
    public String getLoginType() {
        return "FEISHU";
    }

    @Override
    protected String getStrategyName() {
        return "飞书";
    }

    @Override
    protected SocialTokenResponse exchangeToken(String code, String redirectUri) {
        try {
            // 1. 获取 app_access_token
            String appTokenRequestBody = "{\"app_id\":\"" + config.getAppId()
                    + "\",\"app_secret\":\"" + config.getAppSecret() + "\"}";

            String appTokenResponse = restClient.post()
                    .uri(APP_ACCESS_TOKEN_URL)
                    .header("Content-Type", "application/json")
                    .body(appTokenRequestBody)
                    .retrieve()
                    .body(String.class);

            log.debug("Feishu app_access_token response received");

            String appAccessToken = extractJsonField(appTokenResponse, "app_access_token");
            if (appAccessToken == null) {
                log.error("Feishu app_access_token failed: code={}", extractJsonField(appTokenResponse, "code"));
                throw new BusinessException(CommonErrorCode.UNAUTHORIZED, "飞书获取 app_access_token 失败");
            }

            // 2. 用 app_access_token + code 换取用户 access_token
            String userTokenRequestBody = "{\"grant_type\":\"authorization_code\",\"code\":\"" + code + "\"}";

            String userTokenResponse = restClient.post()
                    .uri(USER_ACCESS_TOKEN_URL)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + appAccessToken)
                    .body(userTokenRequestBody)
                    .retrieve()
                    .body(String.class);

            log.debug("Feishu user access_token response received");

            String userAccessToken = extractJsonField(userTokenResponse, "access_token");
            if (userAccessToken == null) {
                log.error("Feishu user access_token failed: code={}", extractJsonField(userTokenResponse, "code"));
                throw new BusinessException(CommonErrorCode.UNAUTHORIZED, "飞书获取用户 access_token 失败");
            }

            SocialTokenResponse response = new SocialTokenResponse();
            response.setAccessToken(userAccessToken);
            return response;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Feishu token exchange error", e);
            throw new BusinessException(CommonErrorCode.CLIENT_REQUEST_FAILED, "飞书授权请求失败", e);
        }
    }

    @Override
    protected SocialUserInfo getUserInfo(SocialTokenResponse tokenResponse) {
        try {
            String response = restClient.get()
                    .uri(USER_INFO_URL)
                    .header("Authorization", "Bearer " + tokenResponse.getAccessToken())
                    .retrieve()
                    .body(String.class);

            log.debug("Feishu user info response received");

            String openId = extractJsonField(response, "open_id");
            String name = extractJsonField(response, "name");
            String avatarUrl = extractJsonField(response, "avatar_url");
            String email = extractJsonField(response, "email");
            String mobile = extractJsonField(response, "mobile");

            return SocialUserInfo.builder()
                    .openId(openId)
                    .nickname(name)
                    .avatar(avatarUrl)
                    .email(email)
                    .phone(mobile)
                    .source("feishu")
                    .build();
        } catch (Exception e) {
            log.error("Feishu get user info error", e);
            throw new BusinessException(CommonErrorCode.CLIENT_REQUEST_FAILED, "获取飞书用户信息失败", e);
        }
    }
}
