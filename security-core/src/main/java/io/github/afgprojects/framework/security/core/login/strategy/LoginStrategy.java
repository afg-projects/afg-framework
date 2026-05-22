package io.github.afgprojects.framework.security.core.login.strategy;

import io.github.afgprojects.framework.security.core.authentication.AfgUserDetails;
import io.github.afgprojects.framework.security.core.login.model.LoginRequest;

/**
 * 登录策略接口。
 *
 * <p>定义登录认证的策略模式，支持通过实现类扩展不同的登录方式。
 *
 * <p>内置策略：
 * <ul>
 *   <li>UsernamePasswordLoginStrategy - 用户名密码登录</li>
 *   <li>MobileCaptchaLoginStrategy - 手机号验证码登录</li>
 *   <li>EmailCaptchaLoginStrategy - 邮箱验证码登录</li>
 * </ul>
 *
 * <p>自定义策略示例：
 * <pre>{@code
 * @Component
 * public class WechatLoginStrategy implements LoginStrategy {
 *     @Override
 *     public String getLoginType() {
 *         return "WECHAT";
 *     }
 *
 *     @Override
 *     public AfgUserDetails authenticate(LoginRequest request) {
 *         // 微信登录逻辑
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public interface LoginStrategy {

    /**
     * 获取登录类型标识。
     *
     * <p>用于匹配登录请求中的 loginType。
     *
     * @return 登录类型标识，如 "USERNAME", "MOBILE", "EMAIL"
     */
    String getLoginType();

    /**
     * 执行登录认证。
     *
     * @param request 登录请求
     * @return 用户详情，认证成功时返回
     */
    AfgUserDetails authenticate(LoginRequest request);

    /**
     * 判断是否支持该登录请求。
     *
     * <p>默认实现基于 loginType 匹配，子类可覆盖实现更复杂的匹配逻辑。
     *
     * @param request 登录请求
     * @return 是否支持
     */
    default boolean supports(LoginRequest request) {
        return getLoginType().equals(request.loginType().name());
    }
}
