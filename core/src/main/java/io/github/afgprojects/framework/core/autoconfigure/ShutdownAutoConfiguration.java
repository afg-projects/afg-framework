package io.github.afgprojects.framework.core.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import io.github.afgprojects.framework.core.config.AfgCoreProperties;
import io.github.afgprojects.framework.core.web.shutdown.ShutdownHook;

/**
 * Auto-configuration for graceful shutdown.
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "afg.core.shutdown", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(AfgCoreProperties.class)
public class ShutdownAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ShutdownHook shutdownHook(AfgCoreProperties properties) {
        return new ShutdownHook(properties);
    }
}
