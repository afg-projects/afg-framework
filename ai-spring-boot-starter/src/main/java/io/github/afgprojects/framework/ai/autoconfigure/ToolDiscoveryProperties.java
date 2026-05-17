package io.github.afgprojects.framework.ai.autoconfigure;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 工具发现配置属性。
 *
 * <p>配置示例：
 * <pre>{@code
 * afg:
 *   ai:
 *     tool:
 *       discovery:
 *         enabled: true
 *         refresh-interval: 30s
 *         static:
 *           enabled: true
 *           tools:
 *             - name: query_users
 *               description: 查询用户列表
 *               inputSchema: '{"type":"object","properties":{"status":{"type":"integer"}}}'
 *               endpoint:
 *                 serviceId: user-service
 *                 path: /api/tools/query_users
 *                 method: POST
 *               permission: user:read
 *               timeoutMs: 30000
 *               retryCount: 3
 *               sensitive: false
 *               auditable: true
 * }</pre>
 *
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "afg.ai.tool.discovery")
public class ToolDiscoveryProperties {

    /**
     * 是否启用工具发现。
     */
    private boolean enabled = false;

    /**
     * 刷新间隔（支持 Duration 格式，如 30s, 1m）。
     */
    private String refreshInterval = "30s";

    /**
     * 静态工具配置。
     */
    @NestedConfigurationProperty
    private StaticConfig staticConfig = new StaticConfig();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getRefreshInterval() {
        return refreshInterval;
    }

    public void setRefreshInterval(String refreshInterval) {
        this.refreshInterval = refreshInterval;
    }

    public StaticConfig getStatic() {
        return staticConfig;
    }

    public void setStatic(StaticConfig staticConfig) {
        this.staticConfig = staticConfig;
    }

    /**
     * 静态工具配置。
     */
    public static class StaticConfig {

        /**
         * 是否启用静态工具发现。
         */
        private boolean enabled = false;

        /**
         * 工具配置列表。
         */
        private List<ToolConfig> tools = new ArrayList<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public List<ToolConfig> getTools() {
            return tools;
        }

        public void setTools(List<ToolConfig> tools) {
            this.tools = tools;
        }
    }

    /**
     * 工具配置。
     */
    public static class ToolConfig {

        /**
         * 工具名称（唯一标识）。
         */
        private String name;

        /**
         * 工具描述。
         */
        private @Nullable String description;

        /**
         * 输入参数 JSON Schema。
         */
        private @Nullable String inputSchema;

        /**
         * 端点配置。
         */
        private EndpointConfig endpoint;

        /**
         * 所需权限标识。
         */
        private @Nullable String permission;

        /**
         * 超时时间（毫秒）。
         */
        private @Nullable Long timeoutMs;

        /**
         * 重试次数。
         */
        private @Nullable Integer retryCount;

        /**
         * 是否敏感操作。
         */
        private @Nullable Boolean sensitive;

        /**
         * 是否审计。
         */
        private @Nullable Boolean auditable;

        /**
         * 额外元数据。
         */
        private @Nullable Map<String, String> metadata;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public @Nullable String getDescription() {
            return description;
        }

        public void setDescription(@Nullable String description) {
            this.description = description;
        }

        public @Nullable String getInputSchema() {
            return inputSchema;
        }

        public void setInputSchema(@Nullable String inputSchema) {
            this.inputSchema = inputSchema;
        }

        public EndpointConfig getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(EndpointConfig endpoint) {
            this.endpoint = endpoint;
        }

        public @Nullable String getPermission() {
            return permission;
        }

        public void setPermission(@Nullable String permission) {
            this.permission = permission;
        }

        public @Nullable Long getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(@Nullable Long timeoutMs) {
            this.timeoutMs = timeoutMs;
        }

        public @Nullable Integer getRetryCount() {
            return retryCount;
        }

        public void setRetryCount(@Nullable Integer retryCount) {
            this.retryCount = retryCount;
        }

        public @Nullable Boolean isSensitive() {
            return sensitive;
        }

        public void setSensitive(@Nullable Boolean sensitive) {
            this.sensitive = sensitive;
        }

        public @Nullable Boolean isAuditable() {
            return auditable;
        }

        public void setAuditable(@Nullable Boolean auditable) {
            this.auditable = auditable;
        }

        public @Nullable Map<String, String> getMetadata() {
            return metadata;
        }

        public void setMetadata(@Nullable Map<String, String> metadata) {
            this.metadata = metadata;
        }
    }

    /**
     * 端点配置。
     */
    public static class EndpointConfig {

        /**
         * 服务 ID（从服务发现查找）。
         */
        private String serviceId;

        /**
         * API 路径。
         */
        private String path;

        /**
         * HTTP 方法。
         */
        private @Nullable String method;

        /**
         * 额外请求头。
         */
        private @Nullable Map<String, String> headers;

        public String getServiceId() {
            return serviceId;
        }

        public void setServiceId(String serviceId) {
            this.serviceId = serviceId;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public @Nullable String getMethod() {
            return method;
        }

        public void setMethod(@Nullable String method) {
            this.method = method;
        }

        public @Nullable Map<String, String> getHeaders() {
            return headers;
        }

        public void setHeaders(@Nullable Map<String, String> headers) {
            this.headers = headers;
        }
    }
}
