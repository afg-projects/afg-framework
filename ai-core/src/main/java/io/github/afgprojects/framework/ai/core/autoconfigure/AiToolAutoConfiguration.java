package io.github.afgprojects.framework.ai.core.autoconfigure;

import io.github.afgprojects.framework.ai.core.api.tool.ToolExecutionRecorder;
import io.github.afgprojects.framework.ai.core.api.tool.ToolRegistry;
import io.github.afgprojects.framework.ai.core.config.AfgAiProperties;
import io.github.afgprojects.framework.ai.core.security.NoOpToolExecutionRecorder;
import io.github.afgprojects.framework.ai.core.tool.DefaultToolRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * AFG AI 工具自动配置。
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

    @Bean
    @ConditionalOnMissingBean(ToolRegistry.class)
    public DefaultToolRegistry defaultToolRegistry() {
        return new DefaultToolRegistry();
    }

    @Bean
    @ConditionalOnMissingBean(ToolExecutionRecorder.class)
    public NoOpToolExecutionRecorder noOpToolExecutionRecorder() {
        return new NoOpToolExecutionRecorder();
    }
}