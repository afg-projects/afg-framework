package io.github.afgprojects.framework.core.api.importexport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.NonNull;

import io.github.afgprojects.framework.core.importexport.ExportMetadataResolver;
import io.github.afgprojects.framework.core.importexport.ExportMetadataResolver.ColumnMetadata;
import io.github.afgprojects.framework.core.importexport.ExportMetadataResolver.SheetMetadata;

import lombok.extern.slf4j.Slf4j;

/**
 * CSV 数据导入器。
 * <p>
 * 使用纯 Java 实现 CSV 导入（无外部依赖），读取 CSV header 映射到注解定义的列，
 * 通过反射创建目标对象。支持必填校验和错误收集。
 *
 * @since 1.0.0
 */
@Slf4j
public class CsvDataImporter implements DataImporter {

    @Override
    public <T> ImportResult<T> importAs(@NonNull InputStream inputStream, @NonNull Class<T> type) {
        SheetMetadata metadata = ExportMetadataResolver.resolve(type);
        if (metadata == null) {
            log.warn("类型 {} 缺少 @ExcelSheet 或 @CsvSheet 注解，返回空结果", type.getName());
            return ImportResult.empty();
        }

        Charset charset = Charset.forName(metadata.getCharset());
        List<T> data = new ArrayList<>();
        List<ImportError> errors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, charset))) {
            // 读取 header 行
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return ImportResult.empty();
            }
            List<String> headers = parseCsvLine(headerLine, metadata.getDelimiter());

            // 建立 header → column 索引映射
            List<ColumnMetadata> columns = metadata.getColumns();
            int[] headerToColumnIndex = new int[headers.size()];
            for (int i = 0; i < headers.size(); i++) {
                headerToColumnIndex[i] = findColumnIndex(columns, headers.get(i));
            }

            // 逐行解析
            String line;
            int rowNum = 1; // 数据行号，从 1 开始（不含标题行）
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                List<String> values = parseCsvLine(line, metadata.getDelimiter());
                List<ImportError> rowErrors = new ArrayList<>();

                try {
                    T instance = type.getDeclaredConstructor().newInstance();

                    for (int colIdx = 0; colIdx < values.size() && colIdx < headerToColumnIndex.length; colIdx++) {
                        int columnIndex = headerToColumnIndex[colIdx];
                        if (columnIndex < 0) {
                            continue; // 跳过不匹配的列
                        }
                        ColumnMetadata column = columns.get(columnIndex);
                        String rawValue = values.get(colIdx);

                        // 必填校验
                        if (column.isRequired() && (rawValue == null || rawValue.trim().isEmpty())) {
                            rowErrors.add(ImportError.builder()
                                    .row(rowNum)
                                    .field(column.getFieldName())
                                    .message("必填字段不能为空")
                                    .value(rawValue)
                                    .build());
                            continue;
                        }

                        // 设置字段值
                        try {
                            Object convertedValue = convertValue(rawValue, column);
                            setFieldValue(instance, column, convertedValue);
                        } catch (Exception e) {
                            rowErrors.add(ImportError.builder()
                                    .row(rowNum)
                                    .field(column.getFieldName())
                                    .message("值转换失败: " + e.getMessage())
                                    .value(rawValue)
                                    .build());
                        }
                    }

                    if (rowErrors.isEmpty()) {
                        data.add(instance);
                    } else {
                        errors.addAll(rowErrors);
                    }
                } catch (Exception e) {
                    errors.add(ImportError.builder()
                            .row(rowNum)
                            .field(null)
                            .message("创建实例失败: " + e.getMessage())
                            .value(null)
                            .build());
                }

                rowNum++;
            }
        } catch (IOException e) {
            log.error("CSV 导入读取失败, type={}", type.getName(), e);
        }

        return ImportResult.of(data, errors);
    }

    @Override
    public String getFormat() {
        return "csv";
    }

    /**
     * 根据 header 标题查找对应列索引。
     */
    private int findColumnIndex(List<ColumnMetadata> columns, String header) {
        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).getTitle().equals(header)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 将字符串值转换为目标字段类型。
     */
    private Object convertValue(String rawValue, ColumnMetadata column) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return null;
        }

        Class<?> fieldType = column.getFieldType();

        if (fieldType == String.class) {
            return rawValue;
        }
        if (fieldType == Integer.class || fieldType == int.class) {
            return Integer.parseInt(rawValue.trim());
        }
        if (fieldType == Long.class || fieldType == long.class) {
            return Long.parseLong(rawValue.trim());
        }
        if (fieldType == Double.class || fieldType == double.class) {
            return Double.parseDouble(rawValue.trim());
        }
        if (fieldType == Float.class || fieldType == float.class) {
            return Float.parseFloat(rawValue.trim());
        }
        if (fieldType == Boolean.class || fieldType == boolean.class) {
            return Boolean.parseBoolean(rawValue.trim());
        }
        if (fieldType == java.math.BigDecimal.class) {
            return new java.math.BigDecimal(rawValue.trim());
        }

        // 默认返回原始字符串
        return rawValue;
    }

    /**
     * 通过反射设置字段值。
     */
    private <T> void setFieldValue(T instance, ColumnMetadata column, Object value) throws Exception {
        if (value == null) {
            return;
        }
        String setterName = "set" + capitalize(column.getFieldName());
        Method setter = instance.getClass().getMethod(setterName, column.getFieldType());
        setter.invoke(instance, value);
    }

    /**
     * 解析 CSV 行（支持引号内包含分隔符和换行符）。
     */
    public static List<String> parseCsvLine(String line, char delimiter) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (inQuotes) {
                if (c == '"') {
                    // 检查是否是转义引号（双引号）
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++; // 跳过下一个引号
                    } else {
                        inQuotes = false; // 结束引号
                    }
                } else {
                    current.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == delimiter) {
                    result.add(current.toString());
                    current.setLength(0);
                } else {
                    current.append(c);
                }
            }
        }

        result.add(current.toString());
        return result;
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}
