package io.github.afgprojects.framework.security.auth.login;

import lombok.extern.slf4j.Slf4j;
import io.github.afgprojects.framework.commons.exception.BusinessException;
import io.github.afgprojects.framework.commons.exception.CommonErrorCode;
import io.github.afgprojects.framework.security.core.authentication.AfgUserDetails;
import io.github.afgprojects.framework.security.core.authentication.AfgUserDetailsService;
import io.github.afgprojects.framework.security.core.audit.LoginLogService;
import io.github.afgprojects.framework.security.core.audit.model.LoginLog;
import io.github.afgprojects.framework.security.core.login.CaptchaService;
import io.github.afgprojects.framework.security.core.login.LoginService;
import io.github.afgprojects.framework.security.core.login.TokenService;
import io.github.afgprojects.framework.security.core.login.model.CaptchaRequest;
import io.github.afgprojects.framework.security.core.login.model.CaptchaResponse;
import io.github.afgprojects.framework.security.core.login.model.LoginRequest;
import io.github.afgprojects.framework.security.core.login.model.LoginResponse;
import io.github.afgprojects.framework.security.core.login.strategy.LoginStrategy;
import io.github.afgprojects.framework.security.core.login.strategy.LoginStrategyFactory;
import io.github.afgprojects.framework.security.core.security.DeviceLimiter;
import io.github.afgprojects.framework.security.core.security.IpRestrictionChecker;
import io.github.afgprojects.framework.security.core.security.LoginFailureTracker;
import io.github.afgprojects.framework.security.core.security.PasswordValidator;
import io.github.afgprojects.framework.security.core.token.TokenValidationException;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
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
     * 登录失败追踪器（可选）
     */
    @Nullable
    private final LoginFailureTracker loginFailureTracker;

    /**
     * 密码验证器（可选）
     */
    @Nullable
    private final PasswordValidator passwordValidator;

    /**
     * IP 限制检查器（可选）
     */
    @Nullable
    private final IpRestrictionChecker ipRestrictionChecker;

    /**
     * 设备限制器（可选）
     */
    @Nullable
    private final DeviceLimiter deviceLimiter;

    /**
     * 登录日志服务（可选）
     */
    @Nullable
    private final LoginLogService loginLogService;

    /**
     * 构造函数（兼容旧版本，不启用安全策略）。
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
        this(strategyFactory, userDetailsService, tokenService, captchaService,
                null, null, null, null, null);
    }

    /**
     * 完整构造函数（启用所有安全策略）。
     *
     * @param strategyFactory 登录策略工厂
     * @param userDetailsService 用户详情服务
     * @param tokenService 令牌服务
     * @param captchaService 验证码服务
     * @param loginFailureTracker 登录失败追踪器（可为 null）
     * @param passwordValidator 密码验证器（可为 null）
     * @param ipRestrictionChecker IP 限制检查器（可为 null）
     * @param deviceLimiter 设备限制器（可为 null）
     * @param loginLogService 登录日志服务（可为 null）
     */
    public DefaultLoginService(
            @NonNull LoginStrategyFactory strategyFactory,
            @NonNull AfgUserDetailsService userDetailsService,
            @NonNull TokenService tokenService,
            @NonNull CaptchaService captchaService,
            @Nullable LoginFailureTracker loginFailureTracker,
            @Nullable PasswordValidator passwordValidator,
            @Nullable IpRestrictionChecker ipRestrictionChecker,
            @Nullable DeviceLimiter deviceLimiter,
            @Nullable LoginLogService loginLogService) {
        this.strategyFactory = strategyFactory;
        this.userDetailsService = userDetailsService;
        this.tokenService = tokenService;
        this.captchaService = captchaService;
        this.loginFailureTracker = loginFailureTracker;
        this.passwordValidator = passwordValidator;
        this.ipRestrictionChecker = ipRestrictionChecker;
        this.deviceLimiter = deviceLimiter;
        this.loginLogService = loginLogService;
    }

    @Override
    @NonNull
    public LoginResponse login(@NonNull LoginRequest request) {
        log.debug("Processing login request: type={}", request.loginType());

        LoginContext ctx = createLoginContext(request);

        // 1. IP 限制检查
        checkIpRestriction(ctx);

        // 2. 执行认证
        AfgUserDetails userDetails = authenticate(request, ctx);

        // 3. 检查账户锁定状态
        checkAccountLock(userDetails, ctx);

        // 4. 检查账号状态
        validateAccountStatus(userDetails);

        // 5. 注册设备
        registerDevice(userDetails, ctx);

        // 6. 清除登录失败记录
        resetLoginFailure(userDetails, ctx);

        // 7. 生成并返回登录响应
        return buildSuccessResponse(userDetails, ctx);
    }

    /**
     * 创建登录上下文
     */
    private LoginContext createLoginContext(LoginRequest request) {
        return new LoginContext(
                extractIp(request),
                extractUsername(request),
                request.deviceId(),
                request.deviceName(),
                request.tenantId()
        );
    }

    /**
     * 检查 IP 限制
     */
    private void checkIpRestriction(LoginContext ctx) {
        if (ipRestrictionChecker != null && !ipRestrictionChecker.isAllowed(ctx.ip, null, ctx.tenantId)) {
            log.warn("Login rejected: IP {} is restricted", ctx.ip);
            recordFailureLog(ctx.username, ctx, "IP 已被限制");
            throw new BusinessException(CommonErrorCode.FORBIDDEN, "IP 已被限制");
        }
    }

    /**
     * 执行认证
     */
    private AfgUserDetails authenticate(LoginRequest request, LoginContext ctx) {
        LoginStrategy strategy = strategyFactory.getStrategy(request)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.PARAM_ERROR,
                        "不支持的登录类型: " + request.loginType()));

        try {
            return strategy.authenticate(request);
        } catch (Exception e) {
            log.warn("Authentication failed for user: {}", ctx.username, e);
            recordFailureLog(ctx.username != null ? ctx.username : "unknown", ctx, e.getMessage());
            throw e;
        }
    }

    /**
     * 检查账户锁定状态
     */
    private void checkAccountLock(AfgUserDetails userDetails, LoginContext ctx) {
        if (loginFailureTracker != null && loginFailureTracker.isLocked(userDetails.getUserId(), ctx.tenantId)) {
            log.warn("Login rejected: account {} is locked", userDetails.getUserId());
            recordFailureLog(userDetails.getUsername(), ctx, "账户已被锁定");
            throw new BusinessException(CommonErrorCode.ACCOUNT_LOCKED, "账户已被锁定");
        }
    }

    /**
     * 注册设备
     */
    private void registerDevice(AfgUserDetails userDetails, LoginContext ctx) {
        if (deviceLimiter != null && ctx.deviceId != null && !ctx.deviceId.isEmpty()) {
            boolean registered = deviceLimiter.registerDevice(
                    userDetails.getUserId(), ctx.tenantId, ctx.deviceId, ctx.deviceName, null, ctx.ip);
            if (!registered) {
                log.warn("Failed to register device {} for user {}", ctx.deviceId, userDetails.getUserId());
            }
        }
    }

    /**
     * 清除登录失败记录
     */
    private void resetLoginFailure(AfgUserDetails userDetails, LoginContext ctx) {
        if (loginFailureTracker != null) {
            loginFailureTracker.reset(userDetails.getUserId(), ctx.tenantId);
        }
    }

    /**
     * 构建成功响应
     */
    private LoginResponse buildSuccessResponse(AfgUserDetails userDetails, LoginContext ctx) {
        LoginResponse response = generateLoginResponse(userDetails);

        recordLoginLog(LoginLog.success(
                userDetails.getUserId(), userDetails.getUsername(), ctx.tenantId,
                ctx.ip, ctx.deviceId, ctx.deviceName, null, null, null));

        log.info("User logged in successfully: userId={}, username={}",
                userDetails.getUserId(), userDetails.getUsername());

        return response;
    }

    /**
     * 记录失败日志
     */
    private void recordFailureLog(String username, LoginContext ctx, String message) {
        recordLoginLog(LoginLog.failure(username, ctx.tenantId, ctx.ip, ctx.deviceId, ctx.deviceName,
                null, null, null, message));
    }

    /**
     * 登录上下文（内部使用）
     */
    private record LoginContext(String ip, String username, String deviceId, String deviceName, String tenantId) {}

    @Override
    public void logout(@NonNull String token) {
        log.debug("Processing logout request");
        tokenService.invalidateToken(token);

        // 记录登出日志
        if (loginLogService != null) {
            String userId = tokenService.extractUserId(token);
            String tenantId = tokenService.extractTenantId(token);
            if (userId != null) {
                // IP 信息无法从 token 中获取，使用空字符串
                loginLogService.recordLogout(userId, tenantId, "");
            }
        }

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
     * 记录登录日志。
     */
    private void recordLoginLog(LoginLog log) {
        if (loginLogService != null) {
            loginLogService.recordLogin(log);
        }
    }

    /**
     * 从登录请求中提取 IP。
     */
    private String extractIp(LoginRequest request) {
        return request.ip() != null ? request.ip() : "unknown";
    }

    /**
     * 从登录请求中提取用户名。
     */
    private String extractUsername(LoginRequest request) {
        return switch (request.loginType()) {
            case USERNAME -> request.username();
            case MOBILE -> request.mobile();
            case EMAIL -> request.email();
            default -> request.username();
        };
    }

    /**
     * 验证账号状态。
     */
    private void validateAccountStatus(AfgUserDetails userDetails) {
        if (!userDetails.isEnabled()) {
            throw new BusinessException(CommonErrorCode.ACCOUNT_DISABLED, "账号已被禁用");
        }
        if (!userDetails.isAccountNonLocked()) {
            throw new BusinessException(CommonErrorCode.ACCOUNT_LOCKED, "账号已被锁定");
        }
        if (!userDetails.isAccountNonExpired()) {
            throw new BusinessException(CommonErrorCode.ACCOUNT_DISABLED, "账号已过期");
        }
        if (!userDetails.isCredentialsNonExpired()) {
            throw new BusinessException(CommonErrorCode.PASSWORD_EXPIRED, "凭证已过期");
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