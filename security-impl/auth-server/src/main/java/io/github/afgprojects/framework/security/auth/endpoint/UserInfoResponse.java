package io.github.afgprojects.framework.security.auth.endpoint;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * 用户信息响应。
 *
 * @param userId 用户 ID
 * @param username 用户名
 * @param nickname 昵称
 * @param avatar 头像 URL
 * @param roles 角色集合
 * @param permissions 权限集合
 * @param tenantId 租户 ID
 * @since 1.0.0
 */
public record UserInfoResponse(
        @NonNull String userId,
        @NonNull String username,
        @Nullable String nickname,
        @Nullable String avatar,
        @NonNull Set<String> roles,
        @NonNull Set<String> permissions,
        @Nullable String tenantId) {
}
