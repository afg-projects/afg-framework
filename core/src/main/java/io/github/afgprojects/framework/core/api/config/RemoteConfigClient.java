package io.github.afgprojects.framework.core.api.config;

import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 远程配置客户端接口
 * 具体实现由 afg-config-nacos、afg-config-apollo、afg-config-consul 提供
 */
public interface RemoteConfigClient {
    Optional<String> getConfig(@NonNull String key);
    Optional<String> getConfig(@NonNull String group, @NonNull String key);
    Map<String, String> getConfigs(@NonNull String prefix);
    boolean publishConfig(@NonNull String key, @NonNull String value);
    boolean publishConfig(@NonNull String group, @NonNull String key, @NonNull String value);
    void addListener(@NonNull String key, @NonNull ConfigChangeListener listener);
    void addListener(@NonNull String group, @NonNull String key, @NonNull ConfigChangeListener listener);
    void removeListener(@NonNull String key);
    void removeListener(@NonNull String group, @NonNull String key);
    void refresh();
    String getClientName();
}
