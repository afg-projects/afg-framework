package io.github.afgprojects.framework.ai.core.tool;

import io.github.afgprojects.framework.ai.core.api.tool.remote.ToolDiscoveryClient;
import io.github.afgprojects.framework.ai.core.api.tool.remote.ToolServiceDefinition;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * 可配置的工具发现客户端默认实现。
 *
 * <p>提供基于内存配置的工具发现能力，支持动态添加和移除工具定义。
 * 使用 {@link CopyOnWriteArrayList} 保证线程安全。
 *
 * <p>使用示例：
 * <pre>{@code
 * ConfigurableToolDiscoveryClient client = new ConfigurableToolDiscoveryClient();
 *
 * // 添加工具定义
 * client.addToolDefinition(ToolServiceDefinition.of(
 *     "query_users",
 *     "Query user list",
 *     "{}",
 *     ToolEndpoint.of("user-service", "/api/tools/query_users")
 * ));
 *
 * // 发现所有工具
 * List<ToolServiceDefinition> tools = client.discoverTools();
 *
 * // 监听工具变化
 * client.addToolChangeListener(tools -> {
 *     System.out.println("Tools changed: " + tools.size());
 * });
 * }</pre>
 *
 * @since 1.0.0
 */
@Slf4j
public class ConfigurableToolDiscoveryClient implements ToolDiscoveryClient {

    private static final String CLIENT_NAME = "configurable-tool-discovery";

    private final CopyOnWriteArrayList<ToolServiceDefinition> toolDefinitions = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<ToolChangeListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * 创建空的工具发现客户端。
     */
    public ConfigurableToolDiscoveryClient() {
        log.debug("ConfigurableToolDiscoveryClient initialized");
    }

    /**
     * 创建带有初始工具定义的客户端。
     *
     * @param initialTools 初始工具定义列表
     */
    public ConfigurableToolDiscoveryClient(@NonNull List<ToolServiceDefinition> initialTools) {
        this.toolDefinitions.addAll(initialTools);
        log.debug("ConfigurableToolDiscoveryClient initialized with {} tools", initialTools.size());
    }

    @Override
    @NonNull
    public List<ToolServiceDefinition> discoverTools() {
        return Collections.unmodifiableList(toolDefinitions);
    }

    @Override
    @NonNull
    public List<ToolServiceDefinition> discoverTools(@NonNull String serviceId) {
        return toolDefinitions.stream()
            .filter(def -> serviceId.equals(def.endpoint().serviceId()))
            .collect(Collectors.toList());
    }

    @Override
    public void refresh() {
        log.debug("Refreshing tools, notifying {} listeners", listeners.size());
        notifyListeners();
    }

    @Override
    public void addToolChangeListener(@NonNull ToolChangeListener listener) {
        listeners.add(listener);
        log.debug("ToolChangeListener added, total listeners: {}", listeners.size());
    }

    @Override
    public void removeToolChangeListener(@NonNull ToolChangeListener listener) {
        listeners.remove(listener);
        log.debug("ToolChangeListener removed, total listeners: {}", listeners.size());
    }

    @Override
    @NonNull
    public String getClientName() {
        return CLIENT_NAME;
    }

    // ========== 扩展方法：动态管理工具定义 ==========

    /**
     * 添加工具定义。
     *
     * @param definition 工具定义
     */
    public void addToolDefinition(@NonNull ToolServiceDefinition definition) {
        toolDefinitions.add(definition);
        log.info("Tool definition added: {}", definition.name());
        notifyListeners();
    }

    /**
     * 添加多个工具定义。
     *
     * @param definitions 工具定义列表
     */
    public void addToolDefinitions(@NonNull List<ToolServiceDefinition> definitions) {
        toolDefinitions.addAll(definitions);
        log.info("Tool definitions added: {} tools", definitions.size());
        notifyListeners();
    }

    /**
     * 移除工具定义。
     *
     * @param definition 工具定义
     * @return 是否移除成功
     */
    public boolean removeToolDefinition(@NonNull ToolServiceDefinition definition) {
        boolean removed = toolDefinitions.remove(definition);
        if (removed) {
            log.info("Tool definition removed: {}", definition.name());
            notifyListeners();
        }
        return removed;
    }

    /**
     * 按名称移除工具定义。
     *
     * @param toolName 工具名称
     * @return 是否移除成功
     */
    public boolean removeToolDefinitionByName(@NonNull String toolName) {
        boolean removed = toolDefinitions.removeIf(def -> toolName.equals(def.name()));
        if (removed) {
            log.info("Tool definition removed by name: {}", toolName);
            notifyListeners();
        }
        return removed;
    }

    /**
     * 清空所有工具定义。
     */
    public void clearToolDefinitions() {
        int count = toolDefinitions.size();
        toolDefinitions.clear();
        log.info("All tool definitions cleared: {} tools removed", count);
        notifyListeners();
    }

    /**
     * 设置工具定义列表（替换现有列表）。
     *
     * @param definitions 新的工具定义列表
     */
    public void setToolDefinitions(@NonNull List<ToolServiceDefinition> definitions) {
        toolDefinitions.clear();
        toolDefinitions.addAll(definitions);
        log.info("Tool definitions set: {} tools", definitions.size());
        notifyListeners();
    }

    /**
     * 获取工具定义数量。
     *
     * @return 工具定义数量
     */
    public int getToolCount() {
        return toolDefinitions.size();
    }

    // ========== 私有方法 ==========

    private void notifyListeners() {
        if (listeners.isEmpty()) {
            return;
        }
        List<ToolServiceDefinition> currentTools = discoverTools();
        for (ToolChangeListener listener : listeners) {
            try {
                listener.onToolsChanged(currentTools);
            } catch (Exception e) {
                log.error("Error notifying ToolChangeListener", e);
            }
        }
    }
}
