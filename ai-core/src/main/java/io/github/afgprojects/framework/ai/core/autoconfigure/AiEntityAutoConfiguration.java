package io.github.afgprojects.framework.ai.core.autoconfigure;

import io.github.afgprojects.framework.ai.core.config.AfgAiProperties;
import io.github.afgprojects.framework.data.core.DataManager;
// import io.github.afgprojects.framework.ai.core.api.entity.AiEntityRegistrar;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AFG AI 实体注册自动配置。
 *
 * <p>负责注册 JPA 实体和 Liquibase 迁移，依赖 {@link DataManager}。
 *
 * <p>配置前缀：{@code afg.ai.entity}
 *
 * @author afg-projects
 * @since 1.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties(AfgAiProperties.class)
@ConditionalOnProperty(prefix = "afg.ai.entity", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnBean(DataManager.class)
public class AiEntityAutoConfiguration {

    @Configuration
    @ConditionalOnProperty(prefix = "afg.ai.entity", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnBean(DataManager.class)
    static class EntityConfiguration {

        // TODO: 阶段5添加实体注册Bean
        // @Bean
        // @ConditionalOnMissingBean
        // public AiEntityRegistrar aiEntityRegistrar(DataManager dataManager) {
        //     return new AiEntityRegistrar(dataManager);
        // }
    }
}
