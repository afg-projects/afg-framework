package io.github.afgprojects.framework.ai.core.autoconfigure;

import io.github.afgprojects.framework.ai.core.config.AfgAiProperties;
// import io.github.afgprojects.framework.ai.core.api.skill.DefaultSkillRegistry;
// import io.github.afgprojects.framework.ai.core.api.skill.DefaultSkillExecutor;
// import io.github.afgprojects.framework.ai.core.api.skill.DefaultSkillDispatcher;
// import io.github.afgprojects.framework.ai.core.api.skill.DefaultIntentAnalyzer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AFG AI 技能系统自动配置。
 *
 * <p>配置前缀：{@code afg.ai.skill}
 *
 * @author afg-projects
 * @since 1.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties(AfgAiProperties.class)
@ConditionalOnProperty(prefix = "afg.ai.skill", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AiSkillAutoConfiguration {

    @Configuration
    @ConditionalOnProperty(prefix = "afg.ai.skill", name = "enabled", havingValue = "true", matchIfMissing = true)
    static class SkillConfiguration {

        // TODO: 阶段3添加默认实现Bean
        // @Bean
        // @ConditionalOnMissingBean
        // public DefaultSkillRegistry defaultSkillRegistry() {
        //     return new DefaultSkillRegistry();
        // }

        // TODO: 阶段3添加默认实现Bean
        // @Bean
        // @ConditionalOnMissingBean
        // public DefaultSkillExecutor defaultSkillExecutor() {
        //     return new DefaultSkillExecutor();
        // }

        // TODO: 阶段3添加默认实现Bean
        // @Bean
        // @ConditionalOnMissingBean
        // public DefaultSkillDispatcher defaultSkillDispatcher() {
        //     return new DefaultSkillDispatcher();
        // }

        // TODO: 阶段3添加默认实现Bean
        // @Bean
        // @ConditionalOnMissingBean
        // public DefaultIntentAnalyzer defaultIntentAnalyzer() {
        //     return new DefaultIntentAnalyzer();
        // }
    }
}
