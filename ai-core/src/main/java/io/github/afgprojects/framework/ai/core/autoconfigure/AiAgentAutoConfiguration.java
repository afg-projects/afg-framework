package io.github.afgprojects.framework.ai.core.autoconfigure;

import io.github.afgprojects.framework.ai.core.config.AfgAiProperties;
// import io.github.afgprojects.framework.ai.core.api.agent.AgentExecutor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AFG AI 智能体自动配置。
 *
 * <p>配置前缀：{@code afg.ai.agent}
 *
 * @author afg-projects
 * @since 1.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties(AfgAiProperties.class)
@ConditionalOnProperty(prefix = "afg.ai.agent", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AiAgentAutoConfiguration {

    @Configuration
    @ConditionalOnProperty(prefix = "afg.ai.agent", name = "enabled", havingValue = "true", matchIfMissing = true)
    static class AgentConfiguration {

        // TODO: 阶段3添加默认实现Bean
        // @Bean
        // @ConditionalOnMissingBean
        // public DefaultAgentExecutor defaultAgentExecutor() {
        //     return new DefaultAgentExecutor();
        // }

        // TODO: 阶段3添加默认实现Bean
        // @Bean
        // @ConditionalOnMissingBean
        // public AiAgentAspect aiAgentAspect() {
        //     return new AiAgentAspect();
        // }
    }
}
