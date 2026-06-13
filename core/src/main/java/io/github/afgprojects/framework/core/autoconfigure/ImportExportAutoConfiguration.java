package io.github.afgprojects.framework.core.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import io.github.afgprojects.framework.core.api.importexport.CsvDataExporter;
import io.github.afgprojects.framework.core.api.importexport.CsvDataImporter;
import io.github.afgprojects.framework.core.api.importexport.DataExporter;
import io.github.afgprojects.framework.core.api.importexport.DataImporter;
import io.github.afgprojects.framework.core.api.importexport.NoOpDataExporter;
import io.github.afgprojects.framework.core.api.importexport.NoOpDataImporter;
import io.github.afgprojects.framework.core.config.AfgCoreProperties;

/**
 * 导入导出自动配置。
 * <p>
 * 自动配置数据导入导出功能，包括：
 * <ul>
 *   <li>{@link CsvDataExporter} — CSV 导出器（默认实现）</li>
 *   <li>{@link CsvDataImporter} — CSV 导入器（默认实现）</li>
 *   <li>{@link NoOpDataExporter} / {@link NoOpDataImporter} — NoOp 降级实现</li>
 * </ul>
 * Excel 实现由集成模块提供，通过 {@code @ConditionalOnMissingBean} 自动升级。
 * </p>
 * <p>
 * 配置示例：
 * <pre>
 * afg:
 *   core:
 *     import-export:
 *       enabled: true
 *       default-format: csv
 *       default-charset: UTF-8
 *       max-import-rows: 10000
 * </pre>
 * </p>
 *
 * @since 1.0.0
 */
@AutoConfiguration(after = AfgAutoConfiguration.class)
@ConditionalOnProperty(prefix = "afg.core.import-export", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(AfgCoreProperties.class)
public class ImportExportAutoConfiguration {

    /**
     * CSV 数据导出器。
     * <p>
     * 使用纯 Java 实现 CSV 导出，无外部依赖。
     * Excel 导出器由集成模块提供，通过 {@code @ConditionalOnMissingBean} 自动升级。
     *
     * @return CSV 导出器实例
     */
    @Bean
    @ConditionalOnMissingBean(DataExporter.class)
    public DataExporter csvDataExporter() {
        return new CsvDataExporter();
    }

    /**
     * CSV 数据导入器。
     * <p>
     * 使用纯 Java 实现 CSV 导入，无外部依赖。
     * Excel 导入器由集成模块提供，通过 {@code @ConditionalOnMissingBean} 自动升级。
     *
     * @return CSV 导入器实例
     */
    @Bean
    @ConditionalOnMissingBean(DataImporter.class)
    public DataImporter csvDataImporter() {
        return new CsvDataImporter();
    }

    /**
     * NoOp 数据导出器降级实现。
     * <p>
     * 当 CSV 导出器也不满足条件时使用，所有导出操作均为空操作。
     *
     * @return NoOp 导出器实例
     */
    @Bean
    @ConditionalOnMissingBean(DataExporter.class)
    public DataExporter noOpDataExporter() {
        return new NoOpDataExporter();
    }

    /**
     * NoOp 数据导入器降级实现。
     * <p>
     * 当 CSV 导入器也不满足条件时使用，所有导入操作返回空结果。
     *
     * @return NoOp 导入器实例
     */
    @Bean
    @ConditionalOnMissingBean(DataImporter.class)
    public DataImporter noOpDataImporter() {
        return new NoOpDataImporter();
    }
}
