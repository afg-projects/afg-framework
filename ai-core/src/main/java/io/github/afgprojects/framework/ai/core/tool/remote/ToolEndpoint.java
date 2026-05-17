package io.github.afgprojects.framework.ai.core.tool.remote;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * 工具端点定义。
 *
 * <p>描述远程工具的网络端点信息。
 *
 * @param serviceId 服务 ID（用于服务发现）
 * @param path      API 路径（如 /api/tools/query_users）
 * @param method    HTTP 方法（默认 POST）
 * @param headers   额外请求头
 * @since 1.0.0
 */
public record ToolEndpoint(
    @NonNull String serviceId,
    @NonNull String path,
    @NonNull String method,
    @NonNull Map<String, String> headers
) {
    /**
     * 创建简单端点（POST 方法，无额外请求头）。
     *
     * @param serviceId 服务 ID
     * @param path      API 路径
     * @return 端点实例
     */
    public static @NonNull ToolEndpoint of(
            @NonNull String serviceId,
            @NonNull String path) {
        return new ToolEndpoint(serviceId, path, "POST", Map.of());
    }

    /**
     * 创建端点（指定方法）。
     *
     * @param serviceId 服务 ID
     * @param path      API 路径
     * @param method    HTTP 方法
     * @return 端点实例
     */
    public static @NonNull ToolEndpoint of(
            @NonNull String serviceId,
            @NonNull String path,
            @NonNull String method) {
        return new ToolEndpoint(serviceId, path, method, Map.of());
    }

    /**
     * 创建 Builder。
     *
     * @return Builder 实例
     */
    public static @NonNull Builder builder() {
        return new Builder();
    }

    /**
     * Builder 类。
     */
    public static class Builder {
        private String serviceId;
        private String path;
        private String method = "POST";
        private Map<String, String> headers = Map.of();

        public @NonNull Builder serviceId(@NonNull String serviceId) {
            this.serviceId = serviceId;
            return this;
        }

        public @NonNull Builder path(@NonNull String path) {
            this.path = path;
            return this;
        }

        public @NonNull Builder method(@NonNull String method) {
            this.method = method;
            return this;
        }

        public @NonNull Builder headers(@NonNull Map<String, String> headers) {
            this.headers = headers;
            return this;
        }

        public @NonNull Builder header(@NonNull String key, @NonNull String value) {
            this.headers = new java.util.HashMap<>(this.headers);
            this.headers.put(key, value);
            return this;
        }

        public @NonNull ToolEndpoint build() {
            return new ToolEndpoint(serviceId, path, method, headers);
        }
    }
}