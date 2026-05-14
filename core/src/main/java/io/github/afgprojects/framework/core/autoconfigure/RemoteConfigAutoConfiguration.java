package io.github.afgprojects.framework.core.autoconfigure;

import io.github.afgprojects.framework.core.api.config.ConfigChangeListener;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
        public java.util.Optional<String> getConfig(@NonNull String key) {
            return java.util.Optional.empty();
        }

        @Override
        public java.util.Optional<String> getConfig(@NonNull String group, @NonNull String key) {
            return java.util.Optional.empty();
        }

        @Override
        public java.util.Map<String, String> getConfigs(@NonNull String prefix) {
            return java.util.Map.of();
        }

        @Override
        public boolean publishConfig(@NonNull String key, @NonNull String value) {
            return false;
        }

        @Override
        public boolean publishConfig(@NonNull String group, @NonNull String key, @NonNull String value) {
            return false;
        }

        @Override
        public void addListener(@NonNull String key, @NonNull ConfigChangeListener listener) {
        }

        @Override
        public void addListener(@NonNull String group, @NonNull String key, @NonNull ConfigChangeListener listener) {
        }

        @Override
        public void removeListener(@NonNull String key) {
        }

        @Override
        public void removeListener(@NonNull String group, @NonNull String key) {
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
