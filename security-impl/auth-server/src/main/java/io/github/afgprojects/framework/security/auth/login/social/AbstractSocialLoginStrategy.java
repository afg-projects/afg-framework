package io.github.afgprojects.framework.security.auth.login.social;

import io.github.afgprojects.framework.commons.exception.BusinessException;
import io.github.afgprojects.framework.commons.exception.CommonErrorCode;
import io.github.afgprojects.framework.security.core.authentication.AfgUserDetails;
import io.github.afgprojects.framework.security.core.authentication.AfgUserDetailsService;
import io.github.afgprojects.framework.security.core.login.model.LoginRequest;
import io.github.afgprojects.framework.security.core.login.strategy.LoginStrategy;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.web.client.RestClient;

/**
 * 社交登录策略抽象基类。
 *
 * <p>使用模板方法模式统一社交登录流程：
 * <ol>
 *   <li>从 LoginRequest.extra() 提取授权码（JSON 格式：{"code":"xxx","redirectUri":"xxx"}）</li>
 *   <li>用授权码换取 access_token 和 openId（{@link #exchangeToken}）</li>
 *   <li>用 access_token 获取第三方用户信息（{@link #getUserInfo}）</li>
 *   <li>映射到系统用户（{@link #mapToSystemUser}）</li>
 *   <li>返回 AfgUserDetails</li>
 * </ol>
 *
 * <p>子类只需实现平台差异部分：{@link #exchangeToken}、{@link #getUserInfo}、{@link #getStrategyName}。
 *
 * @since 1.0.0
 */
@Slf4j
public abstract class AbstractSocialLoginStrategy implements LoginStrategy {

    protected final AfgUserDetailsService userDetailsService;
    protected final RestClient restClient;

    /**
     * 构造函数。
     *
     * @param userDetailsService 用户详情服务
     * @param restClient REST 客户端（用于调用第三方 API）
     */
    protected AbstractSocialLoginStrategy(
            @NonNull AfgUserDetailsService userDetailsService,
            @NonNull RestClient restClient) {
        this.userDetailsService = userDetailsService;
        this.restClient = restClient;
    }

    @Override
    public AfgUserDetails authenticate(LoginRequest request) {
        String code = extractCode(request);
        String redirectUri = extractRedirectUri(request);

        log.debug("Social login attempt: strategy={}, code={}", getStrategyName(), code);

        // 1. 用授权码换取 access_token 和 openId
        SocialTokenResponse tokenResponse = exchangeToken(code, redirectUri);
        if (tokenResponse == null || tokenResponse.getAccessToken() == null || tokenResponse.getAccessToken().isEmpty()) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED,
                    getStrategyName() + " 授权失败：无法获取 access_token");
        }

        // 2. 用 access_token 获取第三方用户信息
        SocialUserInfo userInfo = getUserInfo(tokenResponse);
        if (userInfo == null || userInfo.getOpenId() == null) {
            // 如果 getUserInfo 未返回 openId，从 tokenResponse 中取
            if (tokenResponse.getOpenId() != null && userInfo != null) {
                userInfo.setOpenId(tokenResponse.getOpenId());
            } else {
                throw new BusinessException(CommonErrorCode.UNAUTHORIZED,
                        getStrategyName() + " 授权失败：无法获取用户信息");
            }
        }

        log.info("Social login user fetched: strategy={}, openId={}", getStrategyName(), userInfo.getOpenId());

        // 3. 映射到系统用户
        return mapToSystemUser(userInfo);
    }

    /**
     * 用授权码换取 access_token 和 openId。
     *
     * @param code        授权码
     * @param redirectUri 回调地址
     * @return Token 响应（包含 access_token 和 openId）
     */
    protected abstract SocialTokenResponse exchangeToken(String code, String redirectUri);

    /**
     * 用 access_token 获取第三方用户信息。
     *
     * @param tokenResponse Token 响应（包含 access_token 和 openId）
     * @return 第三方用户信息
     */
    protected abstract SocialUserInfo getUserInfo(SocialTokenResponse tokenResponse);

    /**
     * 获取策略名称（用于日志和错误消息）。
     *
     * @return 策略名称，如 "微信"、"钉钉"
     */
    protected abstract String getStrategyName();

    /**
     * 映射第三方用户信息到系统用户。
     *
     * <p>默认实现通过 openId 查找系统用户。业务系统可通过
     * {@link AfgUserDetailsService#loadUserBySocialOpenId(String, String)} 自定义映射逻辑。
     *
     * @param userInfo 第三方用户信息
     * @return 系统用户详情
     */
    protected AfgUserDetails mapToSystemUser(SocialUserInfo userInfo) {
        try {
            return userDetailsService.loadUserBySocialOpenId(userInfo.getOpenId(), userInfo.getSource());
        } catch (Exception e) {
            log.warn("Failed to map social user to system user: source={}, openId={}",
                    userInfo.getSource(), userInfo.getOpenId(), e);
            throw new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND,
                    getStrategyName() + " 用户未绑定系统账号", e);
        }
    }

    /**
     * 从 LoginRequest.extra() 提取授权码。
     *
     * <p>extra 字段格式为 JSON：{"code":"xxx","redirectUri":"xxx"}
     *
     * @param request 登录请求
     * @return 授权码
     */
    protected String extractCode(LoginRequest request) {
        String extra = request.extra();
        if (extra == null || extra.isEmpty()) {
            throw new BusinessException(CommonErrorCode.PARAM_MISSING, "授权码不能为空");
        }
        String code = extractJsonField(extra, "code");
        if (code == null || code.isEmpty()) {
            throw new BusinessException(CommonErrorCode.PARAM_MISSING, "授权码不能为空");
        }
        return code;
    }

    /**
     * 从 LoginRequest.extra() 提取回调地址。
     *
     * @param request 登录请求
     * @return 回调地址，可能为 null
     */
    protected String extractRedirectUri(LoginRequest request) {
        String extra = request.extra();
        if (extra == null || extra.isEmpty()) {
            return null;
        }
        return extractJsonField(extra, "redirectUri");
    }

    /**
     * 简单 JSON 字段值提取（避免引入 JSON 库依赖）。
     *
     * @param json  JSON 字符串
     * @param field 字段名
     * @return 字段值，未找到返回 null
     */
    protected String extractJsonField(String json, String field) {
        String key = "\"" + field + "\"";
        int keyIndex = json.indexOf(key);
        if (keyIndex < 0) {
            return null;
        }
        int colonIndex = json.indexOf(':', keyIndex + key.length());
        if (colonIndex < 0) {
            return null;
        }
        // 查找值的起始和结束引号
        int valueStart = json.indexOf('"', colonIndex + 1);
        if (valueStart < 0) {
            return null;
        }
        int valueEnd = json.indexOf('"', valueStart + 1);
        if (valueEnd < 0) {
            return null;
        }
        return json.substring(valueStart + 1, valueEnd);
    }
}
