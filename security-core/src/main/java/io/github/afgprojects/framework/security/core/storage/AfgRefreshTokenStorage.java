package io.github.afgprojects.framework.security.core.storage;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Optional;

/**
 * Refresh Token 存储接口。
 *
 * <p>定义 Refresh Token 的持久化存储操作。
 *
 * <p>实现类可以基于内存、Redis、数据库等存储介质。
 *
 * <p>典型使用场景：
 * <ul>
 *   <li>存储已签发的 Refresh Token</li>
 *   <li>验证 Refresh Token 有效性</li>
 *   <li>撤销 Refresh Token</li>
 *   <li>清理过期的 Refresh Token</li>
 * </ul>
 *
 * @since 1.0.0
 */
public interface AfgRefreshTokenStorage {

    /**
     * 保存 Refresh Token。
     *
     * @param tokenId    Token 唯一标识符（如 UUID），永不为 null
     * @param tokenHash  Token 的哈希值（通常使用 SHA-256），永不为 null
     * @param userId     用户 ID，永不为 null
     * @param tenantId   租户 ID，单租户场景可为 null
     * @param clientId   客户端 ID，可选
     * @param deviceId   设备 ID，可选
     * @param expiresAt  过期时间，永不为 null
     */
    void save(
            @NonNull String tokenId,
            @NonNull String tokenHash,
            @NonNull String userId,
            @Nullable String tenantId,
            @Nullable String clientId,
            @Nullable String deviceId,
            @NonNull Instant expiresAt
    );

    /**
     * 根据 Token 哈希值查找 Refresh Token。
     *
     * @param tokenHash Token 的哈希值，永不为 null
     * @return Refresh Token 信息，如果不存在则返回空
     */
    @NonNull
    Optional<RefreshTokenInfo> findByTokenHash(@NonNull String tokenHash);

    /**
     * 根据 Token ID 查找 Refresh Token。
     *
     * @param tokenId Token 唯一标识符，永不为 null
     * @return Refresh Token 信息，如果不存在则返回空
     */
    @NonNull
    Optional<RefreshTokenInfo> findByTokenId(@NonNull String tokenId);

    /**
     * 删除指定的 Refresh Token。
     *
     * <p>用于撤销单个 Refresh Token。
     *
     * @param tokenId Token 唯一标识符，永不为 null
     */
    void delete(@NonNull String tokenId);

    /**
     * 根据 Token 哈希值删除 Refresh Token。
     *
     * <p>用于 Token 刷新时撤销旧的 Refresh Token。
     *
     * @param tokenHash Token 的哈希值，永不为 null
     * @since 1.1.0
     */
    default void deleteByTokenHash(@NonNull String tokenHash) {
        // 默认不实现，子类可覆盖
    }

    /**
     * 删除用户的所有 Refresh Token。
     *
     * <p>用于强制用户下线或重置密码等场景。
     *
     * @param userId 用户 ID，永不为 null
     */
    void deleteByUserId(@NonNull String userId);

    /**
     * 清理过期的 Refresh Token。
     *
     * <p>建议定期执行此方法以清理过期数据。
     *
     * @return 被清理的 Token 数量
     */
    int deleteExpired();

    /**
     * Refresh Token 信息记录。
     *
     * @param tokenId   Token 唯一标识符
     * @param tokenHash Token 的哈希值
     * @param userId    用户 ID
     * @param tenantId  租户 ID
     * @param clientId  客户端 ID
     * @param deviceId  设备 ID
     * @param expiresAt 过期时间
     * @param createdAt 创建时间
     */
    record RefreshTokenInfo(
            @NonNull String tokenId,
            @NonNull String tokenHash,
            @NonNull String userId,
            @Nullable String tenantId,
            @Nullable String clientId,
            @Nullable String deviceId,
            @NonNull Instant expiresAt,
            @NonNull Instant createdAt
    ) {
    }
}
