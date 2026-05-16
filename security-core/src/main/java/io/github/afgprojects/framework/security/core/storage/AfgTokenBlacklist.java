package io.github.afgprojects.framework.security.core.storage;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Duration;

/**
 * Token 黑名单接口。
 *
 * <p>定义 Token 黑名单的添加、查询和批量操作。
 *
 * <p>实现类可以基于内存、Redis、数据库等存储介质。
 *
 * <p>典型使用场景：
 * <ul>
 *   <li>用户登出时将 Token 加入黑名单</li>
 *   <li>Token 被盗用时将其加入黑名单</li>
 *   <li>强制用户下线时将其所有 Token 加入黑名单</li>
 * </ul>
 *
 * @since 1.0.0
 */
public interface AfgTokenBlacklist {

    /**
     * 将 Token 加入黑名单。
     *
     * <p>Token 将与指定的 TTL（生存时间）一起存储，建议 TTL 设置为 Token 的剩余有效期。
     *
     * @param tokenHash Token 的哈希值（通常使用 SHA-256），永不为 null
     * @param userId    用户 ID，永不为 null
     * @param reason    加入黑名单的原因，如 "logout"、"revoked"、"security_breach" 等，永不为 null
     * @param ttl       生存时间，永不为 null
     */
    void addToBlacklist(@NonNull String tokenHash, @NonNull String userId, @NonNull String reason, @NonNull Duration ttl);

    /**
     * 检查 Token 是否在黑名单中。
     *
     * @param tokenHash Token 的哈希值，永不为 null
     * @return 如果在黑名单中则返回 true
     */
    boolean isBlacklisted(@NonNull String tokenHash);

    /**
     * 将用户的所有 Token 加入黑名单。
     *
     * <p>用于强制用户下线或重置密码等场景。
     *
     * <p>实现类可以通过维护用户 Token 列表或使用版本号机制来实现此功能。
     *
     * @param userId 用户 ID，永不为 null
     * @param ttl    生存时间，永不为 null
     */
    void blacklistAllUserTokens(@NonNull String userId, @NonNull Duration ttl);
}
