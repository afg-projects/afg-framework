package io.github.afgprojects.framework.ai.core.autoconfigure;

import io.github.afgprojects.framework.ai.core.config.AfgAiProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * AFG AI 核心自动配置。
 *
 * <p>总入口，仅启用 {@link AfgAiProperties} 配置属性绑定。
 * 各子模块的自动配置由各自的 AutoConfiguration 类负责。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@AutoConfiguration(afterName = "io.github.afgprojects.framework.core.autoconfigure.AfgAutoConfiguration")
@EnableConfigurationProperties({AfgAiProperties.class, AiConfigurationProperties.class})
@ConditionalOnProperty(prefix = "afg.ai", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AiCoreAutoConfiguration {

}
