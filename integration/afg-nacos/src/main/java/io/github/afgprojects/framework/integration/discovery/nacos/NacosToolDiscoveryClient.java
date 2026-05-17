package io.github.afgprojects.framework.integration.discovery.nacos;

import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import io.github.afgprojects.framework.ai.core.tool.remote.ToolDiscoveryClient;
import io.github.afgprojects.framework.ai.core.tool.remote.ToolEndpoint;
import io.github.afgprojects.framework.ai.core.tool.remote.ToolServiceDefinition;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Nacos 工具发现客户端。
 *
 * <p>从 Nacos 服务元数据动态发现远程工具定义。
 * 工具定义存储在服务实例的元数据中，键为 "ai-tools"。
 *
 * <p>元数据格式（JSON）：
 * <pre>{@code
 * {
 *   "tools": [
 *     {
 *       "name": "query_users",
 *       "description": "查询用户列表",
 *       "inputSchema": "{\"type\":\"object\"}",
 *       "path": "/api/tools/query_users",
 *       "method": "POST",
 *       "permission": "user:read",
 *       "timeoutMs": 30000,
 *       "retryCount": 3,
 *       "sensitive": false,
 *       "auditable": true
 *     }
 *   ]
 * }
 * }</pre>
 *
 * <p>使用示例：
 * <pre>{@code
 * NamingService namingService = ...;
 * ToolDiscoveryClient discoveryClient = new NacosToolDiscoveryClient(namingService);
 * List<ToolServiceDefinition> tools = discoveryClient.discoverTools();
 * }</pre>
 *
 * @since 1.0.0
 */
public class NacosToolDiscoveryClient implements ToolDiscoveryClient {

    private static final Logger log = LoggerFactory.getLogger(NacosToolDiscoveryClient.class);

    /**
     * 元数据中的工具定义键。
     */
    private static final String TOOL_METADATA_KEY = "ai-tools";

    private final NamingService namingService;
    private final List<ToolChangeListener> listeners = new CopyOnWriteArrayList<>();
    private volatile List<ToolServiceDefinition> cachedTools = new ArrayList<>();

    /**
     * 创建 Nacos 工具发现客户端。
     *
     * @param namingService Nacos NamingService
     */
    public NacosToolDiscoveryClient(@NonNull NamingService namingService) {
        this.namingService = namingService;
        log.info("Nacos tool discovery client initialized");
    }

    @Override
    public @NonNull List<ToolServiceDefinition> discoverTools() {
        List<ToolServiceDefinition> tools = new ArrayList<>();

        try {
            // 获取所有服务名称
            List<String> serviceNames = namingService.getServicesOfServer(1, 100).getData();

            for (String serviceName : serviceNames) {
                try {
                    // 获取服务实例
                    List<Instance> instances = namingService.getAllInstances(serviceName);

                    for (Instance instance : instances) {
                        // 从实例元数据提取工具定义
                        List<ToolServiceDefinition> instanceTools = extractToolsFromInstance(instance);
                        tools.addAll(instanceTools);
                    }
                } catch (Exception e) {
                    log.warn("Failed to discover tools from service {}: {}", serviceName, e.getMessage());
                }
            }

            cachedTools = new ArrayList<>(tools);
            log.info("Discovered {} tools from Nacos", tools.size());
        } catch (Exception e) {
            log.error("Failed to discover tools from Nacos: {}", e.getMessage());
            // 返回缓存的工具
            return new ArrayList<>(cachedTools);
        }

        return tools;
    }

    @Override
    public @NonNull List<ToolServiceDefinition> discoverTools(@NonNull String serviceId) {
        List<ToolServiceDefinition> tools = new ArrayList<>();

        try {
            List<Instance> instances = namingService.getAllInstances(serviceId);

            for (Instance instance : instances) {
                List<ToolServiceDefinition> instanceTools = extractToolsFromInstance(instance);
                tools.addAll(instanceTools);
            }

            log.debug("Discovered {} tools from service {}", tools.size(), serviceId);
        } catch (Exception e) {
            log.warn("Failed to discover tools from service {}: {}", serviceId, e.getMessage());
        }

        return tools;
    }

    @Override
    public void refresh() {
        log.info("Refreshing tools from Nacos...");
        List<ToolServiceDefinition> oldTools = cachedTools;
        List<ToolServiceDefinition> newTools = discoverTools();

        // 通知监听器
        if (!oldTools.equals(newTools)) {
            notifyListeners(newTools);
        }
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
        return "nacos";
    }

    /**
     * 从实例元数据提取工具定义。
     */
    private List<ToolServiceDefinition> extractToolsFromInstance(Instance instance) {
        Map<String, String> metadata = instance.getMetadata();
        String toolsJson = metadata.get(TOOL_METADATA_KEY);

        if (toolsJson == null || toolsJson.isBlank()) {
            return List.of();
        }

        try {
            return parseToolDefinitions(toolsJson, instance.getServiceName());
        } catch (Exception e) {
            log.warn("Failed to parse tools metadata from instance {}: {}",
                instance.getInstanceId(), e.getMessage());
            return List.of();
        }
    }

    /**
     * 解析工具定义 JSON。
     */
    private List<ToolServiceDefinition> parseToolDefinitions(String toolsJson, String serviceName) {
        // 简单实现，实际应使用 Jackson 或 Gson
        // 这里假设 JSON 格式正确
        List<ToolServiceDefinition> tools = new ArrayList<>();

        // TODO: 使用 JSON 解析器解析 toolsJson
        // 示例解析逻辑（简化版）

        log.debug("Parsing tools from service {}: {}", serviceName, toolsJson);

        return tools;
    }

    /**
     * 通知监听器。
     */
    private void notifyListeners(List<ToolServiceDefinition> tools) {
        for (ToolChangeListener listener : listeners) {
            try {
                listener.onToolsChanged(tools);
            } catch (Exception e) {
                log.warn("Failed to notify tool change listener: {}", e.getMessage());
            }
        }
    }
}