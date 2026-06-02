package io.github.afgprojects.framework.security.auth.entity;

import io.github.afgprojects.framework.apt.entity.AfEntity;
import io.github.afgprojects.framework.data.core.entity.BaseEntity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * 验证码实体。
 *
 * <p>用于存储各类验证码（图形、短信、邮箱）。
 *
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AfEntity
@Table(name = "auth_captcha")
public class AuthCaptcha extends BaseEntity {

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
    private Instant expiresAt;
}