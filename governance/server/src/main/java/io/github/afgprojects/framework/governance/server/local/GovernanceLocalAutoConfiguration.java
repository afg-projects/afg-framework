package io.github.afgprojects.framework.governance.server.local;

import io.github.afgprojects.framework.core.api.config.RemoteConfigClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 本地模式自动配置
 * <p>
 * 当 {@code afg.governance.local.enabled=true} 时激活，
 * 注册本地模式的配置客户端和注册客户端 Bean，
 * 替代 gRPC 客户端实现同 JVM 内直接调用。
 * <p>
 * 本配置在 {@code GovernanceServerAutoConfiguration} 之后执行，
 * 确保服务端组件已就绪；同时在 {@code GovernanceClientAutoConfiguration} 之前执行，
 * 使得 {@code @ConditionalOnMissingBean(RemoteConfigClient.class)} 能阻止 gRPC 客户端创建。
 *
 * @author afg-projects
 */
@Slf4j
@AutoConfiguration(
        afterName = "io.github.afgprojects.framework.governance.server.config.GovernanceServerAutoConfiguration",
        beforeName = "io.github.afgprojects.framework.governance.client.config.GovernanceClientAutoConfiguration"
)
@ConditionalOnProperty(prefix = "afg.governance.local", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(GovernanceLocalProperties.class)
public class GovernanceLocalAutoConfiguration {

    /**
     * 创建本地模式配置客户端，注册为 {@link RemoteConfigClient} Bean。
     * <p>
     * 使用 {@code @ConditionalOnMissingBean(RemoteConfigClient.class)} 确保
     * 如果已有其他实现（如 Nacos/Apollo），不会覆盖。
     * 同时这也使得 {@code GovernanceClientAutoConfiguration} 中的
     * gRPC 配置客户端不会创建。
     */
    @Bean
    @ConditionalOnMissingBean(RemoteConfigClient.class)
    public LocalGovernanceConfigClient localGovernanceConfigClient(
            io.github.afgprojects.framework.governance.server.service.config.ConfigValueService configValueService,
            io.github.afgprojects.framework.governance.server.service.config.ConfigItemService configItemService,
            io.github.afgprojects.framework.governance.server.service.config.ConfigGroupService configGroupService) {

        log.info("Initializing LocalGovernanceConfigClient (local mode, bypassing gRPC)");
        return new LocalGovernanceConfigClient(configValueService, configItemService, configGroupService);
    }

    /**
     * 创建本地模式服务注册发现客户端
     */
    @Bean
    public LocalGovernanceRegistryClient localGovernanceRegistryClient(
            io.github.afgprojects.framework.governance.server.service.registry.ServiceRegistryService registryService,
            GovernanceLocalProperties properties,
            @Value("${spring.application.name:unknown}") String applicationName) {

        // 如果未设置服务名，使用应用名
        if (properties.getServiceName() == null || properties.getServiceName().isBlank()) {
            properties.setServiceName(applicationName);
        }

        log.info("Initializing LocalGovernanceRegistryClient (local mode, bypassing gRPC): serviceName={}",
                properties.getServiceName());

        return new LocalGovernanceRegistryClient(registryService, properties);
    }
}
