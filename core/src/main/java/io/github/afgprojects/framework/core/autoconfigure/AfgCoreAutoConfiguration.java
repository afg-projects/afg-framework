package io.github.afgprojects.framework.core.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import io.github.afgprojects.framework.core.config.AfgCoreProperties;

/**
 * AFG Core 统一配置自动配置类。
 *
 * <p>注册统一的 AfgCoreProperties，支持通过 afg.core 前缀配置所有核心功能。
 *
 * @since 1.1.0
 */
@AutoConfiguration
@EnableConfigurationProperties(AfgCoreProperties.class)
public class AfgCoreAutoConfiguration {
}
