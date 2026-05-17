package io.github.afgprojects.framework.ai.agent.tool.remote;

import io.github.afgprojects.framework.ai.core.tool.*;
import io.github.afgprojects.framework.ai.core.tool.remote.ToolContextHeaders;
import io.github.afgprojects.framework.ai.core.tool.remote.ToolEndpoint;
import io.github.afgprojects.framework.ai.core.tool.remote.ToolServiceDefinition;
import io.github.afgprojects.framework.core.api.registry.ServiceDiscovery;
import io.github.afgprojects.framework.core.api.registry.ServiceInstance;
import io.github.afgprojects.framework.data.core.scope.DataScope;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 远程工具实现。
 *
 * <p>实现 {@link SecureTool} 接口，通过 HTTP 调用远程服务执行工具。
 *
 * <p>特性：
 * <ul>
 *   <li>自动从服务发现获取实例</li>
 *   <li>透传安全上下文</li>
 *   <li>支持超时和重试</li>
 *   <li>支持负载均衡</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class RemoteTool implements SecureTool<Map<String, Object>, Object> {

    private static final Logger log = LoggerFactory.getLogger(RemoteTool.class);

    private final ToolServiceDefinition definition;
    private final ServiceDiscovery serviceDiscovery;
    private final HttpClient httpClient;

    /**
     * 创建远程工具。
     *
     * @param definition       工具服务定义
     * @param serviceDiscovery 服务发现客户端
     */
    public RemoteTool(
            @NonNull ToolServiceDefinition definition,
            @NonNull ServiceDiscovery serviceDiscovery) {
        this.definition = definition;
        this.serviceDiscovery = serviceDiscovery;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    @Override
    public @NonNull String name() {
        return definition.name();
    }

    @Override
    public @NonNull String description() {
        return definition.description();
    }

    @Override
    public @NonNull String inputSchema() {
        return definition.inputSchema();
    }

    @Override
    public @Nullable String requiredPermission() {
        return definition.requiredPermission();
    }

    @Override
    public @NonNull Set<String> requiredRoles() {
        return definition.requiredRoles();
    }

    @Override
    public boolean isSensitive() {
        return definition.sensitive();
    }

    @Override
    public boolean isAuditable() {
        return definition.auditable();
    }

    @Override
    public @Nullable DataScope getDataScope(@NonNull ToolContext context) {
        // 远程工具的数据权限由远程服务处理
        return null;
    }

    @Override
    public Object execute(Map<String, Object> input, @NonNull ToolContext context) {
        // 1. 从服务发现获取实例
        ServiceInstance instance = getServiceInstance();
        if (instance == null) {
            throw new ToolExecutionException(
                "Service not available: " + definition.endpoint().serviceId());
        }

        // 2. 执行远程调用（带重试）
        Exception lastException = null;
        int retryCount = definition.retryCount();

        for (int i = 0; i <= retryCount; i++) {
            try {
                return executeRemoteCall(instance, input, context);
            } catch (Exception e) {
                lastException = e;
                log.warn("Remote tool {} execution failed (attempt {}/{}): {}",
                    name(), i + 1, retryCount + 1, e.getMessage());

                if (i < retryCount) {
                    // 获取新实例重试
                    instance = getServiceInstance();
                    if (instance == null) {
                        break;
                    }
                }
            }
        }

        throw new ToolExecutionException(
            "Remote tool execution failed after " + (retryCount + 1) + " attempts: " + name(),
            lastException);
    }

    /**
     * 获取服务实例。
     */
    private @Nullable ServiceInstance getServiceInstance() {
        Optional<ServiceInstance> instance = serviceDiscovery.getInstance(
            definition.endpoint().serviceId());
        return instance.orElse(null);
    }

    /**
     * 执行远程调用。
     */
    private Object executeRemoteCall(
            @NonNull ServiceInstance instance,
            @NonNull Map<String, Object> input,
            @NonNull ToolContext context) throws IOException, InterruptedException {

        // 构建请求 URL
        URI uri = instance.getUri().resolve(definition.endpoint().path());

        // 构建请求体
        String requestBody = serializeInput(input);

        // 构建请求
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(uri)
            .timeout(Duration.ofMillis(definition.timeoutMs()))
            .header("Content-Type", "application/json");

        // 添加额外请求头
        for (Map.Entry<String, String> header : definition.endpoint().headers().entrySet()) {
            requestBuilder.header(header.getKey(), header.getValue());
        }

        // 透传认证请求头（安全方式）
        Map<String, String> authHeaders = ToolContextHeaders.propagateAuthHeaders(context.getOriginalHeaders());
        for (Map.Entry<String, String> header : authHeaders.entrySet()) {
            requestBuilder.header(header.getKey(), header.getValue());
        }

        // 添加辅助信息请求头
        Map<String, String> auxiliaryHeaders = ToolContextHeaders.writeToHeaders(context);
        for (Map.Entry<String, String> header : auxiliaryHeaders.entrySet()) {
            requestBuilder.header(header.getKey(), header.getValue());
        }

        // 设置请求方法和请求体
        String method = definition.endpoint().method().toUpperCase();
        if ("GET".equals(method)) {
            requestBuilder.GET();
        } else if ("POST".equals(method)) {
            requestBuilder.POST(HttpRequest.BodyPublishers.ofString(requestBody));
        } else if ("PUT".equals(method)) {
            requestBuilder.PUT(HttpRequest.BodyPublishers.ofString(requestBody));
        } else if ("DELETE".equals(method)) {
            requestBuilder.DELETE();
        } else {
            requestBuilder.method(method, HttpRequest.BodyPublishers.ofString(requestBody));
        }

        HttpRequest request = requestBuilder.build();

        // 发送请求
        log.debug("Calling remote tool {} at {}", name(), uri);
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // 检查响应
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return parseResponse(response.body());
        } else {
            throw new ToolExecutionException(
                "Remote tool returned error: " + response.statusCode() + " - " + response.body());
        }
    }

    /**
     * 序列化输入参数。
     */
    private String serializeInput(Map<String, Object> input) {
        // 简单实现，实际应使用 Jackson 或 Gson
        if (input == null || input.isEmpty()) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : input.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(entry.getKey()).append("\":");
            Object value = entry.getValue();
            if (value instanceof String) {
                sb.append("\"").append(value).append("\"");
            } else if (value == null) {
                sb.append("null");
            } else {
                sb.append(value);
            }
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * 解析响应。
     */
    private Object parseResponse(String body) {
        // 简单实现，返回原始字符串
        // 实际应使用 Jackson 或 Gson 解析 JSON
        return body;
    }

    /**
     * 获取工具服务定义。
     *
     * @return 工具服务定义
     */
    public @NonNull ToolServiceDefinition getDefinition() {
        return definition;
    }

    @Override
    public String toString() {
        return "RemoteTool{" +
            "name='" + name() + '\'' +
            ", serviceId='" + definition.endpoint().serviceId() + '\'' +
            ", path='" + definition.endpoint().path() + '\'' +
            '}';
    }
}