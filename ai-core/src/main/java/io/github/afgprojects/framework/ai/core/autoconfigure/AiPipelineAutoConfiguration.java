package io.github.afgprojects.framework.ai.core.autoconfigure;

import io.github.afgprojects.framework.ai.core.config.AfgAiProperties;
import io.github.afgprojects.framework.ai.core.api.pipeline.ChatPipeline;
import io.github.afgprojects.framework.ai.core.api.pipeline.KnowledgeSearchClient;
import io.github.afgprojects.framework.ai.core.api.pipeline.PipelineStep;
import io.github.afgprojects.framework.ai.core.pipeline.DefaultChatPipeline;
import io.github.afgprojects.framework.ai.core.pipeline.NoOpKnowledgeSearchClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;

import java.util.List;

/**
 * AFG AI 对话管道自动配置。
 *
 * <p>配置前缀：{@code afg.ai.pipeline}
 *
 * @author afg-projects
 * @since 1.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties(AfgAiProperties.class)
@ConditionalOnProperty(prefix = "afg.ai.pipeline", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AiPipelineAutoConfiguration {

    @Configuration
    @ConditionalOnProperty(prefix = "afg.ai.pipeline", name = "enabled", havingValue = "true", matchIfMissing = true)
    static class PipelineConfiguration {

        @Bean
        @ConditionalOnMissingBean(KnowledgeSearchClient.class)
        public NoOpKnowledgeSearchClient noOpKnowledgeSearchClient() {
            return new NoOpKnowledgeSearchClient();
        }

        @Bean
        @ConditionalOnMissingBean(ChatPipeline.class)
        public ChatPipeline chatPipeline(@Nullable List<PipelineStep> steps) {
            return new DefaultChatPipeline(steps != null ? steps : List.of());
        }
    }
}
