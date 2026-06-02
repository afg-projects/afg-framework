package io.github.afgprojects.framework.ai.core.autoconfigure;

import io.github.afgprojects.framework.ai.core.config.AfgAiProperties;
// import io.github.afgprojects.framework.ai.core.api.tool.InMemoryToolRegistry;
// import io.github.afgprojects.framework.ai.core.api.tool.DefaultToolExecutionRecorder;
// import io.github.afgprojects.framework.ai.core.api.tool.ToolExecutionAspect;
// import io.github.afgprojects.framework.ai.core.api.tool.ServiceToolRegistrar;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AFG AI 工具系统自动配置。
 *
 * <p>配置前缀：{@code afg.ai.tool}
 *
 * @author afg-projects
 * @since 1.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties(AfgAiProperties.class)
@ConditionalOnProperty(prefix = "afg.ai.tool", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AiToolAutoConfiguration {

    @Configuration
    @ConditionalOnProperty(prefix = "afg.ai.tool", name = "enabled", havingValue = "true", matchIfMissing = true)
    static class ToolConfiguration {

        // TODO: 阶段3添加默认实现Bean
        // @Bean
        // @ConditionalOnMissingBean
        // public InMemoryToolRegistry inMemoryToolRegistry() {
        //     return new InMemoryToolRegistry();
        // }

        // TODO: 阶段3添加默认实现Bean
        // @Bean
        // @ConditionalOnMissingBean
        // public DefaultToolExecutionRecorder defaultToolExecutionRecorder() {
        //     return new DefaultToolExecutionRecorder();
        // }

        // TODO: 阶段3添加默认实现Bean
        // @Bean
        // @ConditionalOnMissingBean
        // public ToolExecutionAspect toolExecutionAspect() {
        //     return new ToolExecutionAspect();
        // }

        // TODO: 阶段3添加默认实现Bean
        // @Bean
        // @ConditionalOnMissingBean
        // public ServiceToolRegistrar serviceToolRegistrar() {
        //     return new ServiceToolRegistrar();
        // }
    }
}
