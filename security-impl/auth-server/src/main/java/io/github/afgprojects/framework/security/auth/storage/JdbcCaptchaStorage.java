package io.github.afgprojects.framework.security.auth.storage;

import lombok.extern.slf4j.Slf4j;
import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.security.auth.entity.AuthCaptcha;
import io.github.afgprojects.framework.security.core.storage.AfgCaptchaStorage;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.time.LocalDateTime;

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
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plus(ttl);

        var existing = dataManager.findOneByField(AuthCaptcha.class,
                AuthCaptcha::getCaptchaKey, key);

        AuthCaptcha entity;
        if (existing.isPresent()) {
            entity = existing.get();
            entity.setCaptchaValue(value);
            entity.setExpiresAt(expiresAt);
        } else {
            entity = new AuthCaptcha();
            entity.setCaptchaKey(key);
            entity.setCaptchaValue(value);
            entity.setCaptchaType("IMAGE");
            entity.setExpiresAt(expiresAt);
        }
        dataManager.save(AuthCaptcha.class, entity);
        log.debug("Saved captcha: key={}, ttl={}", key, ttl);
    }

    @Override
    @Nullable
    public String get(@NonNull String key) {
        deleteExpired();

        return dataManager.findOne(AuthCaptcha.class,
                builder(AuthCaptcha.class)
                        .eq(AuthCaptcha::getCaptchaKey, key)
                        .gt(AuthCaptcha::getExpiresAt, LocalDateTime.now())
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
                        .gt(AuthCaptcha::getExpiresAt, LocalDateTime.now())
                        .build());
    }

    /**
     * 删除过期的验证码记录。
     *
     * @return 删除的记录数
     */
    public int deleteExpired() {
        LocalDateTime now = LocalDateTime.now();
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
