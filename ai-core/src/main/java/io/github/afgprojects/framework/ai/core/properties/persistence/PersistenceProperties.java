package io.github.afgprojects.framework.ai.core.properties.persistence;

import io.github.afgprojects.framework.ai.core.properties.persistence.PersistenceMessageHistoryConfig;
import io.github.afgprojects.framework.ai.core.properties.persistence.PersistenceSessionConfig;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI Persistence configuration properties.
 *
 * <p>Prefix: {@code afg.ai.persistence}
 */
@Data
@ConfigurationProperties(prefix = "afg.ai.persistence")
public class PersistenceProperties {

    /**
     * Whether persistence support is enabled.
     */
    private boolean enabled = true;

    /**
     * Session storage configuration.
     */
    private PersistenceSessionConfig session = new PersistenceSessionConfig();

    /**
     * Message history storage configuration.
     */
    private PersistenceMessageHistoryConfig messageHistory = new PersistenceMessageHistoryConfig();
}
