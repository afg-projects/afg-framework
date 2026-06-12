package io.github.afgprojects.framework.ai.core.autoconfigure;

import io.github.afgprojects.framework.ai.core.config.AfgAiProperties;
import io.github.afgprojects.framework.ai.core.model.DefaultModelRegistry;
import io.github.afgprojects.framework.ai.core.api.model.ModelRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AFG AI 模型管理自动配置。
 *
 * <p>配置前缀：{@code afg.ai.model}
 *
 * @author afg-projects
 * @since 1.0.0
 */
@AutoConfiguration(after = AiCoreAutoConfiguration.class,
        afterName = "io.github.afgprojects.framework.core.autoconfigure.AfgAutoConfiguration")
@EnableConfigurationProperties(AfgAiProperties.class)
@ConditionalOnProperty(prefix = "afg.ai.model", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AiModelAutoConfiguration {

    @Configuration
    @ConditionalOnProperty(prefix = "afg.ai.model", name = "enabled", havingValue = "true", matchIfMissing = true)
    static class ModelConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public ModelRegistry defaultModelRegistry() {
            return new DefaultModelRegistry();
        }
    }
}
