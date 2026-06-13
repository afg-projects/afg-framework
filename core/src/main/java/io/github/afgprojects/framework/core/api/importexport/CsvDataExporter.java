package io.github.afgprojects.framework.core.api.importexport;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.List;

import org.jspecify.annotations.NonNull;

import io.github.afgprojects.framework.core.importexport.ExportMetadataResolver;
import io.github.afgprojects.framework.core.importexport.ExportMetadataResolver.ColumnMetadata;
import io.github.afgprojects.framework.core.importexport.ExportMetadataResolver.SheetMetadata;

import lombok.extern.slf4j.Slf4j;

/**
 * CSV 数据导出器。
 * <p>
 * 使用纯 Java 实现 CSV 导出（无外部依赖），读取 {@code @ExcelColumn} / {@code @CsvSheet} 注解，
 * 写入 header 行 + 数据行。支持自定义分隔符和字符编码。
 *
 * @since 1.0.0
 */
@Slf4j
public class CsvDataExporter implements DataExporter {

    @Override
    public <T> void export(@NonNull List<T> data, @NonNull Class<T> type,
                           @NonNull OutputStream outputStream) {
        SheetMetadata metadata = ExportMetadataResolver.resolve(type);
        if (metadata == null) {
            log.warn("类型 {} 缺少 @ExcelSheet 或 @CsvSheet 注解，跳过导出", type.getName());
            return;
        }

        Charset charset = Charset.forName(metadata.getCharset());
        try (Writer writer = new OutputStreamWriter(outputStream, charset)) {
            writeHeader(writer, metadata);
            writeData(writer, data, metadata);
            writer.flush();
        } catch (IOException e) {
            log.error("CSV 导出写入失败, type={}", type.getName(), e);
        }
    }

    @Override
    public <T> byte[] export(@NonNull List<T> data, @NonNull Class<T> type) {
        SheetMetadata metadata = ExportMetadataResolver.resolve(type);
        if (metadata == null) {
            log.warn("类型 {} 缺少 @ExcelSheet 或 @CsvSheet 注解，返回空数据", type.getName());
            return new byte[0];
        }

        StringBuilder sb = new StringBuilder();
        appendHeader(sb, metadata);
        appendData(sb, data, metadata);
        Charset charset = Charset.forName(metadata.getCharset());
        return sb.toString().getBytes(charset);
    }

    @Override
    public String getFormat() {
        return "csv";
    }

    private void writeHeader(Writer writer, SheetMetadata metadata) throws IOException {
        List<ColumnMetadata> columns = metadata.getColumns();
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) {
                writer.write(metadata.getDelimiter());
            }
            writer.write(escapeCsv(columns.get(i).getTitle()));
        }
        writer.write(System.lineSeparator());
    }

    private <T> void writeData(Writer writer, List<T> data, SheetMetadata metadata)
            throws IOException {
        for (T item : data) {
            List<ColumnMetadata> columns = metadata.getColumns();
            for (int i = 0; i < columns.size(); i++) {
                if (i > 0) {
                    writer.write(metadata.getDelimiter());
                }
                Object value = getFieldValue(item, columns.get(i));
                writer.write(escapeCsv(formatValue(value, columns.get(i))));
            }
            writer.write(System.lineSeparator());
        }
    }

    private void appendHeader(StringBuilder sb, SheetMetadata metadata) {
        List<ColumnMetadata> columns = metadata.getColumns();
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) {
                sb.append(metadata.getDelimiter());
            }
            sb.append(escapeCsv(columns.get(i).getTitle()));
        }
        sb.append(System.lineSeparator());
    }

    private <T> void appendData(StringBuilder sb, List<T> data, SheetMetadata metadata) {
        for (T item : data) {
            List<ColumnMetadata> columns = metadata.getColumns();
            for (int i = 0; i < columns.size(); i++) {
                if (i > 0) {
                    sb.append(metadata.getDelimiter());
                }
                Object value = getFieldValue(item, columns.get(i));
                sb.append(escapeCsv(formatValue(value, columns.get(i))));
            }
            sb.append(System.lineSeparator());
        }
    }

    private Object getFieldValue(Object item, ColumnMetadata column) {
        try {
            String getterName = "get" + capitalize(column.getFieldName());
            Method getter = item.getClass().getMethod(getterName);
            return getter.invoke(item);
        } catch (NoSuchMethodException e) {
            // 尝试 is 前缀（布尔类型）
            try {
                String getterName = "is" + capitalize(column.getFieldName());
                Method getter = item.getClass().getMethod(getterName);
                return getter.invoke(item);
            } catch (Exception ex) {
                log.debug("无法获取字段 {} 的值: {}", column.getFieldName(), ex.getMessage());
                return null;
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            log.debug("无法获取字段 {} 的值: {}", column.getFieldName(), e.getMessage());
            return null;
        }
    }

    private String formatValue(Object value, ColumnMetadata column) {
        if (value == null) {
            return "";
        }
        return String.valueOf(value);
    }

    /**
     * CSV 值转义：包含逗号、引号、换行符时用双引号包裹，内部引号双写。
     */
    public static String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}
