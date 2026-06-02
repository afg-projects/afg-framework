package io.github.afgprojects.framework.ai.agent.autoconfigure;

import io.github.afgprojects.framework.ai.agent.tool.remote.RemoteToolRegistry;
import io.github.afgprojects.framework.ai.agent.tool.remote.StaticToolDiscoveryClient;
import io.github.afgprojects.framework.ai.core.api.tool.remote.ToolDiscoveryClient;
import io.github.afgprojects.framework.core.api.registry.ServiceDiscovery;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * 工具发现自动配置。
 *
 * <p>配置远程工具发现所需的组件：
 * <ul>
 *   <li>ToolDiscoveryClient - 工具发现客户端</li>
 *   <li>RemoteToolRegistry - 远程工具注册表</li>
 * </ul>
 *
 * <p>配置示例：
 * <pre>{@code
 * afg:
 *   ai:
 *     tool:
 *       discovery:
 *         enabled: true
 *         static:
 *           enabled: true
 *           tools:
 *             - name: query_users
 *               description: 查询用户列表
 *               endpoint:
 *                 serviceId: user-service
 *                 path: /api/tools/query_users
 * }</pre>
 *
 * @since 1.0.0
 */
@AutoConfiguration(after = SecureToolAutoConfiguration.class)
@EnableConfigurationProperties(ToolDiscoveryProperties.class)
@ConditionalOnProperty(prefix = "afg.ai.tool.discovery", name = "enabled", havingValue = "true", matchIfMissing = false)
public class ToolDiscoveryAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ToolDiscoveryAutoConfiguration.class);

    /**
     * 配置静态工具发现客户端。
     *
     * <p>从配置文件读取工具定义。
     */
    @Bean
    @ConditionalOnProperty(prefix = "afg.ai.tool.discovery.static", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean(ToolDiscoveryClient.class)
    public ToolDiscoveryClient staticToolDiscoveryClient(ToolDiscoveryProperties properties) {
        log.info("Creating static tool discovery client");

        List<ToolDiscoveryProperties.ToolConfig> toolConfigs = properties.getStatic().getTools();
        if (toolConfigs == null || toolConfigs.isEmpty()) {
            log.warn("No static tools configured");
            return new StaticToolDiscoveryClient(List.of());
        }

        // 转换配置
        java.util.List<io.github.afgprojects.framework.ai.core.api.tool.remote.ToolServiceDefinition> tools =
            toolConfigs.stream()
                .map(this::toToolServiceDefinition)
                .toList();

        return new StaticToolDiscoveryClient(tools);
    }

    /**
     * 配置远程工具注册表。
     *
     * <p>需要 ToolDiscoveryClient 和 ServiceDiscovery。
     */
    @Bean
    @ConditionalOnBean({ToolDiscoveryClient.class, ServiceDiscovery.class})
    @ConditionalOnMissingBean
    public RemoteToolRegistry remoteToolRegistry(
            ToolDiscoveryClient discoveryClient,
            ServiceDiscovery serviceDiscovery) {
        log.info("Creating remote tool registry");

        RemoteToolRegistry registry = new RemoteToolRegistry(discoveryClient, serviceDiscovery);
        registry.initialize();
        return registry;
    }

    /**
     * 转换配置到 ToolServiceDefinition。
     */
    private io.github.afgprojects.framework.ai.core.api.tool.remote.ToolServiceDefinition toToolServiceDefinition(
            ToolDiscoveryProperties.ToolConfig config) {
        io.github.afgprojects.framework.ai.core.api.tool.remote.ToolEndpoint endpoint =
            io.github.afgprojects.framework.ai.core.api.tool.remote.ToolEndpoint.builder()
                .serviceId(config.getEndpoint().getServiceId())
                .path(config.getEndpoint().getPath())
                .method(config.getEndpoint().getMethod() != null ? config.getEndpoint().getMethod() : "POST")
                .build();

        return io.github.afgprojects.framework.ai.core.api.tool.remote.ToolServiceDefinition.builder()
            .name(config.getName())
            .description(config.getDescription() != null ? config.getDescription() : "")
            .inputSchema(config.getInputSchema() != null ? config.getInputSchema() : "{}")
            .endpoint(endpoint)
            .requiredPermission(config.getPermission())
            .timeoutMs(config.getTimeoutMs() != null ? config.getTimeoutMs() : 30000)
            .retryCount(config.getRetryCount() != null ? config.getRetryCount() : 3)
            .sensitive(Boolean.TRUE.equals(config.isSensitive()))
            .auditable(config.isAuditable() == null || config.isAuditable())
            .build();
    }
}