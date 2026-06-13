package io.github.afgprojects.framework.security.auth.properties.totp;

import lombok.Data;

/**
 * TOTP 双因素认证配置。
 *
 * @since 1.0.0
 */
@Data
public class TotpConfig {

    /**
     * 是否启用 TOTP 双因素认证。
     * 默认关闭。
     */
    private boolean enabled = false;

    /**
     * TOTP 发行者名称。
     * 用于 QR Code URL 中的 issuer 参数。
     * 默认为 "AFG-Framework"。
     */
    private String issuer = "AFG-Framework";

    /**
     * 验证码时间窗口。
     * 允许当前时间步前后各 window 个步长的偏差。
     * 默认为 1（前后各 1 个 30 秒步长）。
     */
    private int window = 1;
}
