package io.github.afgprojects.framework.ai.core.autoconfigure;

import io.github.afgprojects.framework.ai.core.config.AfgAiProperties;
// import io.github.afgprojects.framework.ai.core.api.model.ModelRegistry;
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
@AutoConfiguration
@EnableConfigurationProperties(AfgAiProperties.class)
@ConditionalOnProperty(prefix = "afg.ai.model", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AiModelAutoConfiguration {

    @Configuration
    @ConditionalOnProperty(prefix = "afg.ai.model", name = "enabled", havingValue = "true", matchIfMissing = true)
    static class ModelConfiguration {

        // TODO: 阶段3添加默认实现Bean
        // @Bean
        // @ConditionalOnMissingBean
        // public InMemoryModelRegistry inMemoryModelRegistry() {
        //     return new InMemoryModelRegistry();
        // }

        // TODO: 阶段3添加默认实现Bean
        // @Bean
        // @ConditionalOnMissingBean
        // public ModelRouteAspect modelRouteAspect() {
        //     return new ModelRouteAspect();
        // }
    }
}
