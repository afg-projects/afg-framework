package io.github.afgprojects.framework.security.core.totp;

/**
 * TOTP 服务 SPI。
 *
 * <p>提供 TOTP（基于时间的一次性密码）功能，用于双因素认证（2FA）。
 *
 * <p>实现要求：
 * <ul>
 *   <li>基于 RFC 6238 标准实现 TOTP 算法</li>
 *   <li>Secret 使用 Base32 编码</li>
 *   <li>QR Code URL 使用 otpauth://totp/ 格式</li>
 *   <li>支持 window 参数（允许时间偏移）</li>
 * </ul>
 *
 * @since 1.0.0
 */
public interface TotpService {

    /**
     * 生成随机 TOTP Secret。
     *
     * <p>生成的 Secret 应为 Base32 编码的随机字节序列，长度应不少于 160 位（32 个 Base32 字符）。
     *
     * @return Base32 编码的 Secret，永不为 null
     */
    String generateSecret();

    /**
     * 生成 TOTP QR Code URL。
     *
     * <p>URL 格式为 {@code otpauth://totp/{issuer}:{username}?secret={secret}&issuer={issuer}}，
     * 可被 Google Authenticator、Microsoft Authenticator 等应用扫描。
     *
     * @param username 用户名，永不为 null
     * @param secret TOTP Secret，永不为 null
     * @param issuer 发行者名称（如应用名），永不为 null
     * @return QR Code URL，永不为 null
     */
    String generateQrCodeUrl(String username, String secret, String issuer);

    /**
     * 验证 TOTP 验证码。
     *
     * <p>使用默认时间窗口（window=1），允许当前时间步前后各 1 个步长的偏差。
     *
     * @param secret TOTP Secret，永不为 null
     * @param code 待验证的 6 位数字验证码
     * @return 如果验证码有效则返回 true
     */
    boolean verifyCode(String secret, int code);

    /**
     * 验证 TOTP 验证码（带时间窗口）。
     *
     * <p>window 参数控制允许的时间偏差范围：
     * <ul>
     *   <li>window=0 — 仅验证当前时间步</li>
     *   <li>window=1 — 允许当前时间步前后各 1 个步长</li>
     *   <li>window=2 — 允许当前时间步前后各 2 个步长</li>
     * </ul>
     *
     * @param secret TOTP Secret，永不为 null
     * @param code 待验证的 6 位数字验证码
     * @param window 时间窗口大小，必须 >= 0
     * @return 如果验证码有效则返回 true
     */
    boolean verifyCode(String secret, int code, int window);
}
