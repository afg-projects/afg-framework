package io.github.afgprojects.framework.security.core.oauth2;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import io.github.afgprojects.framework.security.core.oauth2.model.AuthorizationCode;

/**
 * 授权码存储接口。
 *
 * <p>提供授权码的持久化存储功能。
 *
 * @since 1.0.0
 */
public interface AuthorizationCodeStorage {

    /**
     * 保存授权码。
     *
     * @param authorizationCode 授权码，永不为 null
     */
    void save(@NonNull AuthorizationCode authorizationCode);

    /**
     * 根据授权码查找。
     *
     * @param code 授权码，永不为 null
     * @return 授权码信息，如果不存在则返回 null
     */
    @Nullable
    AuthorizationCode findByCode(@NonNull String code);

    /**
     * 删除授权码。
     *
     * <p>授权码使用后应立即删除，防止重放攻击。
     *
     * @param code 授权码，永不为 null
     */
    void delete(@NonNull String code);

    /**
     * 删除用户的所有授权码。
     *
     * @param userId 用户 ID，永不为 null
     */
    void deleteByUserId(@NonNull String userId);

    /**
     * 清理过期的授权码。
     *
     * @return 清理的授权码数量
     */
    int deleteExpired();
}