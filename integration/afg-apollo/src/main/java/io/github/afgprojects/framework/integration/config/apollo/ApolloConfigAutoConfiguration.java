package io.github.afgprojects.framework.integration.config.apollo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;

import com.ctrip.framework.apollo.Config;

import io.github.afgprojects.framework.core.api.config.RemoteConfigClient;

/**
 * Apollo 配置客户端自动配置
 *
 * <p>当满足以下条件时自动配置 Apollo 配置客户端：
 * <ul>
 *   <li>classpath 中存在 {@link Config} 类</li>
 *   <li>配置属性 afg.config.apollo.enabled 为 true（默认）</li>
 * </ul>
 *
 * <h3>配置示例</h3>
 * <pre>
 * afg:
 *   config:
 *     apollo:
 *       enabled: true
 *       namespace: application
 * </pre>
 *
 * <h3>Apollo 系统属性</h3>
 * <ul>
 *   <li>app.id - 应用 ID（必填）</li>
 *   <li>apollo.meta - Apollo Meta Server 地址（必填）</li>
 *   <li>apollo.cluster - 集群名称（可选）</li>
 * </ul>
 *
 * @since 1.0.0
 */
@AutoConfiguration
@ConditionalOnClass(Config.class)
@ConditionalOnProperty(prefix = "afg.config.apollo", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ApolloConfigAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ApolloConfigAutoConfiguration.class);

    /**
     * 配置 Apollo 配置属性
     *
     * @return Apollo 配置属性对象
     */
    @Bean
    @ConfigurationProperties(prefix = "afg.config.apollo")
    public ApolloConfigProperties apolloConfigProperties() {
        return new ApolloConfigProperties();
    }

    /**
     * 创建 Apollo 配置客户端
     *
     * @param properties Apollo 配置属性
     * @return Apollo 配置客户端实例
     */
    @Bean
    @ConditionalOnMissingBean(RemoteConfigClient.class)
    public ApolloConfigClient apolloConfigClient(ApolloConfigProperties properties) {
        log.info("Creating ApolloConfigClient, namespace: {}", properties.getNamespace());
        return new ApolloConfigClient(properties.getNamespace());
    }
}
