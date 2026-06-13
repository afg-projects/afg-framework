package io.github.afgprojects.framework.security.auth.totp;

import io.github.afgprojects.framework.commons.model.Result;
import io.github.afgprojects.framework.security.core.authentication.AfgAuthentication;
import io.github.afgprojects.framework.security.core.authentication.AfgUserDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * TOTP 双因素认证 REST 控制器。
 *
 * <p>提供 TOTP 2FA 相关的 REST API：
 * <ul>
 *   <li>POST /auth/totp/setup - 生成 TOTP Secret 和 QR Code URL</li>
 *   <li>POST /auth/totp/verify - 验证 TOTP 验证码</li>
 *   <li>POST /auth/totp/enable - 启用 2FA</li>
 *   <li>POST /auth/totp/disable - 禁用 2FA</li>
 *   <li>GET /auth/totp/status - 获取 2FA 状态</li>
 * </ul>
 *
 * <p>框架通过 ModuleWebAutoConfiguration 自动为 auth-server 模块下的 Controller
 * 添加 /auth-api 前缀，因此完整路径为 /auth-api/auth/totp/setup 等。
 *
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/auth/totp")
public class TotpController {

    private final TwoFactorAuthenticationService twoFactorAuthService;

    /**
     * 构造函数。
     *
     * @param twoFactorAuthService 双因素认证服务
     */
    public TotpController(TwoFactorAuthenticationService twoFactorAuthService) {
        this.twoFactorAuthService = twoFactorAuthService;
    }

    /**
     * 生成 TOTP 设置信息。
     *
     * <p>返回 TOTP Secret 和 QR Code URL，用户需要用认证器应用扫描 QR Code。
     * Secret 尚未启用，需要用户验证后调用 enable 接口启用。
     *
     * @param request 设置请求
     * @return TOTP 设置响应
     */
    @PostMapping("/setup")
    public Result<TotpSetupResponse> setup(@RequestBody TotpSetupRequest request) {
        AfgUserDetails currentUser = getCurrentUser();
        TwoFactorAuthenticationService.TotpSetupResponse setupResponse =
                twoFactorAuthService.setup(currentUser.getUserId(), currentUser.getUsername(), request.issuer());

        return Result.success(new TotpSetupResponse(setupResponse.secret(), setupResponse.qrCodeUrl()));
    }

    /**
     * 验证 TOTP 验证码。
     *
     * @param request 验证请求
     * @return 验证结果
     */
    @PostMapping("/verify")
    public Result<Boolean> verify(@RequestBody TotpVerifyRequest request) {
        AfgUserDetails currentUser = getCurrentUser();
        boolean valid = twoFactorAuthService.verifyTotpCode(currentUser.getUserId(), request.code());
        return Result.success(valid);
    }

    /**
     * 启用 2FA。
     *
     * <p>启用成功后返回恢复码列表，请妥善保存。恢复码每个只能使用一次。
     *
     * @param request 启用请求
     * @return 恢复码列表
     */
    @PostMapping("/enable")
    public Result<List<String>> enable(@RequestBody TotpVerifyRequest request) {
        AfgUserDetails currentUser = getCurrentUser();
        List<String> recoveryCodes = twoFactorAuthService.enable2fa(currentUser.getUserId(), request.code());
        return Result.success(recoveryCodes);
    }

    /**
     * 禁用 2FA。
     *
     * @param request 禁用请求
     * @return 操作结果
     */
    @PostMapping("/disable")
    public Result<Void> disable(@RequestBody TotpVerifyRequest request) {
        AfgUserDetails currentUser = getCurrentUser();
        twoFactorAuthService.disable2fa(currentUser.getUserId(), request.code());
        return Result.success();
    }

    /**
     * 获取 2FA 状态。
     *
     * @return 2FA 是否启用
     */
    @GetMapping("/status")
    public Result<Boolean> status() {
        AfgUserDetails currentUser = getCurrentUser();
        boolean enabled = twoFactorAuthService.is2faEnabled(currentUser.getUserId());
        return Result.success(enabled);
    }

    /**
     * 获取当前登录用户。
     *
     * @return 当前用户详情
     */
    private AfgUserDetails getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof AfgAuthentication afgAuth) {
            return afgAuth.getUserDetails();
        }
        throw new IllegalStateException("未认证");
    }

    /**
     * TOTP 设置请求。
     */
    public record TotpSetupRequest(String issuer) {}

    /**
     * TOTP 设置响应。
     */
    public record TotpSetupResponse(String secret, String qrCodeUrl) {}

    /**
     * TOTP 验证请求。
     */
    public record TotpVerifyRequest(int code) {}
}
