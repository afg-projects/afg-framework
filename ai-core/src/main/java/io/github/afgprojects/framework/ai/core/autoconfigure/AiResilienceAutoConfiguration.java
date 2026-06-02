package io.github.afgprojects.framework.ai.core.autoconfigure;

import io.github.afgprojects.framework.ai.core.config.AfgAiProperties;
// import io.github.afgprojects.framework.ai.core.api.resilience.CircuitBreaker;
// import io.github.afgprojects.framework.ai.core.api.resilience.RetryPolicy;
// import io.github.afgprojects.framework.ai.core.api.resilience.ResilienceExecutor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AFG AI 韧性自动配置。
 *
 * <p>配置前缀：{@code afg.ai.resilience}
 *
 * @author afg-projects
 * @since 1.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties(AfgAiProperties.class)
@ConditionalOnProperty(prefix = "afg.ai.resilience", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AiResilienceAutoConfiguration {

    @Configuration
    @ConditionalOnProperty(prefix = "afg.ai.resilience", name = "enabled", havingValue = "true", matchIfMissing = true)
    static class ResilienceConfiguration {

        // TODO: 阶段3添加默认实现Bean
        // @Bean
        // @ConditionalOnMissingBean
        // public DefaultCircuitBreaker defaultCircuitBreaker() {
        //     return new DefaultCircuitBreaker();
        // }

        // TODO: 阶段3添加默认实现Bean
        // @Bean
        // @ConditionalOnMissingBean
        // public DefaultRetryPolicy defaultRetryPolicy() {
        //     return new DefaultRetryPolicy();
        // }

        // TODO: 阶段3添加默认实现Bean
        // @Bean
        // @ConditionalOnMissingBean
        // public DefaultResilienceExecutor defaultResilienceExecutor() {
        //     return new DefaultResilienceExecutor();
        // }

        // TODO: 阶段4添加AOP切面Bean
        // @Bean
        // @ConditionalOnMissingBean
        // public AiResilientAspect aiResilientAspect() {
        //     return new AiResilientAspect();
        // }
    }
}
