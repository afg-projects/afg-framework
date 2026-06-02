package io.github.afgprojects.framework.ai.core.autoconfigure;

import io.github.afgprojects.framework.ai.core.config.AfgAiProperties;
// import io.github.afgprojects.framework.ai.core.api.persistence.SessionStore;
// import io.github.afgprojects.framework.ai.core.api.persistence.MessageHistoryStore;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AFG AI 持久化自动配置。
 *
 * <p>配置前缀：{@code afg.ai.persistence}
 *
 * @author afg-projects
 * @since 1.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties(AfgAiProperties.class)
@ConditionalOnProperty(prefix = "afg.ai.persistence", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AiPersistenceAutoConfiguration {

    @Configuration
    @ConditionalOnProperty(prefix = "afg.ai.persistence", name = "enabled", havingValue = "true", matchIfMissing = true)
    static class PersistenceConfiguration {

        // TODO: 阶段3添加默认实现Bean
        // @Bean
        // @ConditionalOnMissingBean
        // public DefaultSessionStore defaultSessionStore() {
        //     return new DefaultSessionStore();
        // }

        // TODO: 阶段3添加默认实现Bean
        // @Bean
        // @ConditionalOnMissingBean
        // public DefaultMessageHistoryStore defaultMessageHistoryStore() {
        //     return new DefaultMessageHistoryStore();
        // }
    }
}
