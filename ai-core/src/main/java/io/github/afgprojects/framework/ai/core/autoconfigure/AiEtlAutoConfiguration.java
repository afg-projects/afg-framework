package io.github.afgprojects.framework.ai.core.autoconfigure;

import io.github.afgprojects.framework.ai.core.config.AfgAiProperties;
import io.github.afgprojects.framework.ai.core.etl.reader.CompositeReader;
import io.github.afgprojects.framework.ai.core.etl.reader.DefaultEncodingDetector;
import io.github.afgprojects.framework.ai.core.etl.transformer.RecursiveCharacterTextSplitter;
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

        @Bean
        @ConditionalOnMissingBean
        public CompositeReader compositeReader() {
            return new CompositeReader();
        }

        @Bean
        @ConditionalOnMissingBean
        public RecursiveCharacterTextSplitter recursiveCharacterTextSplitter(AfgAiProperties properties) {
            return new RecursiveCharacterTextSplitter(
                properties.getEtl().getSplitter().getChunkSize(),
                properties.getEtl().getSplitter().getChunkOverlap()
            );
        }

        @Bean
        @ConditionalOnMissingBean
        public DefaultEncodingDetector defaultEncodingDetector() {
            return new DefaultEncodingDetector();
        }
    }
}