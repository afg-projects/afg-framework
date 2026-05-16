package io.github.afgprojects.framework.security.auth.login;

import lombok.extern.slf4j.Slf4j;
import io.github.afgprojects.framework.security.core.authentication.AfgUserDetails;
import io.github.afgprojects.framework.security.core.authentication.AfgUserDetailsService;
import io.github.afgprojects.framework.security.core.login.CaptchaService;
import io.github.afgprojects.framework.security.core.login.LoginService;
import io.github.afgprojects.framework.security.core.login.TokenService;
import io.github.afgprojects.framework.security.core.login.model.CaptchaRequest;
import io.github.afgprojects.framework.security.core.login.model.CaptchaResponse;
import io.github.afgprojects.framework.security.core.login.model.LoginRequest;
import io.github.afgprojects.framework.security.core.login.model.LoginResponse;
import io.github.afgprojects.framework.security.core.login.strategy.LoginStrategy;
import io.github.afgprojects.framework.security.core.login.strategy.LoginStrategyFactory;
import io.github.afgprojects.framework.security.core.token.TokenValidationException;
import org.jspecify.annotations.NonNull;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 默认登录服务实现。
 *
 * <p>使用策略模式支持多种登录方式，通过 {@link LoginStrategyFactory} 查找和执行登录策略。
 *
 * <p>内置策略：
 * <ul>
 *   <li>USERNAME - 用户名密码登录</li>
 *   <li>MOBILE - 手机号验证码登录</li>
 *   <li>EMAIL - 邮箱验证码登录</li>
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
@Slf4j
public class DefaultLoginService implements LoginService {
    private final LoginStrategyFactory strategyFactory;
    private final AfgUserDetailsService userDetailsService;
    private final TokenService tokenService;
    private final CaptchaService captchaService;

    /**
     * 构造函数。
     *
     * @param strategyFactory 登录策略工厂
     * @param userDetailsService 用户详情服务
     * @param tokenService 令牌服务
     * @param captchaService 验证码服务
     */
    public DefaultLoginService(
            @NonNull LoginStrategyFactory strategyFactory,
            @NonNull AfgUserDetailsService userDetailsService,
            @NonNull TokenService tokenService,
            @NonNull CaptchaService captchaService) {
        this.strategyFactory = strategyFactory;
        this.userDetailsService = userDetailsService;
        this.tokenService = tokenService;
        this.captchaService = captchaService;
    }

    @Override
    @NonNull
    public LoginResponse login(@NonNull LoginRequest request) {
        log.debug("Processing login request: type={}", request.loginType());

        // 查找登录策略
        LoginStrategy strategy = strategyFactory.getStrategy(request)
                .orElseThrow(() -> new IllegalArgumentException(
                        "不支持的登录类型: " + request.loginType()));

        // 执行认证
        AfgUserDetails userDetails = strategy.authenticate(request);

        // 生成登录响应
        return generateLoginResponse(userDetails);
    }

    @Override
    public void logout(@NonNull String token) {
        log.debug("Processing logout request");
        tokenService.invalidateToken(token);
        log.info("User logged out successfully");
    }

    @Override
    @NonNull
    public LoginResponse refreshToken(@NonNull String refreshToken) {
        log.debug("Processing refresh token request");

        // 验证刷新令牌
        if (!tokenService.validateRefreshToken(refreshToken)) {
            throw TokenValidationException.invalid();
        }

        // 提取用户 ID
        String userId = tokenService.extractUserId(refreshToken);
        if (userId == null) {
            throw TokenValidationException.invalid();
        }

        // 加载用户详情
        AfgUserDetails userDetails = userDetailsService.loadUserByUserId(userId);

        // 检查账号状态
        validateAccountStatus(userDetails);

        // 生成新的令牌
        return generateLoginResponse(userDetails);
    }

    @Override
    @NonNull
    public CaptchaResponse generateCaptcha(@NonNull CaptchaRequest request) {
        return captchaService.generate(request);
    }

    @Override
    public boolean validateCaptcha(@NonNull String captchaKey, @NonNull String captchaValue) {
        return captchaService.validate(captchaKey, captchaValue);
    }

    /**
     * 验证账号状态。
     */
    private void validateAccountStatus(AfgUserDetails userDetails) {
        if (!userDetails.isEnabled()) {
            throw new IllegalArgumentException("账号已被禁用");
        }
        if (!userDetails.isAccountNonLocked()) {
            throw new IllegalArgumentException("账号已被锁定");
        }
        if (!userDetails.isAccountNonExpired()) {
            throw new IllegalArgumentException("账号已过期");
        }
        if (!userDetails.isCredentialsNonExpired()) {
            throw new IllegalArgumentException("凭证已过期");
        }
    }

    /**
     * 生成登录响应。
     */
    private LoginResponse generateLoginResponse(AfgUserDetails userDetails) {
        // 提取权限
        Set<String> permissions = userDetails.getAuthorities().stream()
                .map(auth -> auth.getAuthority())
                .collect(Collectors.toSet());

        // 生成令牌
        String accessToken = tokenService.generateAccessToken(
                userDetails.getUserId(),
                userDetails.getUsername(),
                userDetails.getRoles(),
                permissions,
                userDetails.getTenantId());

        String refreshToken = tokenService.generateRefreshToken(
                userDetails.getUserId(),
                userDetails.getTenantId());

        log.info("User logged in successfully: userId={}, username={}",
                userDetails.getUserId(), userDetails.getUsername());

        // 构建响应
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(tokenService.getAccessTokenTtl())
                .userId(userDetails.getUserId())
                .username(userDetails.getUsername())
                .roles(userDetails.getRoles().stream().toList())
                .permissions(permissions.stream().toList())
                .tenantId(userDetails.getTenantId())
                .build();
    }
}