package io.github.afgprojects.framework.security.auth.login;

import java.util.Set;
import java.util.stream.Collectors;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import io.github.afgprojects.framework.security.core.authentication.AfgUserDetails;
import io.github.afgprojects.framework.security.core.authentication.AfgUserDetailsService;
import io.github.afgprojects.framework.security.core.login.CaptchaService;
import io.github.afgprojects.framework.security.core.login.LoginService;
import io.github.afgprojects.framework.security.core.login.TokenService;
import io.github.afgprojects.framework.security.core.login.model.CaptchaRequest;
import io.github.afgprojects.framework.security.core.login.model.CaptchaResponse;
import io.github.afgprojects.framework.security.core.login.model.LoginRequest;
import io.github.afgprojects.framework.security.core.login.model.LoginResponse;
import io.github.afgprojects.framework.security.core.token.TokenValidationException;

/**
 * 默认登录服务实现。
 *
 * <p>支持多种登录方式：
 * <ul>
 *   <li>用户名密码登录</li>
 *   <li>用户名密码验证码登录</li>
 *   <li>手机号验证码登录</li>
 *   <li>邮箱验证码登录</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class DefaultLoginService implements LoginService {

    private final AfgUserDetailsService userDetailsService;
    private final TokenService tokenService;
    private final CaptchaService captchaService;
    private final PasswordEncoder passwordEncoder;

    /**
     * 构造函数。
     *
     * @param userDetailsService 用户详情服务
     * @param tokenService 令牌服务
     * @param captchaService 验证码服务
     * @param passwordEncoder 密码编码器
     */
    public DefaultLoginService(
            @NonNull AfgUserDetailsService userDetailsService,
            @NonNull TokenService tokenService,
            @NonNull CaptchaService captchaService,
            @NonNull PasswordEncoder passwordEncoder) {
        this.userDetailsService = userDetailsService;
        this.tokenService = tokenService;
        this.captchaService = captchaService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @NonNull
    public LoginResponse login(@NonNull LoginRequest request) {
        return switch (request.loginType()) {
            case USERNAME -> handleUsernameLogin(request);
            case MOBILE -> handleMobileLogin(request);
            case EMAIL -> handleEmailLogin(request);
            case THIRD_PARTY -> throw new UnsupportedOperationException("第三方登录暂不支持");
        };
    }

    @Override
    public void logout(@NonNull String token) {
        tokenService.invalidateToken(token);
    }

    @Override
    @NonNull
    public LoginResponse refreshToken(@NonNull String refreshToken) {
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
     * 处理用户名密码登录。
     */
    private LoginResponse handleUsernameLogin(LoginRequest request) {
        String username = request.username();
        String password = request.password();

        // 验证验证码（如果提供）
        if (request.captchaKey() != null && request.captchaValue() != null) {
            if (!captchaService.validate(request.captchaKey(), request.captchaValue())) {
                throw new RuntimeException("验证码错误");
            }
        }

        // 加载用户详情
        AfgUserDetails userDetails;
        try {
            userDetails = userDetailsService.loadUserByUsername(username);
        } catch (UsernameNotFoundException e) {
            throw e;
        }

        // 验证密码
        if (!passwordEncoder.matches(password, userDetails.getPassword())) {
            throw new RuntimeException("密码错误");
        }

        // 检查账号状态
        if (!userDetails.isEnabled()) {
            throw new RuntimeException("账号已被禁用");
        }

        if (!userDetails.isAccountNonLocked()) {
            throw new RuntimeException("账号已被锁定");
        }

        if (!userDetails.isAccountNonExpired()) {
            throw new RuntimeException("账号已过期");
        }

        if (!userDetails.isCredentialsNonExpired()) {
            throw new RuntimeException("凭证已过期");
        }

        return generateLoginResponse(userDetails);
    }

    /**
     * 处理手机号验证码登录。
     */
    private LoginResponse handleMobileLogin(LoginRequest request) {
        String mobile = request.mobile();
        String captchaValue = request.captchaValue();

        // 验证验证码
        String captchaKey = "sms:" + mobile;
        if (!captchaService.validate(captchaKey, captchaValue)) {
            throw new RuntimeException("验证码错误");
        }

        // 加载用户详情
        AfgUserDetails userDetails = userDetailsService.loadUserByMobile(mobile);

        // 检查账号状态
        validateAccountStatus(userDetails);

        return generateLoginResponse(userDetails);
    }

    /**
     * 处理邮箱验证码登录。
     */
    private LoginResponse handleEmailLogin(LoginRequest request) {
        String email = request.email();
        String captchaValue = request.captchaValue();

        // 验证验证码
        String captchaKey = "email:" + email;
        if (!captchaService.validate(captchaKey, captchaValue)) {
            throw new RuntimeException("验证码错误");
        }

        // 加载用户详情
        AfgUserDetails userDetails = userDetailsService.loadUserByEmail(email);

        // 检查账号状态
        validateAccountStatus(userDetails);

        return generateLoginResponse(userDetails);
    }

    /**
     * 验证账号状态。
     */
    private void validateAccountStatus(AfgUserDetails userDetails) {
        if (!userDetails.isEnabled()) {
            throw new RuntimeException("账号已被禁用");
        }

        if (!userDetails.isAccountNonLocked()) {
            throw new RuntimeException("账号已被锁定");
        }

        if (!userDetails.isAccountNonExpired()) {
            throw new RuntimeException("账号已过期");
        }

        if (!userDetails.isCredentialsNonExpired()) {
            throw new RuntimeException("凭证已过期");
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
