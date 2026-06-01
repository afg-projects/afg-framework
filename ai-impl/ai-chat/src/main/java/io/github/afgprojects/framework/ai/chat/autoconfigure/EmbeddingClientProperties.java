package io.github.afgprojects.framework.ai.chat.autoconfigure;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * EmbeddingClient 配置属性
 *
 * @author afg-projects
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "afg.ai.embedding")
public class EmbeddingClientProperties {

    private boolean enabled = true;

    @Nullable
    private String defaultName;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Nullable
    public String getDefaultName() {
        return defaultName;
    }

    public void setDefaultName(@Nullable String defaultName) {
        this.defaultName = defaultName;
    }
}