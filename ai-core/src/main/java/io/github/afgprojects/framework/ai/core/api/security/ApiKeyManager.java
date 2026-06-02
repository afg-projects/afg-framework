package io.github.afgprojects.framework.ai.core.api.security;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Set;

/**
 * API Key 管理器接口
 *
 * <p>用于管理 AI 服务的 API Key：
 * <ul>
 *   <li>安全存储和检索</li>
 *   <li>密钥轮换</li>
 *   <li>权限控制</li>
 *   <li>使用统计</li>
 * </ul>
 *
 * @author afg-projects
 * @since 1.0.0
 */
public interface ApiKeyManager {

    /**
     * 获取 API Key
     *
     * @param provider 提供商名称（如 "openai", "anthropic"）
     * @param context  上下文信息（如用户 ID、租户 ID）
     * @return API Key，如果不存在返回 null
     */
    @Nullable
    ApiKey getKey(@NonNull String provider, @NonNull ApiKeyContext context);

    /**
     * 获取 API Key（指定密钥名称）
     *
     * @param provider   提供商名称
     * @param keyName    密钥名称
     * @param context    上下文信息
     * @return API Key
     */
    @Nullable
    ApiKey getKey(@NonNull String provider, @NonNull String keyName, @NonNull ApiKeyContext context);

    /**
     * 存储 API Key
     *
     * @param provider 提供商名称
     * @param key      API Key
     * @param context  上下文信息
     */
    void storeKey(@NonNull String provider, @NonNull ApiKey key, @NonNull ApiKeyContext context);

    /**
     * 轮换 API Key
     *
     * @param provider 提供商名称
     * @param keyName  密钥名称
     * @param newKey   新的 API Key
     * @param context  上下文信息
     */
    void rotateKey(@NonNull String provider, @NonNull String keyName, @NonNull ApiKey newKey, @NonNull ApiKeyContext context);

    /**
     * 删除 API Key
     *
     * @param provider 提供商名称
     * @param keyName  密钥名称
     * @param context  上下文信息
     */
    void deleteKey(@NonNull String provider, @NonNull String keyName, @NonNull ApiKeyContext context);

    /**
     * 列出所有 API Key
     *
     * @param provider 提供商名称
     * @param context  上下文信息
     * @return API Key 列表
     */
    @NonNull
    Set<ApiKeyInfo> listKeys(@NonNull String provider, @NonNull ApiKeyContext context);

    /**
     * 验证 API Key 权限
     *
     * @param provider  提供商名称
     * @param keyName   密钥名称
     * @param operation 操作类型
     * @param context   上下文信息
     * @return 是否有权限
     */
    boolean hasPermission(@NonNull String provider, @NonNull String keyName, @NonNull String operation, @NonNull ApiKeyContext context);

    /**
     * 记录 API Key 使用
     *
     * @param provider 提供商名称
     * @param keyName  密钥名称
     * @param usage    使用信息
     */
    void recordUsage(@NonNull String provider, @NonNull String keyName, @NonNull ApiKeyUsage usage);

    /**
     * API Key 接口
     */
    interface ApiKey {

        /**
         * 获取密钥名称
         *
         * @return 密钥名称
         */
        @NonNull
        String getName();

        /**
         * 获取密钥值（解密后）
         *
         * @return 密钥值
         */
        @NonNull
        String getValue();

        /**
         * 获取提供商
         *
         * @return 提供商
         */
        @NonNull
        String getProvider();

        /**
         * 获取权限列表
         *
         * @return 权限列表
         */
        @NonNull
        Set<String> getPermissions();

        /**
         * 获取过期时间
         *
         * @return 过期时间（毫秒时间戳），如果永不过期返回 null
         */
        @Nullable
        Long getExpiresAt();

        /**
         * 获取元数据
         *
         * @return 元数据
         */
        @NonNull
        Map<String, String> getMetadata();

        /**
         * 是否已过期
         *
         * @return 是否已过期
         */
        boolean isExpired();

        /**
         * 是否已启用
         *
         * @return 是否已启用
         */
        boolean isEnabled();
    }

    /**
     * API Key 信息接口（不包含敏感值）
     */
    interface ApiKeyInfo {

        /**
         * 获取密钥名称
         *
         * @return 密钥名称
         */
        @NonNull
        String getName();

        /**
         * 获取提供商
         *
         * @return 提供商
         */
        @NonNull
        String getProvider();

        /**
         * 获取权限列表
         *
         * @return 权限列表
         */
        @NonNull
        Set<String> getPermissions();

        /**
         * 获取创建时间
         *
         * @return 创建时间（毫秒时间戳）
         */
        long getCreatedAt();

        /**
         * 获取过期时间
         *
         * @return 过期时间（毫秒时间戳），如果永不过期返回 null
         */
        @Nullable
        Long getExpiresAt();

        /**
         * 是否已启用
         *
         * @return 是否已启用
         */
        boolean isEnabled();

        /**
         * 获取最后使用时间
         *
         * @return 最后使用时间（毫秒时间戳），如果从未使用返回 null
         */
        @Nullable
        Long getLastUsedAt();
    }

    /**
     * API Key 上下文接口
     */
    interface ApiKeyContext {

        /**
         * 获取用户 ID
         *
         * @return 用户 ID
         */
        @Nullable
        String getUserId();

        /**
         * 获取租户 ID
         *
         * @return 租户 ID
         */
        @Nullable
        String getTenantId();

        /**
         * 获取应用程序 ID
         *
         * @return 应用程序 ID
         */
        @Nullable
        String getApplicationId();

        /**
         * 获取额外属性
         *
         * @return 额外属性
         */
        @NonNull
        Map<String, String> getAttributes();
    }

    /**
     * API Key 使用信息接口
     */
    interface ApiKeyUsage {

        /**
         * 获取操作类型
         *
         * @return 操作类型
         */
        @NonNull
        String getOperation();

        /**
         * 获取模型名称
         *
         * @return 模型名称
         */
        @Nullable
        String getModelName();

        /**
         * 获取输入 Token 数
         *
         * @return 输入 Token 数
         */
        long getInputTokens();

        /**
         * 获取输出 Token 数
         *
         * @return 输出 Token 数
         */
        long getOutputTokens();

        /**
         * 获取成本
         *
         * @return 成本（美元）
         */
        double getCost();

        /**
         * 获取时间戳
         *
         * @return 时间戳（毫秒）
         */
        long getTimestamp();
    }
}