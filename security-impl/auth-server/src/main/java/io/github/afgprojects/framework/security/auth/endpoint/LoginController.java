package io.github.afgprojects.framework.security.auth.endpoint;

import io.github.afgprojects.framework.security.core.login.LoginService;
import io.github.afgprojects.framework.security.core.login.model.CaptchaRequest;
import io.github.afgprojects.framework.security.core.login.model.CaptchaResponse;
import io.github.afgprojects.framework.security.core.login.model.LoginRequest;
import io.github.afgprojects.framework.security.core.login.model.LoginResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录 REST 控制器。
 *
 * <p>提供登录相关的 REST API：
 * <ul>
 *   <li>POST /login - 用户登录</li>
 *   <li>POST /logout - 用户登出</li>
 *   <li>POST /refresh - 刷新令牌</li>
 *   <li>GET /captcha - 获取图形验证码</li>
 *   <li>POST /captcha/sms - 发送短信验证码</li>
 *   <li>POST /captcha/email - 发送邮箱验证码</li>
 * </ul>
 *
 * <p>框架通过 ModuleWebAutoConfiguration 自动为 auth-server 模块下的 Controller
 * 添加 /auth-api 前缀，因此完整路径为 /auth-api/auth/login 等。
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/auth")
public class LoginController {

    private final LoginService loginService;

    public LoginController(LoginService loginService) {
        this.loginService = loginService;
    }

    /**
     * 用户登录。
     *
     * @param request 登录请求
     * @return 登录响应
     */
    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return loginService.login(request);
    }

    /**
     * 用户登出。
     *
     * @param authorization Authorization 请求头
     */
    @PostMapping("/logout")
    public void logout(@RequestHeader("Authorization") String authorization) {
        String token = extractToken(authorization);
        loginService.logout(token);
    }

    /**
     * 刷新访问令牌。
     *
     * @param request 刷新令牌请求
     * @return 新的登录响应
     */
    @PostMapping("/refresh")
    public LoginResponse refresh(@RequestBody RefreshTokenRequest request) {
        return loginService.refreshToken(request.refreshToken());
    }

    /**
     * 获取图形验证码。
     *
     * @return 验证码响应
     */
    @GetMapping("/captcha")
    public CaptchaResponse getCaptcha() {
        return loginService.generateCaptcha(CaptchaRequest.ofImage());
    }

    /**
     * 发送短信验证码。
     *
     * @param request 短信验证码请求
     * @return 验证码响应
     */
    @PostMapping("/captcha/sms")
    public CaptchaResponse sendSmsCaptcha(@RequestBody SmsCaptchaRequest request) {
        return loginService.generateCaptcha(CaptchaRequest.ofSms(request.mobile(), request.tenantId()));
    }

    /**
     * 发送邮箱验证码。
     *
     * @param request 邮箱验证码请求
     * @return 验证码响应
     */
    @PostMapping("/captcha/email")
    public CaptchaResponse sendEmailCaptcha(@RequestBody EmailCaptchaRequest request) {
        return loginService.generateCaptcha(CaptchaRequest.ofEmail(request.email(), request.tenantId()));
    }

    /**
     * 从 Authorization 头中提取 Token。
     */
    private String extractToken(String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        throw new IllegalArgumentException("Invalid authorization header");
    }

    /**
     * 刷新令牌请求。
     */
    public record RefreshTokenRequest(@NonNull String refreshToken) {}

    /**
     * 短信验证码请求。
     */
    public record SmsCaptchaRequest(@NonNull String mobile, String tenantId) {}

    /**
     * 邮箱验证码请求。
     */
    public record EmailCaptchaRequest(@NonNull String email, String tenantId) {}
}