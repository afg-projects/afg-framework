package io.github.afgprojects.framework.security.auth.totp;

import io.github.afgprojects.framework.commons.exception.BusinessException;
import io.github.afgprojects.framework.commons.exception.CommonErrorCode;
import io.github.afgprojects.framework.security.core.totp.TotpService;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 双因素认证服务。
 *
 * <p>管理用户的 2FA 启用/禁用、验证 TOTP 验证码、生成恢复码。
 *
 * <p>注意：当前实现使用内存存储用户的 TOTP Secret 和恢复码，
 * 生产环境应替换为数据库存储（通过 DataManager）。
 *
 * @since 1.0.0
 */
@Slf4j
public class TwoFactorAuthenticationService {

    private static final int RECOVERY_CODE_COUNT = 10;
    private static final int RECOVERY_CODE_LENGTH = 8;

    private final TotpService totpService;

    /**
     * 用户 TOTP Secret 存储（userId -> secret）。
     * 生产环境应替换为数据库存储。
     */
    private final Map<String, String> userSecrets = new ConcurrentHashMap<>();

    /**
     * 用户恢复码存储（userId -> recovery codes）。
     * 生产环境应替换为数据库存储。
     */
    private final Map<String, List<String>> userRecoveryCodes = new ConcurrentHashMap<>();

    /**
     * 已启用 2FA 的用户（userId -> enabled）。
     * 生产环境应替换为数据库存储。
     */
    private final Map<String, Boolean> user2faEnabled = new ConcurrentHashMap<>();

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 构造函数。
     *
     * @param totpService TOTP 服务
     */
    public TwoFactorAuthenticationService(@NonNull TotpService totpService) {
        this.totpService = totpService;
    }

    /**
     * 生成 TOTP 设置信息（secret + QR Code URL）。
     *
     * <p>生成的 secret 尚未启用，需要用户验证后调用 {@link #enable2fa} 启用。
     *
     * @param userId 用户 ID
     * @param username 用户名
     * @param issuer 发行者名称（如应用名）
     * @return TOTP 设置响应（包含 secret 和 QR Code URL）
     */
    public TotpSetupResponse setup(@NonNull String userId, @NonNull String username, @NonNull String issuer) {
        String secret = totpService.generateSecret();
        String qrCodeUrl = totpService.generateQrCodeUrl(username, secret, issuer);

        // 临时存储 secret，等待用户验证
        userSecrets.put(userId, secret);

        log.info("TOTP setup initiated for userId={}", userId);

        return new TotpSetupResponse(secret, qrCodeUrl);
    }

    /**
     * 验证 TOTP 验证码。
     *
     * @param userId 用户 ID
     * @param code TOTP 验证码
     * @return 验证是否成功
     */
    public boolean verifyTotpCode(@NonNull String userId, int code) {
        String secret = userSecrets.get(userId);
        if (secret == null) {
            log.warn("No TOTP secret found for userId={}", userId);
            return false;
        }
        return totpService.verifyCode(secret, code);
    }

    /**
     * 启用 2FA。
     *
     * <p>用户需要先通过 {@link #setup} 生成 secret，然后在认证器应用中验证一个 TOTP 码后调用此方法启用。
     *
     * @param userId 用户 ID
     * @param code 验证码（必须与 secret 生成的验证码匹配）
     * @return 恢复码列表（启用成功后生成，请妥善保存）
     * @throws BusinessException 如果验证码不正确
     */
    public List<String> enable2fa(@NonNull String userId, int code) {
        String secret = userSecrets.get(userId);
        if (secret == null) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR, "请先设置 TOTP");
        }

        if (!totpService.verifyCode(secret, code)) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR, "验证码错误");
        }

        // 启用 2FA
        user2faEnabled.put(userId, true);

        // 生成恢复码
        List<String> recoveryCodes = generateRecoveryCodes();
        userRecoveryCodes.put(userId, recoveryCodes);

        log.info("2FA enabled for userId={}", userId);

        return recoveryCodes;
    }

    /**
     * 禁用 2FA。
     *
     * @param userId 用户 ID
     * @param code 验证码（用于确认操作者身份）
     * @throws BusinessException 如果验证码不正确
     */
    public void disable2fa(@NonNull String userId, int code) {
        if (!is2faEnabled(userId)) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR, "2FA 未启用");
        }

        if (!totpService.verifyCode(userSecrets.get(userId), code)) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR, "验证码错误");
        }

        user2faEnabled.remove(userId);
        userSecrets.remove(userId);
        userRecoveryCodes.remove(userId);

        log.info("2FA disabled for userId={}", userId);
    }

    /**
     * 使用恢复码验证。
     *
     * <p>每个恢复码只能使用一次，使用后自动从列表中移除。
     *
     * @param userId 用户 ID
     * @param recoveryCode 恢复码
     * @return 验证是否成功
     */
    public boolean verifyRecoveryCode(@NonNull String userId, @NonNull String recoveryCode) {
        List<String> codes = userRecoveryCodes.get(userId);
        if (codes == null) {
            return false;
        }

        boolean removed = codes.remove(recoveryCode);
        if (removed) {
            log.info("Recovery code used for userId={}, remaining={}", userId, codes.size());
            // 如果恢复码用尽，可能需要提醒用户重新生成
            if (codes.isEmpty()) {
                userRecoveryCodes.remove(userId);
                log.warn("All recovery codes used for userId={}", userId);
            }
        }
        return removed;
    }

    /**
     * 检查用户是否启用了 2FA。
     *
     * @param userId 用户 ID
     * @return 是否启用 2FA
     */
    public boolean is2faEnabled(@NonNull String userId) {
        return Boolean.TRUE.equals(user2faEnabled.get(userId));
    }

    /**
     * 获取用户的 TOTP Secret。
     *
     * @param userId 用户 ID
     * @return TOTP Secret，未设置返回 null
     */
    @Nullable
    public String getUserSecret(@NonNull String userId) {
        return userSecrets.get(userId);
    }

    /**
     * 生成恢复码列表。
     *
     * @return 恢复码列表
     */
    private List<String> generateRecoveryCodes() {
        List<String> codes = new ArrayList<>(RECOVERY_CODE_COUNT);
        for (int i = 0; i < RECOVERY_CODE_COUNT; i++) {
            codes.add(generateRecoveryCode());
        }
        return codes;
    }

    /**
     * 生成单个恢复码。
     *
     * @return 8 位字母数字恢复码
     */
    private String generateRecoveryCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // 排除容易混淆的字符
        StringBuilder sb = new StringBuilder(RECOVERY_CODE_LENGTH);
        for (int i = 0; i < RECOVERY_CODE_LENGTH; i++) {
            sb.append(chars.charAt(secureRandom.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * TOTP 设置响应。
     */
    public record TotpSetupResponse(String secret, String qrCodeUrl) {}
}
