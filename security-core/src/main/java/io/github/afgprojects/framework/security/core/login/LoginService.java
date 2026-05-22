package io.github.afgprojects.framework.security.core.login;

import io.github.afgprojects.framework.security.core.login.model.CaptchaRequest;
import io.github.afgprojects.framework.security.core.login.model.CaptchaResponse;
import io.github.afgprojects.framework.security.core.login.model.LoginRequest;
import io.github.afgprojects.framework.security.core.login.model.LoginResponse;
import org.jspecify.annotations.NonNull;

/**
 * 登录服务接口。
 *
 * <p>定义登录、登出、令牌刷新和验证码相关操作的标准接口。
 *
 * <p>实现类应处理具体的认证逻辑，包括：
 * <ul>
 *   <li>用户名密码登录</li>
 *   <li>手机号登录</li>
 *   <li>邮箱登录</li>
 *   <li>第三方登录</li>
 * </ul>
 *
 * @since 1.0.0
 */
public interface LoginService {

    /**
     * 用户登录。
     *
     * <p>根据登录请求中的登录类型执行相应的认证逻辑。
     *
     * @param request 登录请求，永不为 null
     * @return 登录响应，包含访问令牌和用户信息，永不为 null
     */
    @NonNull
    LoginResponse login(@NonNull LoginRequest request);

    /**
     * 用户登出。
     *
     * <p>撤销当前用户的访问令牌。
     *
     * @param token 访问令牌，永不为 null
     */
    void logout(@NonNull String token);

    /**
     * 刷新访问令牌。
     *
     * <p>使用刷新令牌获取新的访问令牌。
     *
     * @param refreshToken 刷新令牌，永不为 null
     * @return 新的登录响应，包含新的访问令牌，永不为 null
     * @throws io.github.afgprojects.framework.security.core.token.TokenValidationException 如果刷新令牌无效或已过期
     */
    @NonNull
    LoginResponse refreshToken(@NonNull String refreshToken);

    /**
     * 生成验证码。
     *
     * <p>根据验证码类型生成相应的验证码：
     * <ul>
     *   <li>图形验证码：返回 Base64 编码的图片</li>
     *   <li>短信验证码：发送到指定手机号</li>
     *   <li>邮箱验证码：发送到指定邮箱</li>
     * </ul>
     *
     * @param request 验证码请求，永不为 null
     * @return 验证码响应，包含验证码 key 和相关信息，永不为 null
     */
    @NonNull
    CaptchaResponse generateCaptcha(@NonNull CaptchaRequest request);

    /**
     * 验证验证码。
     *
     * @param captchaKey 验证码 key，永不为 null
     * @param captchaValue 验证码值，永不为 null
     * @return 如果验证码正确则返回 true，否则返回 false
     */
    boolean validateCaptcha(@NonNull String captchaKey, @NonNull String captchaValue);
}
