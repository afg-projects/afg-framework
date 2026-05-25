package io.github.afgprojects.framework.ai.agent.tool.remote;

import io.github.afgprojects.framework.ai.core.tool.Tool;
import io.github.afgprojects.framework.ai.core.tool.ToolRegistry;
import io.github.afgprojects.framework.ai.core.tool.remote.ToolDiscoveryClient;
import io.github.afgprojects.framework.ai.core.tool.remote.ToolServiceDefinition;
import io.github.afgprojects.framework.core.api.registry.ServiceDiscovery;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 远程工具注册表。
 *
 * <p>实现 {@link ToolRegistry} 接口，从服务发现动态获取远程工具。
 *
 * <p>特性：
 * <ul>
 *   <li>动态发现远程工具</li>
 *   <li>监听工具变化</li>
 *   <li>支持工具刷新</li>
 *   <li>线程安全</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class RemoteToolRegistry implements ToolRegistry {

    private static final Logger log = LoggerFactory.getLogger(RemoteToolRegistry.class);

    private final ToolDiscoveryClient discoveryClient;
    private final ServiceDiscovery serviceDiscovery;
    private final Map<String, RemoteTool> remoteTools = new ConcurrentHashMap<>();
    private volatile boolean initialized = false;

    /**
     * 创建远程工具注册表。
     *
     * @param discoveryClient 工具发现客户端
     * @param serviceDiscovery 服务发现客户端
     */
    public RemoteToolRegistry(
            @NonNull ToolDiscoveryClient discoveryClient,
            @NonNull ServiceDiscovery serviceDiscovery) {
        this.discoveryClient = discoveryClient;
        this.serviceDiscovery = serviceDiscovery;
    }

    /**
     * 初始化注册表。
     *
     * <p>发现并注册所有远程工具。
     */
    public void initialize() {
        if (initialized) {
            return;
        }

        log.info("Initializing remote tool registry from {}", discoveryClient.getClientName());

        // 发现并注册工具
        refresh();

        // 监听工具变化
        discoveryClient.addToolChangeListener(this::onToolsChanged);

        initialized = true;
        log.info("Remote tool registry initialized with {} tools", remoteTools.size());
    }

    /**
     * 刷新工具列表。
     */
    public void refresh() {
        log.debug("Refreshing remote tools...");

        try {
            List<ToolServiceDefinition> tools = discoveryClient.discoverTools();

            // 清除旧工具
            Set<String> oldNames = new HashSet<>(remoteTools.keySet());

            // 注册新工具
            for (ToolServiceDefinition definition : tools) {
                registerRemoteTool(definition);
                oldNames.remove(definition.name());
            }

            // 移除不再存在的工具
            for (String name : oldNames) {
                remoteTools.remove(name);
                log.debug("Removed remote tool: {}", name);
            }

            log.info("Refreshed remote tools: {} active", remoteTools.size());
        } catch (Exception e) {
            log.error("Failed to refresh remote tools: {}", e.getMessage());
        }
    }

    /**
     * 工具变化回调。
     */
    private void onToolsChanged(List<ToolServiceDefinition> tools) {
        log.info("Remote tools changed, updating registry...");

        // 清除旧工具
        remoteTools.clear();

        // 注册新工具
        for (ToolServiceDefinition definition : tools) {
            registerRemoteTool(definition);
        }

        log.info("Remote tools updated: {} active", remoteTools.size());
    }

    /**
     * 注册远程工具。
     */
    private void registerRemoteTool(@NonNull ToolServiceDefinition definition) {
        RemoteTool tool = new RemoteTool(definition, serviceDiscovery);
        remoteTools.put(definition.name(), tool);
        log.debug("Registered remote tool: {} -> {}",
            definition.name(), definition.endpoint().serviceId());
    }

    @Override
    public <I, O> void register(@NonNull Tool<I, O> tool) {
        throw new UnsupportedOperationException(
            "RemoteToolRegistry does not support manual registration. " +
            "Use ToolDiscoveryClient to discover tools.");
    }

    @Override
    public <I, O> void registerOrReplace(@NonNull Tool<I, O> tool) {
        throw new UnsupportedOperationException(
            "RemoteToolRegistry does not support manual registration. " +
            "Use ToolDiscoveryClient to discover tools.");
    }

    @Override
    public @NonNull Optional<Tool<?, ?>> getTool(@NonNull String name) {
        return Optional.ofNullable(remoteTools.get(name));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <I, O> @NonNull Optional<Tool<I, O>> getTool(
            @NonNull String name,
            @NonNull Class<I> inputType,
            @NonNull Class<O> outputType) {
        RemoteTool tool = remoteTools.get(name);
        if (tool == null) {
            return Optional.empty();
        }
        // 类型安全由调用者保证
        return Optional.of((Tool<I, O>) tool);
    }

    @Override
    public @NonNull Collection<Tool<?, ?>> getAllTools() {
        return Collections.unmodifiableCollection(remoteTools.values());
    }

    @Override
    public boolean exists(@NonNull String name) {
        return remoteTools.containsKey(name);
    }

    @Override
    public boolean unregister(@NonNull String name) {
        throw new UnsupportedOperationException(
            "RemoteToolRegistry does not support manual unregistration. " +
            "Use ToolDiscoveryClient to manage tools.");
    }

    @Override
    public void clear() {
        remoteTools.clear();
        log.info("Remote tool registry cleared");
    }

    @Override
    public int size() {
        return remoteTools.size();
    }

    /**
     * 获取工具服务定义。
     *
     * @param name 工具名称
     * @return 工具服务定义
     */
    public @Nullable ToolServiceDefinition getToolDefinition(@NonNull String name) {
        RemoteTool tool = remoteTools.get(name);
        if (tool == null) {
            return null;
        }
        return tool.getDefinition();
    }

    /**
     * 获取所有工具服务定义。
     *
     * @return 工具服务定义列表
     */
    public @NonNull List<ToolServiceDefinition> getAllToolDefinitionsDetailed() {
        return remoteTools.values().stream()
            .map(RemoteTool::getDefinition)
            .toList();
    }
}
