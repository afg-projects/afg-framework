package io.github.afgprojects.framework.security.core.storage;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Duration;

/**
 * NoOp 验证码存储降级实现。
 * <p>
 * 存储操作为空操作，查询返回空/不存在。
 *
 * @since 1.0.0
 */
public class NoOpCaptchaStorage implements AfgCaptchaStorage {

    @Override
    public void save(@NonNull String key, @NonNull String value, @NonNull Duration ttl) {
        // no-op
    }

    @Override
    public @Nullable String get(@NonNull String key) {
        return null;
    }

    @Override
    public void delete(@NonNull String key) {
        // no-op
    }

    @Override
    public boolean exists(@NonNull String key) {
        return false;
    }
}
