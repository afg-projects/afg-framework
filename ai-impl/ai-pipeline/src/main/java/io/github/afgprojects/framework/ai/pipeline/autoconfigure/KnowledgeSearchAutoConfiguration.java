package io.github.afgprojects.framework.ai.pipeline.autoconfigure;

import io.github.afgprojects.framework.ai.core.api.pipeline.KnowledgeSearchClient;
import io.github.afgprojects.framework.ai.pipeline.NoOpKnowledgeSearchClient;
import io.github.afgprojects.framework.ai.pipeline.RestKnowledgeSearchClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

/**
 * Auto-configuration for knowledge search client.
 *
 * <p>Provides two implementations:
 * <ul>
 *   <li>{@link RestKnowledgeSearchClient} - when {@code afg.ai.pipeline.knowledge-search.enabled=true}
 *       and a base URL is configured</li>
 *   <li>{@link NoOpKnowledgeSearchClient} - fallback when no knowledge search is configured</li>
 * </ul>
 *
 * <p>Configuration:
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
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(KnowledgeSearchProperties.class)
@ConditionalOnClass(KnowledgeSearchClient.class)
@ConditionalOnProperty(prefix = "afg.ai.pipeline.knowledge-search", name = "enabled", havingValue = "true", matchIfMissing = true)
public class KnowledgeSearchAutoConfiguration {

    /**
     * REST-based knowledge search client.
     *
     * <p>Activated when {@code afg.ai.pipeline.knowledge-search.base-url} is set.
     */
    @Bean
    @ConditionalOnMissingBean(KnowledgeSearchClient.class)
    @ConditionalOnProperty(prefix = "afg.ai.pipeline.knowledge-search", name = "base-url")
    public KnowledgeSearchClient restKnowledgeSearchClient(
            RestClient.Builder restClientBuilder,
            KnowledgeSearchProperties properties) {
        String baseUrl = properties.getBaseUrl();
        log.info("Creating RestKnowledgeSearchClient with baseUrl={}", baseUrl);
        RestClient restClient = restClientBuilder
            .baseUrl(baseUrl)
            .build();
        return new RestKnowledgeSearchClient(restClient);
    }

    /**
     * No-op fallback knowledge search client.
     *
     * <p>Activated when no other {@link KnowledgeSearchClient} bean is present.
     */
    @Bean
    @ConditionalOnMissingBean(KnowledgeSearchClient.class)
    public KnowledgeSearchClient noOpKnowledgeSearchClient() {
        log.info("Creating NoOpKnowledgeSearchClient (no knowledge search service configured)");
        return new NoOpKnowledgeSearchClient();
    }
}
