package io.github.afgprojects.framework.governance.client.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.afgprojects.framework.core.api.config.RemoteConfigClient;
import io.github.afgprojects.framework.core.config.AfgConfigRegistry;
import io.github.afgprojects.framework.governance.client.common.GovernanceChannelManager;
import io.github.afgprojects.framework.governance.client.config.GovernanceConfigClient;
import io.github.afgprojects.framework.governance.client.registry.GovernanceRegistryClient;
import io.github.afgprojects.framework.governance.client.properties.common.GovernanceCommonProperties;
import io.github.afgprojects.framework.governance.client.properties.config.GovernanceConfigProperties;
import io.github.afgprojects.framework.governance.client.properties.registry.GovernanceRegistryProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import jakarta.annotation.PreDestroy;

/**
 * Governance Client 自动配置
 * <p>
 * 支持配置中心和服务注册发现两个独立的客户端：
 * <ul>
 *   <li>GovernanceConfigClient - 配置中心客户端</li>
 *   <li>GovernanceRegistryClient - 服务注册发现客户端</li>
 * </ul>
 *
 * @author afg-projects
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties({
        GovernanceCommonProperties.class,
        GovernanceConfigProperties.class,
        GovernanceRegistryProperties.class
})
@ConditionalOnProperty(prefix = "afg.governance.client", name = "enabled", havingValue = "true", matchIfMissing = true)
public class GovernanceClientAutoConfiguration {

    private GovernanceChannelManager channelManager;
    private GovernanceConfigClient configClient;
    private GovernanceRegistryClient registryClient;

    /**
     * 创建共享的 Channel 管理器
     */
    @Bean
    @ConditionalOnMissingBean
    public GovernanceChannelManager governanceChannelManager(GovernanceCommonProperties properties) {
        log.info("Initializing GovernanceChannelManager with server: {}", properties.getServerAddr());
        this.channelManager = new GovernanceChannelManager(properties);
        return this.channelManager;
    }

    // ==================== 配置中心客户端 ====================

    /**
     * 创建配置中心客户端
     */
    @Bean
    @ConditionalOnProperty(prefix = "afg.governance.client.config", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(RemoteConfigClient.class)
    public GovernanceConfigClient governanceConfigClient(
            GovernanceChannelManager channelManager,
            GovernanceConfigProperties properties,
            @Value("${spring.application.name:unknown}") String applicationName) {

        // 如果未设置服务名，使用应用名
        if (properties.getServiceName() == null || properties.getServiceName().isBlank()) {
            properties.setServiceName(applicationName);
        }

        log.info("Initializing GovernanceConfigClient: serviceName={}, environment={}",
                properties.getServiceName(), properties.getEnvironment());

        this.configClient = new GovernanceConfigClient(channelManager, properties);
        this.configClient.start();

        return this.configClient;
    }

    // ==================== 服务注册发现客户端 ====================

    /**
     * 创建服务注册发现客户端
     */
    @Bean
    @ConditionalOnProperty(prefix = "afg.governance.client.registry", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean
    public GovernanceRegistryClient governanceRegistryClient(
            GovernanceChannelManager channelManager,
            GovernanceRegistryProperties properties,
            @Value("${spring.application.name:unknown}") String applicationName,
            @Value("${server.port:8080}") int serverPort) {

        // 如果未设置服务名，使用应用名
        if (properties.getServiceName() == null || properties.getServiceName().isBlank()) {
            properties.setServiceName(applicationName);
        }

        // 如果未设置端口，使用服务器端口
        if (properties.getPort() == null) {
            properties.setPort(serverPort);
        }

        log.info("Initializing GovernanceRegistryClient: serviceName={}, port={}",
                properties.getServiceName(), properties.getPort());

        this.registryClient = new GovernanceRegistryClient(channelManager, properties);
        this.registryClient.start();

        return this.registryClient;
    }

    // ==================== 配置自动注册 ====================

    /**
     * 创建配置自动注册器
     */
    @Bean
    @ConditionalOnProperty(prefix = "afg.governance.client.config", name = "auto-register", havingValue = "true")
    public ConfigAutoRegistrar configAutoRegistrar(
            AfgConfigRegistry configRegistry,
            Environment environment,
            GovernanceConfigClient configClient,
            GovernanceConfigProperties properties,
            ObjectMapper objectMapper) {

        log.info("Config auto-registration enabled with prefix filter: {}",
                properties.getEffectiveAutoRegisterPrefixes());

        return new ConfigAutoRegistrar(configRegistry, environment, configClient, properties, objectMapper);
    }

    @PreDestroy
    public void destroy() {
        if (configClient != null) {
            configClient.stop();
        }
        if (registryClient != null) {
            registryClient.stop();
        }
        if (channelManager != null) {
            channelManager.shutdown();
        }
    }
}
