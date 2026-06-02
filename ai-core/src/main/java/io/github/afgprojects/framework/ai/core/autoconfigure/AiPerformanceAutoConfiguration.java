package io.github.afgprojects.framework.ai.core.autoconfigure;

import io.github.afgprojects.framework.ai.core.config.AfgAiProperties;
// import io.github.afgprojects.framework.ai.core.api.performance.Cache;
// import io.github.afgprojects.framework.ai.core.api.performance.RateLimiter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AFG AI 性能优化自动配置。
 *
 * <p>配置前缀：{@code afg.ai.performance}
 *
 * @author afg-projects
 * @since 1.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties(AfgAiProperties.class)
@ConditionalOnProperty(prefix = "afg.ai.performance", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AiPerformanceAutoConfiguration {

    @Configuration
    @ConditionalOnProperty(prefix = "afg.ai.performance", name = "enabled", havingValue = "true", matchIfMissing = true)
    static class PerformanceConfiguration {

        // TODO: 阶段3添加默认实现Bean
        // @Bean
        // @ConditionalOnMissingBean
        // public DefaultCache defaultCache(AfgAiProperties properties) {
        //     return new DefaultCache(properties.getPerformance().getCache());
        // }

        // TODO: 阶段3添加默认实现Bean
        // @Bean
        // @ConditionalOnMissingBean
        // public DefaultRateLimiter defaultRateLimiter(AfgAiProperties properties) {
        //     return new DefaultRateLimiter(properties.getPerformance().getRateLimit());
        // }

        // TODO: 阶段3添加默认实现Bean
        // @Bean
        // @ConditionalOnMissingBean
        // public AiRateLimitedAspect aiRateLimitedAspect(RateLimiter rateLimiter) {
        //     return new AiRateLimitedAspect(rateLimiter);
        // }
    }
}
