package io.github.afgprojects.framework.ai.agent.tool.remote;

import io.github.afgprojects.framework.ai.core.api.tool.remote.ToolDiscoveryClient;
import io.github.afgprojects.framework.ai.core.api.tool.remote.ToolEndpoint;
import io.github.afgprojects.framework.ai.core.api.tool.remote.ToolServiceDefinition;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 静态配置工具发现客户端。
 *
 * <p>从配置文件静态定义远程工具，不依赖注册中心。
 *
 * <p>配置示例（application.yml）：
 * <pre>{@code
 * afg:
 *   ai:
 *     tool:
 *       discovery:
 *         static:
 *           enabled: true
 *           tools:
 *             - name: query_users
 *               description: 查询用户列表
 *               endpoint:
 *                 serviceId: user-service
 *                 path: /api/tools/query_users
 *               permission: user:read
 *               timeoutMs: 30000
 * }</pre>
 *
 * @since 1.0.0
 */
public class StaticToolDiscoveryClient implements ToolDiscoveryClient {

    private static final Logger log = LoggerFactory.getLogger(StaticToolDiscoveryClient.class);

    private final List<ToolServiceDefinition> tools;
    private final List<ToolChangeListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * 创建静态工具发现客户端。
     *
     * @param tools 工具定义列表
     */
    public StaticToolDiscoveryClient(@NonNull List<ToolServiceDefinition> tools) {
        this.tools = new ArrayList<>(tools);
        log.info("Static tool discovery client initialized with {} tools", tools.size());
    }

    /**
     * 从配置 Map 创建工具发现客户端。
     *
     * @param toolConfigs 工具配置列表
     * @return 工具发现客户端
     */
    public static @NonNull StaticToolDiscoveryClient fromConfigs(
            @NonNull List<Map<String, Object>> toolConfigs) {
        List<ToolServiceDefinition> tools = new ArrayList<>();

        for (Map<String, Object> config : toolConfigs) {
            ToolServiceDefinition definition = parseConfig(config);
            tools.add(definition);
        }

        return new StaticToolDiscoveryClient(tools);
    }

    /**
     * 解析配置。
     */
    private static ToolServiceDefinition parseConfig(Map<String, Object> config) {
        String name = (String) config.get("name");
        String description = (String) config.getOrDefault("description", "");
        String inputSchema = (String) config.getOrDefault("inputSchema", "{}");
        String permission = (String) config.get("permission");

        // 解析端点
        Map<String, Object> endpointConfig = (Map<String, Object>) config.get("endpoint");
        ToolEndpoint endpoint = parseEndpoint(endpointConfig);

        // 解析其他配置
        long timeoutMs = config.containsKey("timeoutMs")
            ? ((Number) config.get("timeoutMs")).longValue()
            : 30000;
        int retryCount = config.containsKey("retryCount")
            ? ((Number) config.get("retryCount")).intValue()
            : 3;
        boolean sensitive = Boolean.TRUE.equals(config.get("sensitive"));
        boolean auditable = !Boolean.FALSE.equals(config.get("auditable"));

        return ToolServiceDefinition.builder()
            .name(name)
            .description(description)
            .inputSchema(inputSchema)
            .endpoint(endpoint)
            .requiredPermission(permission)
            .timeoutMs(timeoutMs)
            .retryCount(retryCount)
            .sensitive(sensitive)
            .auditable(auditable)
            .build();
    }

    /**
     * 解析端点配置。
     */
    private static ToolEndpoint parseEndpoint(Map<String, Object> endpointConfig) {
        if (endpointConfig == null) {
            throw new IllegalArgumentException("Endpoint config is required");
        }

        String serviceId = (String) endpointConfig.get("serviceId");
        String path = (String) endpointConfig.get("path");
        String method = (String) endpointConfig.getOrDefault("method", "POST");

        return ToolEndpoint.builder()
            .serviceId(serviceId)
            .path(path)
            .method(method)
            .build();
    }

    @Override
    public @NonNull List<ToolServiceDefinition> discoverTools() {
        return new ArrayList<>(tools);
    }

    @Override
    public @NonNull List<ToolServiceDefinition> discoverTools(@NonNull String serviceId) {
        return tools.stream()
            .filter(t -> t.endpoint().serviceId().equals(serviceId))
            .toList();
    }

    @Override
    public void refresh() {
        // 静态配置不支持刷新
        log.debug("Static tool discovery does not support refresh");
    }

    @Override
    public void addToolChangeListener(@NonNull ToolChangeListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeToolChangeListener(@NonNull ToolChangeListener listener) {
        listeners.remove(listener);
    }

    @Override
    public @NonNull String getClientName() {
        return "static";
    }

    /**
     * 添加工具（动态）。
     *
     * @param definition 工具定义
     */
    public void addTool(@NonNull ToolServiceDefinition definition) {
        tools.add(definition);
        notifyListeners();
        log.info("Added static tool: {}", definition.name());
    }

    /**
     * 移除工具。
     *
     * @param name 工具名称
     */
    public void removeTool(@NonNull String name) {
        tools.removeIf(t -> t.name().equals(name));
        notifyListeners();
        log.info("Removed static tool: {}", name);
    }

    /**
     * 通知监听器。
     */
    private void notifyListeners() {
        List<ToolServiceDefinition> currentTools = discoverTools();
        for (ToolChangeListener listener : listeners) {
            listener.onToolsChanged(currentTools);
        }
    }
}