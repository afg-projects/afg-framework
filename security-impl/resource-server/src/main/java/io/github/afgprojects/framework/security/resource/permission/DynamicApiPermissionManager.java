package io.github.afgprojects.framework.security.resource.permission;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.afgprojects.framework.core.api.config.ConfigChangeListener;
import io.github.afgprojects.framework.core.api.config.RemoteConfigClient;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.util.AntPathMatcher;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态接口权限配置管理器。
 *
 * <p>从治理中心获取接口权限配置，支持动态更新。
 *
 * <h3>配置格式</h3>
 * <p>配置 Key: afg.security.api-permissions
 * <p>配置 Value: JSON 格式的接口权限配置列表
 * <pre>
 * [
 *   {
 *     "pattern": "/api/admin/**",
 *     "requireAuth": true,
 *     "roles": ["ADMIN"],
 *     "roleLogical": "AND"
 *   },
 *   {
 *     "pattern": "/api/public/**",
 *     "requireAuth": false
 *   },
 *   {
 *     "pattern": "/api/users/**",
 *     "method": "POST",
 *     "requireAuth": true,
 *     "permissions": ["user:create"]
 *   }
 * ]
 * </pre>
 */
@Slf4j
public class DynamicApiPermissionManager implements ConfigChangeListener {

    private static final String CONFIG_KEY = "afg.security.api-permissions";

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<ApiPermissionConfig> configs = new ArrayList<>();
    private final Map<String, ApiPermissionConfig> configCache = new ConcurrentHashMap<>();

    @Nullable
    private final RemoteConfigClient remoteConfigClient;

    public DynamicApiPermissionManager(@Nullable RemoteConfigClient remoteConfigClient) {
        this.remoteConfigClient = remoteConfigClient;
        loadConfig();
        subscribeConfig();
    }

    /**
     * 获取接口的权限配置。
     *
     * @param path 接口路径
     * @param method HTTP 方法
     * @return 匹配的权限配置，如果没有匹配返回 null
     */
    @Nullable
    public ApiPermissionConfig getConfig(String path, String method) {
        // 先从缓存获取
        String cacheKey = path + ":" + method;
        ApiPermissionConfig cached = configCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // 查找匹配的配置
        ApiPermissionConfig matched = findMatchingConfig(path, method);
        if (matched != null) {
            configCache.put(cacheKey, matched);
        }
        return matched;
    }

    /**
     * 检查接口是否需要登录。
     */
    public boolean requireAuth(String path, String method) {
        ApiPermissionConfig config = getConfig(path, method);
        return config != null && config.isEnabled() && config.isRequireAuth();
    }

    /**
     * 检查接口是否需要权限校验。
     */
    public boolean requirePermission(String path, String method) {
        ApiPermissionConfig config = getConfig(path, method);
        return config != null && config.isEnabled() && config.isRequirePermission();
    }

    /**
     * 获取接口需要的角色。
     */
    @Nullable
    public Set<String> getRequiredRoles(String path, String method) {
        ApiPermissionConfig config = getConfig(path, method);
        return config != null ? config.getRoles() : null;
    }

    /**
     * 获取接口需要的权限。
     */
    @Nullable
    public Set<String> getRequiredPermissions(String path, String method) {
        ApiPermissionConfig config = getConfig(path, method);
        return config != null ? config.getPermissions() : null;
    }

    /**
     * 清除缓存。
     */
    public void clearCache() {
        configCache.clear();
    }

    /**
     * 刷新配置。
     */
    public void refresh() {
        loadConfig();
        clearCache();
    }

    @Override
    public void onChange(io.github.afgprojects.framework.core.api.config.ConfigChangeEvent event) {
        log.info("API permission config changed: key={}", event.key());
        loadConfig();
        clearCache();
    }

    private void loadConfig() {
        if (remoteConfigClient == null) {
            log.debug("No RemoteConfigClient, using default config");
            return;
        }

        Optional<String> configValue = remoteConfigClient.getConfig(CONFIG_KEY);
        if (configValue.isEmpty()) {
            log.debug("No API permission config found");
            return;
        }

        try {
            List<ApiPermissionConfig> newConfigs = parseConfigs(configValue.get());
            synchronized (configs) {
                configs.clear();
                configs.addAll(newConfigs);
                // 按优先级排序
                configs.sort(Comparator.comparingInt(ApiPermissionConfig::getPriority));
            }
            log.info("Loaded {} API permission configs", configs.size());
        } catch (Exception e) {
            log.error("Failed to parse API permission config: {}", e.getMessage());
        }
    }

    private void subscribeConfig() {
        if (remoteConfigClient != null) {
            remoteConfigClient.addListener(CONFIG_KEY, this);
            log.info("Subscribed to API permission config changes");
        }
    }

    private List<ApiPermissionConfig> parseConfigs(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<ApiPermissionConfig>>() {});
        } catch (Exception e) {
            log.error("Failed to parse API permission config JSON: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    @Nullable
    private ApiPermissionConfig findMatchingConfig(String path, String method) {
        synchronized (configs) {
            for (ApiPermissionConfig config : configs) {
                if (!config.isEnabled()) {
                    continue;
                }

                // 匹配路径
                if (!pathMatcher.match(config.getPattern(), path)) {
                    continue;
                }

                // 匹配方法（如果配置了方法）
                if (config.getMethod() != null && !config.getMethod().equalsIgnoreCase(method)) {
                    continue;
                }

                return config;
            }
        }
        return null;
    }
}