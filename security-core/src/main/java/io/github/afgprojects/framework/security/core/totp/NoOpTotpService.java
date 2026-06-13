package io.github.afgprojects.framework.security.core.totp;

/**
 * TOTP 服务的 NoOp 降级实现。
 *
 * <p>当 TOTP 功能未启用时使用此实现，所有操作均返回默认值。
 *
 * @since 1.0.0
 */
public class NoOpTotpService implements TotpService {

    @Override
    public String generateSecret() {
        return "";
    }

    @Override
    public String generateQrCodeUrl(String username, String secret, String issuer) {
        return "";
    }

    @Override
    public boolean verifyCode(String secret, int code) {
        return false;
    }

    @Override
    public boolean verifyCode(String secret, int code, int window) {
        return false;
    }
}
