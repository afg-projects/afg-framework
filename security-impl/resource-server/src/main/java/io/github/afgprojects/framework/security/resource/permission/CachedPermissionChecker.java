package io.github.afgprojects.framework.security.resource.permission;

import io.github.afgprojects.framework.core.cache.AfgCache;
import io.github.afgprojects.framework.core.cache.CacheManager;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 带缓存的权限校验器。
 *
 * <p>优先从本地缓存获取权限，缓存未命中时调用远程客户端。
 * 使用 core 模块的统一缓存接口。
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

    private static final String PERMISSION_CACHE_NAME = "security:permission:check";
    private static final String USER_PERMISSIONS_CACHE_NAME = "security:permission:user";
    private static final long CACHE_TTL_MS = 5 * 60 * 1000; // 5 分钟

    private final RemotePermissionClient remoteClient;
    private final JwtPermissionChecker jwtChecker;
    private final AfgCache<Boolean> permissionCache;
    private final AfgCache<Set<String>> userPermissionsCache;

    public CachedPermissionChecker(
            @Nullable RemotePermissionClient remoteClient,
            @NonNull JwtPermissionChecker jwtChecker,
            @Nullable CacheManager cacheManager) {
        this.remoteClient = remoteClient;
        this.jwtChecker = jwtChecker;

        // 使用统一缓存接口
        if (cacheManager != null) {
            this.permissionCache = cacheManager.getCache(PERMISSION_CACHE_NAME);
            this.userPermissionsCache = cacheManager.getCache(USER_PERMISSIONS_CACHE_NAME);
        } else {
            // 如果没有 CacheManager，使用空缓存实现
            this.permissionCache = new NoOpCache<>(PERMISSION_CACHE_NAME);
            this.userPermissionsCache = new NoOpCache<>(USER_PERMISSIONS_CACHE_NAME);
            log.warn("No CacheManager available, permission caching is disabled");
        }
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

        // 3. 从缓存获取
        String cacheKey = buildPermissionKey(userId, permission, tenantId);
        Boolean cached = permissionCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // 4. 远程调用
        boolean result = remoteClient.hasPermission(userId, permission, tenantId);
        permissionCache.put(cacheKey, result, CACHE_TTL_MS);

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

        // 3. 从缓存获取
        String cacheKey = buildUserKey(userId, tenantId);
        Set<String> cached = userPermissionsCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // 4. 远程调用
        Set<String> permissions = remoteClient.getPermissions(userId, tenantId);
        userPermissionsCache.put(cacheKey, permissions, CACHE_TTL_MS);

        return permissions;
    }

    /**
     * 清除用户的权限缓存。
     */
    public void clearCache(@NonNull String userId, @Nullable String tenantId) {
        String userKey = buildUserKey(userId, tenantId);
        userPermissionsCache.evict(userKey);

        // 清除该用户的所有权限缓存（需要遍历）
        for (String key : permissionCache.keys()) {
            if (key.startsWith(userId + ":")) {
                permissionCache.evict(key);
            }
        }
    }

    private String buildPermissionKey(String userId, String permission, String tenantId) {
        return userId + ":" + permission + ":" + (tenantId != null ? tenantId : "");
    }

    private String buildUserKey(String userId, String tenantId) {
        return userId + ":" + (tenantId != null ? tenantId : "");
    }

    /**
     * 空缓存实现（当没有 CacheManager 时使用）。
     */
    private static class NoOpCache<V> implements AfgCache<V> {
        private final String name;

        NoOpCache(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public V get(String key) {
            return null;
        }

        @Override
        public void put(String key, V value) {
        }

        @Override
        public void put(String key, V value, long ttlMillis) {
        }

        @Override
        public void evict(String key) {
        }

        @Override
        public void clear() {
        }

        @Override
        public V putIfAbsent(String key, V value, long ttlMillis) {
            return null;
        }

        @Override
        public boolean containsKey(String key) {
            return false;
        }

        @Override
        public long size() {
            return 0;
        }
    }
}