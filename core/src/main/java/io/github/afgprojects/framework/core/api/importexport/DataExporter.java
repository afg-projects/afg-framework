package io.github.afgprojects.framework.core.api.importexport;

import java.io.OutputStream;
import java.util.List;

import org.jspecify.annotations.NonNull;

/**
 * 数据导出器 SPI 接口。
 * <p>
 * 定义统一的数据导出接口，支持将 Java 对象列表导出为指定格式。
 * CSV 默认实现由 Core 模块提供（{@link CsvDataExporter}），
 * Excel 实现由集成模块提供。
 *
 * <pre>{@code
 * @Autowired
 * private DataExporter csvExporter;  // format = "csv"
 *
 * // 导出到输出流
 * csvExporter.export(users, UserExportVO.class, response.getOutputStream());
 *
 * // 导出为字节数组
 * byte[] bytes = csvExporter.export(users, UserExportVO.class);
 * }</pre>
 *
 * @see DataImporter
 * @since 1.0.0
 */
public interface DataExporter {

    /**
     * 将数据导出到输出流。
     *
     * @param data         数据列表
     * @param type         数据类型（含导出注解）
     * @param outputStream 输出流
     * @param <T>          数据类型泛型
     */
    <T> void export(@NonNull List<T> data, @NonNull Class<T> type, @NonNull OutputStream outputStream);

    /**
     * 将数据导出为字节数组。
     *
     * @param data 数据列表
     * @param type 数据类型（含导出注解）
     * @param <T>  数据类型泛型
     * @return 导出数据的字节数组
     */
    <T> byte[] export(@NonNull List<T> data, @NonNull Class<T> type);

    /**
     * 获取导出器支持的格式标识。
     *
     * @return 格式标识（如 "csv"、"excel"）
     */
    String getFormat();
}
