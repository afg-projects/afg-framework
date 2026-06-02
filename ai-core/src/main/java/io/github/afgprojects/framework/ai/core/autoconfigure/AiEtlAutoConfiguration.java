package io.github.afgprojects.framework.ai.core.autoconfigure;

import io.github.afgprojects.framework.ai.core.config.AfgAiProperties;
// import io.github.afgprojects.framework.ai.core.api.etl.CompositeReader;
// import io.github.afgprojects.framework.ai.core.api.etl.RecursiveCharacterTextSplitter;
// import io.github.afgprojects.framework.ai.core.api.etl.DefaultEncodingDetector;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AFG AI ETL 自动配置。
 *
 * <p>配置前缀：{@code afg.ai.etl}
 *
 * @author afg-projects
 * @since 1.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties(AfgAiProperties.class)
@ConditionalOnProperty(prefix = "afg.ai.etl", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AiEtlAutoConfiguration {

    @Configuration
    @ConditionalOnProperty(prefix = "afg.ai.etl", name = "enabled", havingValue = "true", matchIfMissing = true)
    static class EtlConfiguration {

        // TODO: 阶段3添加默认实现Bean
        // @Bean
        // @ConditionalOnMissingBean
        // public CompositeReader compositeReader() {
        //     return new CompositeReader();
        // }

        // TODO: 阶段3添加默认实现Bean
        // @Bean
        // @ConditionalOnMissingBean
        // public RecursiveCharacterTextSplitter recursiveCharacterTextSplitter(AfgAiProperties properties) {
        //     return new RecursiveCharacterTextSplitter(
        //         properties.getEtl().getSplitter().getChunkSize(),
        //         properties.getEtl().getSplitter().getChunkOverlap()
        //     );
        // }

        // TODO: 阶段3添加默认实现Bean
        // @Bean
        // @ConditionalOnMissingBean
        // public DefaultEncodingDetector defaultEncodingDetector() {
        //     return new DefaultEncodingDetector();
        // }
    }
}
