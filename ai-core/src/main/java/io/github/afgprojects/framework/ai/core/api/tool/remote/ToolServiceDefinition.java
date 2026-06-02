package io.github.afgprojects.framework.ai.core.api.tool.remote;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Set;

/**
 * 工具服务定义。
 *
 * <p>描述一个远程工具服务的完整信息，包括：
 * <ul>
 *   <li>工具定义（名称、描述、参数 Schema）</li>
 *   <li>服务端点信息</li>
 *   <li>安全配置</li>
 *   <li>元数据</li>
 * </ul>
 *
 * @param name              工具名称
 * @param description       工具描述
 * @param inputSchema       输入参数 JSON Schema
 * @param outputType        输出类型
 * @param endpoint          服务端点
 * @param requiredPermission 所需权限
 * @param requiredRoles     所需角色
 * @param sensitive         是否敏感操作
 * @param auditable         是否需要审计
 * @param timeoutMs         超时时间（毫秒）
 * @param retryCount        重试次数
 * @param metadata          元数据
 * @since 1.0.0
 */
public record ToolServiceDefinition(
    @NonNull String name,
    @NonNull String description,
    @NonNull String inputSchema,
    @NonNull String outputType,
    @NonNull ToolEndpoint endpoint,
    @Nullable String requiredPermission,
    @NonNull Set<String> requiredRoles,
    boolean sensitive,
    boolean auditable,
    long timeoutMs,
    int retryCount,
    @NonNull Map<String, String> metadata
) {

    /**
     * 创建 Builder。
     *
     * @return Builder 实例
     */
    public static @NonNull Builder builder() {
        return new Builder();
    }

    /**
     * 从基本信息创建定义。
     *
     * @param name        工具名称
     * @param description 工具描述
     * @param inputSchema 输入参数 Schema
     * @param endpoint    服务端点
     * @return 工具服务定义
     */
    public static @NonNull ToolServiceDefinition of(
            @NonNull String name,
            @NonNull String description,
            @NonNull String inputSchema,
            @NonNull ToolEndpoint endpoint) {
        return builder()
            .name(name)
            .description(description)
            .inputSchema(inputSchema)
            .endpoint(endpoint)
            .build();
    }

    /**
     * Builder 类。
     */
    public static class Builder {
        private String name;
        private String description;
        private String inputSchema = "{}";
        private String outputType = "java.lang.Object";
        private ToolEndpoint endpoint;
        private String requiredPermission;
        private Set<String> requiredRoles = Set.of();
        private boolean sensitive;
        private boolean auditable = true;
        private long timeoutMs = 30000;
        private int retryCount = 3;
        private Map<String, String> metadata = Map.of();

        public @NonNull Builder name(@NonNull String name) {
            this.name = name;
            return this;
        }

        public @NonNull Builder description(@NonNull String description) {
            this.description = description;
            return this;
        }

        public @NonNull Builder inputSchema(@NonNull String inputSchema) {
            this.inputSchema = inputSchema;
            return this;
        }

        public @NonNull Builder outputType(@NonNull String outputType) {
            this.outputType = outputType;
            return this;
        }

        public @NonNull Builder endpoint(@NonNull ToolEndpoint endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        public @NonNull Builder requiredPermission(@Nullable String requiredPermission) {
            this.requiredPermission = requiredPermission;
            return this;
        }

        public @NonNull Builder requiredRoles(@NonNull Set<String> requiredRoles) {
            this.requiredRoles = requiredRoles;
            return this;
        }

        public @NonNull Builder sensitive(boolean sensitive) {
            this.sensitive = sensitive;
            return this;
        }

        public @NonNull Builder auditable(boolean auditable) {
            this.auditable = auditable;
            return this;
        }

        public @NonNull Builder timeoutMs(long timeoutMs) {
            this.timeoutMs = timeoutMs;
            return this;
        }

        public @NonNull Builder retryCount(int retryCount) {
            this.retryCount = retryCount;
            return this;
        }

        public @NonNull Builder metadata(@NonNull Map<String, String> metadata) {
            this.metadata = metadata;
            return this;
        }

        public @NonNull Builder metadata(@NonNull String key, @NonNull String value) {
            this.metadata = new java.util.HashMap<>(this.metadata);
            this.metadata.put(key, value);
            return this;
        }

        public @NonNull ToolServiceDefinition build() {
            return new ToolServiceDefinition(
                name, description, inputSchema, outputType, endpoint,
                requiredPermission, requiredRoles, sensitive, auditable,
                timeoutMs, retryCount, metadata
            );
        }
    }
}