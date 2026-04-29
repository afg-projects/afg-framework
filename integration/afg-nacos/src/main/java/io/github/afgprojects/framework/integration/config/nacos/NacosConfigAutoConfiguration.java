package io.github.afgprojects.framework.integration.config.nacos;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;

import io.github.afgprojects.framework.core.api.config.RemoteConfigClient;

/**
 * Nacos 配置客户端自动配置
 *
 * <p>当满足以下条件时自动配置 Nacos 配置客户端：
 * <ul>
 *   <li>classpath 中存在 {@link ConfigService} 类</li>
 *   <li>配置属性 afg.config.nacos.enabled 为 true（默认）</li>
 * </ul>
 *
 * <h3>配置示例</h3>
 * <pre>
 * afg:
 *   config:
 *     nacos:
 *       enabled: true
 *       server-addr: ${NACOS_ADDR:localhost:8848}
 *       namespace: ${NACOS_NAMESPACE:}
 *       group: DEFAULT_GROUP
 *       username: ${NACOS_USERNAME:nacos}
 *       password: ${NACOS_PASSWORD:nacos}
 * </pre>
 *
 * @since 1.0.0
 */
@AutoConfiguration
@ConditionalOnClass(ConfigService.class)
@ConditionalOnProperty(prefix = "afg.config.nacos", name = "enabled", havingValue = "true", matchIfMissing = true)
public class NacosConfigAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(NacosConfigAutoConfiguration.class);

    /**
     * 配置 Nacos 配置属性
     *
     * @return Nacos 配置属性对象
     */
    @Bean
    @ConfigurationProperties(prefix = "afg.config.nacos")
    public NacosConfigProperties nacosConfigProperties() {
        return new NacosConfigProperties();
    }

    /**
     * 创建 Nacos ConfigService 实例
     *
     * @param properties Nacos 配置属性
     * @return ConfigService 实例
     * @throws NacosException 如果创建失败
     */
    @Bean
    @ConditionalOnMissingBean(ConfigService.class)
    public ConfigService nacosConfigService(NacosConfigProperties properties) throws NacosException {
        log.info("Creating Nacos ConfigService, server: {}", properties.getServerAddr());
        return createConfigService(properties);
    }

    /**
     * 创建 Nacos 配置客户端
     *
     * @param configService Nacos ConfigService 实例
     * @param properties    Nacos 配置属性
     * @return Nacos 配置客户端实例
     */
    @Bean
    @ConditionalOnMissingBean(RemoteConfigClient.class)
    public NacosConfigClient nacosConfigClient(ConfigService configService, NacosConfigProperties properties) {
        log.info("Creating NacosConfigClient, group: {}", properties.getGroup());
        return new NacosConfigClient(configService, properties.getGroup());
    }

    /**
     * 创建 ConfigService
     *
     * @param properties 配置属性
     * @return ConfigService 实例
     * @throws NacosException 如果创建失败
     */
    private ConfigService createConfigService(NacosConfigProperties props) throws NacosException {
        java.util.Properties nacosProps = new java.util.Properties();

        // 必填：服务器地址
        nacosProps.setProperty("serverAddr", props.getServerAddr());

        // 可选：命名空间
        setIfNotEmpty(nacosProps, "namespace", props.getNamespace());

        // 可选：认证信息
        setIfNotEmpty(nacosProps, "username", props.getUsername());
        setIfNotEmpty(nacosProps, "password", props.getPassword());
        setIfNotEmpty(nacosProps, "accessToken", props.getAccessToken());

        // 可选：超时配置
        if (props.getConnectTimeout() > 0) {
            nacosProps.setProperty("configLongPollTimeout", String.valueOf(props.getPollTimeout()));
        }
        if (props.getReadTimeout() > 0) {
            nacosProps.setProperty("configReadTimeout", String.valueOf(props.getReadTimeout()));
        }

        // 可选：Context path
        setIfNotEmpty(nacosProps, "contextPath", props.getContextPath());

        // 可选：Endpoint
        setIfNotEmpty(nacosProps, "endpoint", props.getEndpoint());

        // 可选：HTTPS
        if (props.isSecure()) {
            nacosProps.setProperty("isUseCloudNamespaceParsing", "false");
        }

        // 可选：缓存配置
        if (props.isCacheEnabled() && props.getCacheDir() != null) {
            nacosProps.setProperty("configCacheDir", props.getCacheDir());
        }

        return NacosFactory.createConfigService(nacosProps);
    }

    private void setIfNotEmpty(java.util.Properties props, String key, @Nullable String value) {
        if (value != null && !value.isEmpty()) {
            props.setProperty(key, value);
        }
    }
}
