package io.github.afgprojects.framework.security.auth.endpoint;

import io.github.afgprojects.framework.commons.model.Result;
import io.github.afgprojects.framework.security.core.authentication.AfgAuthentication;
import io.github.afgprojects.framework.security.core.authentication.AfgUserDetails;
import io.github.afgprojects.framework.security.core.authentication.AfgUserDetailsService;
import io.github.afgprojects.framework.security.core.login.LoginService;
import io.github.afgprojects.framework.security.core.login.model.CaptchaRequest;
import io.github.afgprojects.framework.security.core.login.model.CaptchaResponse;
import io.github.afgprojects.framework.security.core.login.model.LoginRequest;
import io.github.afgprojects.framework.security.core.login.model.LoginResponse;
import io.github.afgprojects.framework.security.core.token.JwtClaimsExtractor;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

/**
 * 登录 REST 控制器。
 *
 * <p>提供登录相关的 REST API：
 * <ul>
 *   <li>POST /login - 用户登录</li>
 *   <li>POST /logout - 用户登出</li>
 *   <li>POST /refresh - 刷新令牌</li>
 *   <li>GET /user-info - 获取当前登录用户信息</li>
 *   <li>GET /session - 检查当前会话状态</li>
 *   <li>GET /captcha - 获取图形验证码</li>
 *   <li>POST /captcha/sms - 发送短信验证码</li>
 *   <li>POST /captcha/email - 发送邮箱验证码</li>
 * </ul>
 *
 * <p>框架通过 ModuleWebAutoConfiguration 自动为 auth-server 模块下的 Controller
 * 添加 /auth-api 前缀，因此完整路径为 /auth-api/auth/login 等。
 *
 * <p>需要认证的端点（/logout、/user-info）由 {@link io.github.afgprojects.framework.security.auth.token.AuthServerBearerTokenFilter}
 * 自动解析 Bearer Token 并填充 SecurityContext，控制器直接从 SecurityContext 获取认证信息即可。
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/auth")
public class LoginController {

    private final LoginService loginService;
    private final AfgUserDetailsService userDetailsService;

    public LoginController(LoginService loginService,
                           @Nullable AfgUserDetailsService userDetailsService) {
        this.loginService = loginService;
        this.userDetailsService = userDetailsService;
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
        String token = JwtClaimsExtractor.extractBearerToken(authorization);
        if (token == null) {
            throw new IllegalArgumentException("Invalid authorization header");
        }
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
     * 获取当前登录用户信息。
     *
     * <p>由 {@link io.github.afgprojects.framework.security.auth.token.AuthServerBearerTokenFilter}
     * 在请求前自动解析 Bearer Token 并填充 SecurityContext，此方法直接从 SecurityContext 获取认证信息。
     *
     * <p>如果有 UserDetailsService，尝试获取更完整的用户信息（如昵称、头像等）。
     *
     * @param httpRequest HTTP 请求
     * @return 用户信息响应
     */
    @GetMapping("/user-info")
    public Result<UserInfoResponse> userInfo(HttpServletRequest httpRequest) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof AfgAuthentication afgAuth)) {
            return Result.fail(401, "未认证");
        }

        AfgUserDetails details = afgAuth.getUserDetails();

        // 如果有 UserDetailsService，尝试获取更完整的用户信息（如昵称、头像等）
        if (userDetailsService != null) {
            try {
                AfgUserDetails fullDetails = userDetailsService.loadUserByUserId(details.getUserId());
                return Result.success(new UserInfoResponse(
                        fullDetails.getUserId(),
                        fullDetails.getUsername(),
                        fullDetails.getDisplayName(),
                        null,
                        fullDetails.getRoles(),
                        fullDetails.getAuthorities().stream()
                                .map(auth -> auth.getAuthority())
                                .collect(Collectors.toSet()),
                        fullDetails.getTenantId()));
            } catch (Exception e) {
                // 降级到 Token 中的信息
            }
        }

        return Result.success(new UserInfoResponse(
                details.getUserId(),
                details.getUsername(),
                details.getDisplayName(),
                null,
                details.getRoles(),
                details.getAuthorities().stream()
                        .map(auth -> auth.getAuthority())
                        .collect(Collectors.toSet()),
                details.getTenantId()));
    }

    /**
     * 检查当前会话状态。
     *
     * <p>SSO 前端调用此端点检查用户是否已登录。
     * 由 {@link io.github.afgprojects.framework.security.auth.token.AuthServerBearerTokenFilter}
     * 自动解析 Bearer Token 并填充 SecurityContext。
     *
     * @return 会话响应
     */
    @GetMapping("/session")
    public SessionResponse session() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof AfgAuthentication afgAuth)) {
            return new SessionResponse(false, null);
        }
        AfgUserDetails details = afgAuth.getUserDetails();
        return new SessionResponse(true, new SessionUser(details.getUsername(), details.getDisplayName()));
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
     * 会话响应。
     */
    public record SessionResponse(boolean authenticated, @Nullable SessionUser user) {}

    /**
     * 会话用户信息。
     */
    public record SessionUser(@NonNull String username, @Nullable String realName) {}

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