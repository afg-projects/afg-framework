package io.github.afgprojects.framework.security.auth.storage;

import lombok.extern.slf4j.Slf4j;
import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.security.auth.entity.AuthCaptcha;
import io.github.afgprojects.framework.security.core.storage.AfgCaptchaStorage;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;

import static io.github.afgprojects.framework.data.core.condition.Conditions.*;

/**
 * 基于 DataManager 的验证码存储。
 *
 * <p>将验证码持久化到关系型数据库，支持：
 * <ul>
 *   <li>验证码保存与获取</li>
 *   <li>验证码删除</li>
 *   <li>自动过期清理</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Slf4j
public class JdbcCaptchaStorage implements AfgCaptchaStorage {

    private final DataManager dataManager;

    /**
     * 构造函数。
     *
     * @param dataManager 数据管理器
     */
    public JdbcCaptchaStorage(@NonNull DataManager dataManager) {
        this.dataManager = dataManager;
    }

    @Override
    public void save(@NonNull String key, @NonNull String value, @NonNull Duration ttl) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(ttl);

        var existing = dataManager.findOneByField(AuthCaptcha.class,
                AuthCaptcha::getCaptchaKey, key);

        // 从 key 推断验证码类型和目标
        String captchaType = inferCaptchaType(key);
        String target = inferTarget(key, captchaType);

        AuthCaptcha entity;
        if (existing.isPresent()) {
            entity = existing.get();
            entity.setCaptchaValue(value);
            entity.setCaptchaType(captchaType);
            entity.setTarget(target);
            entity.setExpiresAt(expiresAt);
            entity.setUpdatedAt(now);
        } else {
            entity = new AuthCaptcha();
            entity.setCaptchaKey(key);
            entity.setCaptchaValue(value);
            entity.setCaptchaType(captchaType);
            entity.setTarget(target);
            entity.setExpiresAt(expiresAt);
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
        }
        dataManager.save(AuthCaptcha.class, entity);
        log.debug("Saved captcha: key={}, type={}, target={}, ttl={}", key, captchaType, target, ttl);
    }

    /**
     * 从 key 推断验证码类型。
     *
     * <p>key 格式约定：
     * <ul>
     *   <li>IMAGE: 随机 UUID（无前缀）</li>
     *   <li>SMS: "sms:" + 手机号</li>
     *   <li>EMAIL: "email:" + 邮箱</li>
     * </ul>
     */
    private String inferCaptchaType(String key) {
        if (key.startsWith("sms:")) {
            return "SMS";
        } else if (key.startsWith("email:")) {
            return "EMAIL";
        }
        return "IMAGE";
    }

    /**
     * 从 key 推断验证码目标（手机号或邮箱）。
     */
    @Nullable
    private String inferTarget(String key, String captchaType) {
        return switch (captchaType) {
            case "SMS" -> key.substring(4); // 去掉 "sms:" 前缀
            case "EMAIL" -> key.substring(6); // 去掉 "email:" 前缀
            default -> null; // IMAGE 类型无 target
        };
    }

    @Override
    @Nullable
    public String get(@NonNull String key) {
        deleteExpired();

        return dataManager.findOne(AuthCaptcha.class,
                builder(AuthCaptcha.class)
                        .eq(AuthCaptcha::getCaptchaKey, key)
                        .gt(AuthCaptcha::getExpiresAt, Instant.now())
                        .build())
                .map(AuthCaptcha::getCaptchaValue)
                .orElse(null);
    }

    @Override
    public void delete(@NonNull String key) {
        dataManager.findOneByField(AuthCaptcha.class, AuthCaptcha::getCaptchaKey, key)
                .ifPresent(entity -> {
                    dataManager.deleteById(AuthCaptcha.class, entity.getId());
                    log.debug("Deleted captcha: key={}", key);
                });
    }

    @Override
    public boolean exists(@NonNull String key) {
        deleteExpired();

        return dataManager.existsByCondition(AuthCaptcha.class,
                builder(AuthCaptcha.class)
                        .eq(AuthCaptcha::getCaptchaKey, key)
                        .gt(AuthCaptcha::getExpiresAt, Instant.now())
                        .build());
    }

    /**
     * 删除过期的验证码记录。
     *
     * @return 删除的记录数
     */
    public int deleteExpired() {
        Instant now = Instant.now();
        var entities = dataManager.findList(AuthCaptcha.class,
                builder(AuthCaptcha.class)
                        .lt(AuthCaptcha::getExpiresAt, now)
                        .build());

        for (var entity : entities) {
            dataManager.deleteById(AuthCaptcha.class, entity.getId());
        }

        int count = entities.size();
        if (count > 0) {
            log.debug("Deleted expired captcha records: count={}", count);
        }
        return count;
    }
}
