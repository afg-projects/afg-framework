package io.github.afgprojects.framework.ai.core.autoconfigure;

import io.github.afgprojects.framework.ai.core.api.tool.ToolAuditLogger;
import io.github.afgprojects.framework.ai.core.api.tool.ToolContextProvider;
import io.github.afgprojects.framework.ai.core.api.tool.ToolExecutionRecorder;
import io.github.afgprojects.framework.ai.core.api.tool.ToolPermissionChecker;
import io.github.afgprojects.framework.ai.core.api.tool.ToolRegistry;
import io.github.afgprojects.framework.ai.core.api.tool.remote.ToolDiscoveryClient;
import io.github.afgprojects.framework.ai.core.config.AfgAiProperties;
import io.github.afgprojects.framework.ai.core.security.NoOpToolExecutionRecorder;
import io.github.afgprojects.framework.ai.core.tool.ConfigurableToolDiscoveryClient;
import io.github.afgprojects.framework.ai.core.tool.DefaultToolRegistry;
import io.github.afgprojects.framework.ai.core.tool.NoOpToolAuditLogger;
import io.github.afgprojects.framework.ai.core.tool.NoOpToolPermissionChecker;
import io.github.afgprojects.framework.ai.core.tool.SecurityToolContextProvider;
import io.github.afgprojects.framework.ai.core.tool.ToolAspect;
import io.github.afgprojects.framework.ai.core.tool.ToolRegistrar;
import org.springframework.beans.factory.annotation.Autowired;
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
@AutoConfiguration(after = AiSecurityAutoConfiguration.class,
        afterName = "io.github.afgprojects.framework.core.autoconfigure.AfgAutoConfiguration")
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

    @Bean
    @ConditionalOnMissingBean(ToolContextProvider.class)
    public SecurityToolContextProvider securityToolContextProvider() {
        return new SecurityToolContextProvider();
    }

    @Bean
    @ConditionalOnMissingBean(ToolDiscoveryClient.class)
    @ConditionalOnProperty(prefix = "afg.ai.tool.discovery", name = "enabled", havingValue = "true")
    public ConfigurableToolDiscoveryClient configurableToolDiscoveryClient() {
        return new ConfigurableToolDiscoveryClient();
    }

    @Bean
    @ConditionalOnMissingBean(ToolAuditLogger.class)
    public NoOpToolAuditLogger noOpToolAuditLogger() {
        return new NoOpToolAuditLogger();
    }

    @Bean
    @ConditionalOnMissingBean(ToolPermissionChecker.class)
    public NoOpToolPermissionChecker noOpToolPermissionChecker() {
        return new NoOpToolPermissionChecker();
    }

    @Bean
    @ConditionalOnMissingBean
    public ToolRegistrar toolRegistrar(ToolRegistry toolRegistry) {
        return new ToolRegistrar(toolRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    public ToolAspect toolAspect(@Autowired(required = false) ToolExecutionRecorder recorder) {
        return new ToolAspect(recorder);
    }
}