package io.github.afgprojects.framework.security.auth.entity;

import lombok.Data;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;

/**
 * 验证码实体。
 *
 * <p>用于存储各类验证码（图形、短信、邮箱）。
 *
 * @since 1.0.0
 */
@Data
public class AuthCaptcha {

    /**
     * 验证码唯一标识
     */
    private String captchaId;

    /**
     * 验证码 Key（用于客户端查询）
     */
    private String captchaKey;

    /**
     * 验证码值
     */
    private String captchaValue;

    /**
     * 验证码类型（IMAGE、SMS、EMAIL）
     */
    private String captchaType;

    /**
     * 目标（手机号或邮箱，短信/邮箱验证码时使用）
     */
    private @Nullable String target;

    /**
     * 过期时间
     */
    private LocalDateTime expiresAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}