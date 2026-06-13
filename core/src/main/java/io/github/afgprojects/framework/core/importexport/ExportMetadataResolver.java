package io.github.afgprojects.framework.core.importexport;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.jspecify.annotations.Nullable;

import lombok.Data;

/**
 * 导出/导入元数据解析器。
 * <p>
 * 解析类上的 {@link ExcelSheet}/{@link CsvSheet} 注解和字段上的 {@link ExcelColumn} 注解，
 * 返回排序后的列元数据列表。解析结果会被缓存以提升性能。
 *
 * @since 1.0.0
 */
public class ExportMetadataResolver {

    private static final ConcurrentMap<Class<?>, SheetMetadata> CACHE = new ConcurrentHashMap<>();

    /**
     * 解析类的导出元数据。
     * <p>
     * 解析结果会被缓存，重复调用直接返回缓存结果。
     *
     * @param type 目标类型
     * @return Sheet 元数据，如果类没有导出注解则返回 null
     */
    @Nullable
    public static SheetMetadata resolve(Class<?> type) {
        return CACHE.computeIfAbsent(type, ExportMetadataResolver::doResolve);
    }

    @Nullable
    private static SheetMetadata doResolve(Class<?> type) {
        ExcelSheet excelSheet = type.getAnnotation(ExcelSheet.class);
        CsvSheet csvSheet = type.getAnnotation(CsvSheet.class);

        if (excelSheet == null && csvSheet == null) {
            return null;
        }

        SheetMetadata metadata = new SheetMetadata();

        if (excelSheet != null) {
            metadata.setSheetName(excelSheet.name());
            metadata.setSheetNo(excelSheet.sheetNo());
        } else {
            metadata.setSheetName(csvSheet.name());
            metadata.setSheetNo(0);
            metadata.setDelimiter(csvSheet.delimiter());
            metadata.setCharset(csvSheet.charset());
        }

        // 解析字段上的 @ExcelColumn 注解
        List<ColumnMetadata> columns = new ArrayList<>();
        for (Field field : type.getDeclaredFields()) {
            ExcelColumn column = field.getAnnotation(ExcelColumn.class);
            if (column != null) {
                ColumnMetadata col = new ColumnMetadata();
                col.setFieldName(field.getName());
                col.setTitle(column.name());
                col.setOrder(column.order());
                col.setFormat(column.format());
                col.setEnumConverter(column.enumConverter() != Void.class ? column.enumConverter() : null);
                col.setRequired(column.required());
                col.setFieldType(field.getType());
                columns.add(col);
            }
        }

        // 按 order 升序排列
        columns.sort(java.util.Comparator.comparingInt(ColumnMetadata::getOrder));
        metadata.setColumns(columns);

        return metadata;
    }

    /**
     * 清除缓存。
     */
    public static void clearCache() {
        CACHE.clear();
    }

    /**
     * Sheet 元数据。
     */
    @Data
    public static class SheetMetadata {

        private String sheetName;
        private int sheetNo;
        private char delimiter = ',';
        private String charset = "UTF-8";
        private List<ColumnMetadata> columns = new ArrayList<>();
    }

    /**
     * 列元数据。
     */
    @Data
    public static class ColumnMetadata {

        private String fieldName;
        private String title;
        private int order;
        private String format;
        private Class<?> enumConverter;
        private boolean required;
        private Class<?> fieldType;
    }
}
