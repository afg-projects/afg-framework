package io.github.afgprojects.framework.ai.pipeline.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for knowledge search client.
 *
 * <p>Properties are bound to the prefix {@code afg.ai.pipeline.knowledge-search}.
 *
 * <p>Example:
 * <pre>{@code
 * afg:
 *   ai:
 *     pipeline:
 *       knowledge-search:
 *         enabled: true
 *         base-url: http://afg-ai-services-ai-knowledge
 * }</pre>
 *
 * @author afg-projects
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "afg.ai.pipeline.knowledge-search")
public class KnowledgeSearchProperties {

    /**
     * Whether knowledge search is enabled.
     */
    private boolean enabled = true;

    /**
     * Base URL of the remote knowledge search service.
     *
     * <p>When set, a {@link io.github.afgprojects.framework.ai.pipeline.RestKnowledgeSearchClient}
     * will be created. When not set, a no-op fallback is used.
     */
    private String baseUrl;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
