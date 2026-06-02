package io.github.afgprojects.framework.ai.agent.autoconfigure;

import io.github.afgprojects.framework.ai.core.api.tool.ServiceToolRegistrar;
import io.github.afgprojects.framework.ai.core.api.tool.ToolRegistry;
import io.github.afgprojects.framework.core.invocation.BeanInvocationEngine;
import io.github.afgprojects.framework.core.invocation.ServiceMetadataRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnBean({BeanInvocationEngine.class, ToolRegistry.class})
@ConditionalOnProperty(prefix = "afg.invocation", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ServiceToolAutoConfiguration {

    @Bean
    public ServiceToolRegistrar serviceToolRegistrar(
            ServiceMetadataRegistry metadataRegistry,
            BeanInvocationEngine engine,
            ToolRegistry toolRegistry) {
        return new ServiceToolRegistrar(metadataRegistry, engine, toolRegistry);
    }
}
