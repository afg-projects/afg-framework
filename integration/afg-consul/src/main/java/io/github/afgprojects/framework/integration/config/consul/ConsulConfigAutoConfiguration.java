package io.github.afgprojects.framework.integration.config.consul;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;

import com.ecwid.consul.v1.ConsulClient;

import io.github.afgprojects.framework.core.api.config.RemoteConfigClient;

/**
 * Consul 配置客户端自动配置
 *
 * <p>当满足以下条件时自动配置 Consul 配置客户端：
 * <ul>
 *   <li>classpath 中存在 {@link ConsulClient} 类</li>
 *   <li>配置属性 afg.config.consul.enabled 为 true（默认）</li>
 * </ul>
 *
 * <h3>配置示例</h3>
 * <pre>
 * afg:
 *   config:
 *     consul:
 *       enabled: true
 *       host: localhost
 *       port: 8500
 *       prefix: config/afg
 * </pre>
 *
 * @since 1.0.0
 */
@AutoConfiguration
@ConditionalOnClass(ConsulClient.class)
@ConditionalOnProperty(prefix = "afg.config.consul", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ConsulConfigAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ConsulConfigAutoConfiguration.class);

    /**
     * 配置 Consul 配置属性
     *
     * @return Consul 配置属性对象
     */
    @Bean
    @ConfigurationProperties(prefix = "afg.config.consul")
    public ConsulConfigProperties consulConfigProperties() {
        return new ConsulConfigProperties();
    }

    /**
     * 创建 Consul 配置客户端
     *
     * @param properties Consul 配置属性
     * @return Consul 配置客户端实例
     */
    @Bean
    @ConditionalOnMissingBean(RemoteConfigClient.class)
    public ConsulConfigClient consulConfigClient(ConsulConfigProperties properties) {
        log.info("Creating ConsulConfigClient, host: {}, port: {}, prefix: {}",
                properties.getHost(), properties.getPort(), properties.getPrefix());
        return new ConsulConfigClient(properties);
    }
}