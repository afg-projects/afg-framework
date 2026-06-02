package io.github.afgprojects.framework.ai.core.api.tool.remote;

import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * 工具发现客户端接口。
 *
 * <p>提供远程工具发现能力，支持从注册中心或配置中发现工具服务。
 *
 * <p>使用示例：
 * <pre>{@code
 * ToolDiscoveryClient client = ...;
 *
 * // 发现所有工具
 * List<ToolServiceDefinition> tools = client.discoverTools();
 *
 * // 发现指定服务的工具
 * List<ToolServiceDefinition> tools = client.discoverTools("user-service");
 *
 * // 监听工具变化
 * client.addToolChangeListener(tools -> {
 *     System.out.println("Tools changed: " + tools.size());
 * });
 * }</pre>
 *
 * @since 1.0.0
 */
public interface ToolDiscoveryClient {

    /**
     * 发现所有工具。
     *
     * @return 工具服务定义列表
     */
    @NonNull
    List<ToolServiceDefinition> discoverTools();

    /**
     * 发现指定服务的工具。
     *
     * @param serviceId 服务 ID
     * @return 工具服务定义列表
     */
    @NonNull
    List<ToolServiceDefinition> discoverTools(@NonNull String serviceId);

    /**
     * 刷新工具列表。
     *
     * <p>重新从注册中心获取工具定义。
     */
    void refresh();

    /**
     * 添加工具变化监听器。
     *
     * @param listener 监听器
     */
    void addToolChangeListener(@NonNull ToolChangeListener listener);

    /**
     * 移除工具变化监听器。
     *
     * @param listener 监听器
     */
    void removeToolChangeListener(@NonNull ToolChangeListener listener);

    /**
     * 获取发现客户端名称。
     *
     * @return 客户端名称（如 "static", "nacos", "consul"）
     */
    @NonNull
    String getClientName();

    /**
     * 工具变化监听器。
     */
    @FunctionalInterface
    interface ToolChangeListener {

        /**
         * 工具变化回调。
         *
         * @param tools 变化后的工具列表
         */
        void onToolsChanged(@NonNull List<ToolServiceDefinition> tools);
    }
}