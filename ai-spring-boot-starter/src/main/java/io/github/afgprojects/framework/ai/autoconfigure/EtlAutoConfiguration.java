package io.github.afgprojects.framework.ai.autoconfigure;

import io.github.afgprojects.framework.ai.etl.reader.CompositeReader;
import io.github.afgprojects.framework.ai.etl.reader.DocumentReader;
import io.github.afgprojects.framework.ai.etl.reader.DefaultEncodingDetector;
import io.github.afgprojects.framework.ai.etl.reader.EncodingDetector;
import io.github.afgprojects.framework.ai.etl.transformer.RecursiveCharacterTextSplitter;
import io.github.afgprojects.framework.ai.etl.transformer.TextSplitter;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * ETL 模块自动配置
 *
 * <p>自动配置 ETL 模块的核心组件：
 * <ul>
 *   <li>EncodingDetector - 编码检测器</li>
 *   <li>DocumentReader - 文档读取器（CompositeReader）</li>
 *   <li>TextSplitter - 文本切片器</li>
 * </ul>
 *
 * <p>配置示例：
 * <pre>{@code
 * afg:
 *   ai:
 *     etl:
 *       enabled: true
 *       splitter:
 *         chunk-size: 500
 *         chunk-overlap: 50
 * }</pre>
 *
 * @author afg-projects
 * @since 1.0.0
 */
@AutoConfiguration(after = AiAutoConfiguration.class)
@EnableConfigurationProperties(EtlProperties.class)
@ConditionalOnProperty(prefix = "afg.ai", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnProperty(prefix = "afg.ai.etl", name = "enabled", havingValue = "true", matchIfMissing = true)
public class EtlAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(EtlAutoConfiguration.class);

    /**
     * 配置编码检测器
     *
     * <p>用于自动检测文件编码，支持 UTF-8、GBK、GB2312 等常见编码。
     * 当容器中不存在 EncodingDetector Bean 时自动创建。
     */
    @Bean
    @ConditionalOnMissingBean(EncodingDetector.class)
    public EncodingDetector encodingDetector() {
        log.info("Creating default encoding detector");
        return new DefaultEncodingDetector();
    }

    /**
     * 配置文档读取器
     *
     * <p>使用 CompositeReader 作为默认实现，支持多种文档格式：
     * <ul>
     *   <li>TXT - 纯文本文件</li>
     *   <li>Markdown - Markdown 文档</li>
     *   <li>PDF - PDF 文档</li>
     * </ul>
     *
     * <p>当容器中不存在 DocumentReader Bean 时自动创建。
     */
    @Bean
    @ConditionalOnMissingBean(DocumentReader.class)
    public DocumentReader documentReader() {
        log.info("Creating composite document reader");
        return new CompositeReader();
    }

    /**
     * 配置文本切片器
     *
     * <p>使用 RecursiveCharacterTextSplitter 作为默认实现，
     * 按分隔符层级递归分割文本，尽量保持语义完整性。
     *
     * <p>支持中英文分隔符，优先按段落分割，再按句子分割，最后按字符分割。
     *
     * @param properties ETL 配置属性
     */
    @Bean
    @ConditionalOnMissingBean(TextSplitter.class)
    public TextSplitter textSplitter(@NonNull EtlProperties properties) {
        EtlProperties.SplitterConfig config = properties.getSplitter();
        int chunkSize = config.getChunkSize();
        int chunkOverlap = config.getChunkOverlap();

        log.info("Creating recursive character text splitter with chunkSize={}, chunkOverlap={}",
            chunkSize, chunkOverlap);

        return new RecursiveCharacterTextSplitter(chunkSize, chunkOverlap);
    }
}
