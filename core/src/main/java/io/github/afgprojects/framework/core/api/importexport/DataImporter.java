package io.github.afgprojects.framework.core.api.importexport;

import java.io.InputStream;
import java.util.List;

import org.jspecify.annotations.NonNull;

/**
 * 数据导入器 SPI 接口。
 * <p>
 * 定义统一的数据导入接口，支持从输入流读取数据并解析为 Java 对象列表。
 * CSV 默认实现由 Core 模块提供（{@link CsvDataImporter}），
 * Excel 实现由集成模块提供。
 *
 * <pre>{@code
 * @Autowired
 * private DataImporter csvImporter;  // format = "csv"
 *
 * ImportResult<UserImportVO> result = csvImporter.importAs(inputStream, UserImportVO.class);
 * if (result.hasErrors()) {
 *     result.getErrors().forEach(err ->
 *         log.warn("第 {} 行字段 {} 错误: {}", err.getRow(), err.getField(), err.getMessage()));
 * }
 * }</pre>
 *
 * @see DataExporter
 * @see ImportResult
 * @since 1.0.0
 */
public interface DataImporter {

    /**
     * 从输入流导入数据。
     *
     * @param inputStream 输入流
     * @param type        目标类型
     * @param <T>         目标类型泛型
     * @return 导入结果（含成功数据和错误信息）
     */
    <T> ImportResult<T> importAs(@NonNull InputStream inputStream, @NonNull Class<T> type);

    /**
     * 获取导入器支持的格式标识。
     *
     * @return 格式标识（如 "csv"、"excel"）
     */
    String getFormat();
}
