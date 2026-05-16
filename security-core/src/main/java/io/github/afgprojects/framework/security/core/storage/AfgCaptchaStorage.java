package io.github.afgprojects.framework.security.core.storage;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Duration;

/**
 * 验证码存储接口。
 *
 * <p>定义验证码的存储、获取和删除操作。
 *
 * <p>实现类可以基于内存、Redis、数据库等存储介质。
 *
 * <p>典型使用场景：
 * <ul>
 *   <li>图形验证码存储</li>
 *   <li>短信验证码存储</li>
 *   <li>邮件验证码存储</li>
 * </ul>
 *
 * @since 1.0.0
 */
public interface AfgCaptchaStorage {

    /**
     * 保存验证码。
     *
     * <p>验证码将与指定的 TTL（生存时间）一起存储，超时后自动失效。
     *
     * @param key   验证码键，如会话ID、手机号、邮箱等，永不为 null
     * @param value 验证码值，永不为 null
     * @param ttl   生存时间，永不为 null
     */
    void save(@NonNull String key, @NonNull String value, @NonNull Duration ttl);

    /**
     * 获取验证码。
     *
     * <p>如果验证码不存在或已过期，返回 null。
     *
     * @param key 验证码键，永不为 null
     * @return 验证码值，如果不存在或已过期则返回 null
     */
    @Nullable
    String get(@NonNull String key);

    /**
     * 删除验证码。
     *
     * <p>验证通过后应调用此方法删除验证码，防止重复使用。
     *
     * @param key 验证码键，永不为 null
     */
    void delete(@NonNull String key);

    /**
     * 检查验证码是否存在。
     *
     * @param key 验证码键，永不为 null
     * @return 如果存在且未过期则返回 true
     */
    boolean exists(@NonNull String key);
}
