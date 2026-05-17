package io.github.afgprojects.framework.integration.discovery.nacos;

import com.alibaba.nacos.api.naming.NamingService;
import io.github.afgprojects.framework.ai.core.tool.remote.ToolDiscoveryClient;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Nacos 工具发现自动配置。
 *
 * <p>配置示例：
 * <pre>{@code
 * afg:
 *   ai:
 *     tool:
 *       discovery:
 *         enabled: true
 *         nacos:
 *           enabled: true
 * }</pre>
 *
 * @since 1.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties(NacosToolDiscoveryProperties.class)
@ConditionalOnProperty(prefix = "afg.ai.tool.discovery", name = "enabled", havingValue = "true")
public class NacosToolDiscoveryAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(NacosToolDiscoveryAutoConfiguration.class);

    /**
     * 配置 Nacos 工具发现客户端。
     */
    @Bean
    @ConditionalOnBean(NamingService.class)
    @ConditionalOnProperty(prefix = "afg.ai.tool.discovery.nacos", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean(ToolDiscoveryClient.class)
    public ToolDiscoveryClient nacosToolDiscoveryClient(NamingService namingService) {
        log.info("Creating Nacos tool discovery client");
        return new NacosToolDiscoveryClient(namingService);
    }
}
