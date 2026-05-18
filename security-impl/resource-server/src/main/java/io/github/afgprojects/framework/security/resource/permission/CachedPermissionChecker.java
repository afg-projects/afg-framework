package io.github.afgprojects.framework.security.resource.permission;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 带缓存的权限校验器。
 *
 * <p>优先从本地缓存获取权限，缓存未命中时调用远程客户端。
 * 适用于资源服务器场景，减少对认证服务器的调用。
 *
 * <h3>缓存策略</h3>
 * <ul>
 *   <li>权限结果缓存 5 分钟</li>
 *   <li>用户权限列表缓存 5 分钟</li>
 *   <li>角色校验从 JWT Token 本地获取（不缓存）</li>
 * </ul>
 */
@Component
@Slf4j
public class CachedPermissionChecker {

    private final RemotePermissionClient remoteClient;
    private final JwtPermissionChecker jwtChecker;

    private final Cache<String, Boolean> permissionCache;
    private final Cache<String, Set<String>> userPermissionsCache;

    public CachedPermissionChecker(
            @Nullable RemotePermissionClient remoteClient,
            @NonNull JwtPermissionChecker jwtChecker) {
        this.remoteClient = remoteClient;
        this.jwtChecker = jwtChecker;

        // 权限缓存：key = "userId:permission:tenantId"
        this.permissionCache = Caffeine.newBuilder()
                .maximumSize(10000)
                .expireAfterWrite(Duration.ofMinutes(5))
                .build();

        // 用户权限列表缓存
        this.userPermissionsCache = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(Duration.ofMinutes(5))
                .build();
    }

    /**
     * 检查当前用户是否具有指定角色。
     * 直接从 JWT Token 中获取，无需远程调用。
     */
    public boolean hasRole(@NonNull String role) {
        return jwtChecker.hasRole(role);
    }

    /**
     * 检查当前用户是否具有指定权限。
     * 优先从缓存获取，缓存未命中时调用远程客户端。
     */
    public boolean hasPermission(@NonNull String permission) {
        String userId = jwtChecker.getUserId();
        String tenantId = jwtChecker.getTenantId();

        if (userId == null) {
            return false;
        }

        // 1. 先尝试从 JWT Token 中获取（如果 Token 中包含权限）
        if (jwtChecker.hasPermission(permission)) {
            return true;
        }

        // 2. 如果没有远程客户端，返回 false
        if (remoteClient == null) {
            log.debug("No remote client configured, permission check failed for: {}", permission);
            return false;
        }

        // 3. 从缓存或远程获取
        String cacheKey = buildPermissionKey(userId, permission, tenantId);
        Boolean cached = permissionCache.getIfPresent(cacheKey);
        if (cached != null) {
            return cached;
        }

        // 4. 远程调用
        boolean result = remoteClient.hasPermission(userId, permission, tenantId);
        permissionCache.put(cacheKey, result);

        return result;
    }

    /**
     * 获取当前用户的所有权限。
     */
    @NonNull
    public Set<String> getPermissions() {
        String userId = jwtChecker.getUserId();
        String tenantId = jwtChecker.getTenantId();

        if (userId == null) {
            return Set.of();
        }

        // 1. 先尝试从 JWT Token 获取
        Set<String> tokenPermissions = jwtChecker.getPermissions();
        if (!tokenPermissions.isEmpty()) {
            return tokenPermissions;
        }

        // 2. 如果没有远程客户端，返回空
        if (remoteClient == null) {
            return Set.of();
        }

        // 3. 从缓存或远程获取
        String cacheKey = buildUserKey(userId, tenantId);
        Set<String> cached = userPermissionsCache.getIfPresent(cacheKey);
        if (cached != null) {
            return cached;
        }

        // 4. 远程调用
        Set<String> permissions = remoteClient.getPermissions(userId, tenantId);
        userPermissionsCache.put(cacheKey, permissions);

        return permissions;
    }

    /**
     * 清除用户的权限缓存。
     */
    public void clearCache(@NonNull String userId, @Nullable String tenantId) {
        String userKey = buildUserKey(userId, tenantId);
        userPermissionsCache.invalidate(userKey);

        // 清除该用户的所有权限缓存（需要遍历）
        permissionCache.asMap().keySet().removeIf(key -> key.startsWith(userId + ":"));
    }

    private String buildPermissionKey(String userId, String permission, String tenantId) {
        return userId + ":" + permission + ":" + (tenantId != null ? tenantId : "");
    }

    private String buildUserKey(String userId, String tenantId) {
        return userId + ":" + (tenantId != null ? tenantId : "");
    }
}