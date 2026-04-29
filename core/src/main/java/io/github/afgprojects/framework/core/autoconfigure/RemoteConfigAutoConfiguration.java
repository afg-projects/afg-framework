package io.github.afgprojects.framework.core.autoconfigure;

import io.github.afgprojects.framework.core.api.config.ConfigChangeListener;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;

import io.github.afgprojects.framework.core.api.config.RemoteConfigClient;

/**
 * 远程配置自动配置类
 * 当启用远程配置时，提供默认的 NoOp 实现
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "afg.remote-config", name = "enabled", havingValue = "true")
public class RemoteConfigAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(RemoteConfigClient.class)
    public RemoteConfigClient noOpRemoteConfigClient() {
        return new NoOpRemoteConfigClient();
    }

    /**
     * 默认的空实现远程配置客户端
     */
    private static class NoOpRemoteConfigClient implements RemoteConfigClient {
        @Override
        public java.util.Optional<String> getConfig(String key) {
            return java.util.Optional.empty();
        }

        @Override
        public java.util.Optional<String> getConfig(String group, String key) {
            return java.util.Optional.empty();
        }

        @Override
        public java.util.Map<String, String> getConfigs(String prefix) {
            return java.util.Map.of();
        }

        @Override
        public boolean publishConfig(String key, String value) {
            return false;
        }

        @Override
        public boolean publishConfig(String group, String key, String value) {
            return false;
        }

        @Override
        public void addListener(String key, ConfigChangeListener listener) {
        }

        @Override
        public void addListener(String group, String key, ConfigChangeListener listener) {
        }

        @Override
        public void removeListener(String key) {
        }

        @Override
        public void removeListener(String group, String key) {
        }

        @Override
        public void refresh() {
        }

        @Override
        public String getClientName() {
            return "no-op";
        }
    }
}
