package io.github.afgprojects.framework.core.importexport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.importexport.ExportMetadataResolver.ColumnMetadata;
import io.github.afgprojects.framework.core.importexport.ExportMetadataResolver.SheetMetadata;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ExportMetadataResolver")
class ExportMetadataResolverTest {

    @ExcelSheet(name = "测试Sheet", sheetNo = 1)
    static class TestExportVO {

        @ExcelColumn(name = "用户名", order = 2)
        private String username;

        @ExcelColumn(name = "年龄", order = 1)
        private Integer age;

        @ExcelColumn(name = "邮箱", order = 3, required = true)
        private String email;

        @ExcelColumn(name = "状态", order = 4, enumConverter = TestStatus.class)
        private Integer status;
    }

    @CsvSheet(name = "测试CSV", delimiter = ';', charset = "GBK")
    static class TestCsvVO {

        @ExcelColumn(name = "姓名", order = 1)
        private String name;

        @ExcelColumn(name = "分数", order = 2, format = "0.00")
        private Double score;
    }

    static class NoAnnotationVO {

        @ExcelColumn(name = "字段", order = 1)
        private String field;
    }

    enum TestStatus {
        ACTIVE(1, "激活"),
        INACTIVE(0, "停用");

        private final int code;
        private final String label;

        TestStatus(int code, String label) {
            this.code = code;
            this.label = label;
        }

        public int getCode() { return code; }
        public String getLabel() { return label; }
    }

    @BeforeEach
    void setUp() {
        ExportMetadataResolver.clearCache();
    }

    @Nested
    @DisplayName("resolve - ExcelSheet 注解")
    class ExcelSheetResolve {

        @Test
        @DisplayName("解析 @ExcelSheet 注解的类")
        void shouldResolveExcelSheetAnnotation() {
            SheetMetadata metadata = ExportMetadataResolver.resolve(TestExportVO.class);

            assertThat(metadata).isNotNull();
            assertThat(metadata.getSheetName()).isEqualTo("测试Sheet");
            assertThat(metadata.getSheetNo()).isEqualTo(1);
            assertThat(metadata.getDelimiter()).isEqualTo(',');
            assertThat(metadata.getCharset()).isEqualTo("UTF-8");
        }

        @Test
        @DisplayName("按 order 升序排列列")
        void shouldSortColumnsByOrder() {
            SheetMetadata metadata = ExportMetadataResolver.resolve(TestExportVO.class);

            assertThat(metadata.getColumns()).hasSize(4);
            assertThat(metadata.getColumns().get(0).getTitle()).isEqualTo("年龄");
            assertThat(metadata.getColumns().get(1).getTitle()).isEqualTo("用户名");
            assertThat(metadata.getColumns().get(2).getTitle()).isEqualTo("邮箱");
            assertThat(metadata.getColumns().get(3).getTitle()).isEqualTo("状态");
        }

        @Test
        @DisplayName("解析列的 required 和 enumConverter 属性")
        void shouldParseColumnAttributes() {
            SheetMetadata metadata = ExportMetadataResolver.resolve(TestExportVO.class);

            ColumnMetadata emailColumn = metadata.getColumns().stream()
                    .filter(c -> c.getTitle().equals("邮箱"))
                    .findFirst()
                    .orElseThrow();

            assertThat(emailColumn.isRequired()).isTrue();

            ColumnMetadata statusColumn = metadata.getColumns().stream()
                    .filter(c -> c.getTitle().equals("状态"))
                    .findFirst()
                    .orElseThrow();

            assertThat(statusColumn.getEnumConverter()).isEqualTo(TestStatus.class);
        }
    }

    @Nested
    @DisplayName("resolve - CsvSheet 注解")
    class CsvSheetResolve {

        @Test
        @DisplayName("解析 @CsvSheet 注解的类")
        void shouldResolveCsvSheetAnnotation() {
            SheetMetadata metadata = ExportMetadataResolver.resolve(TestCsvVO.class);

            assertThat(metadata).isNotNull();
            assertThat(metadata.getSheetName()).isEqualTo("测试CSV");
            assertThat(metadata.getDelimiter()).isEqualTo(';');
            assertThat(metadata.getCharset()).isEqualTo("GBK");
        }
    }

    @Nested
    @DisplayName("resolve - 无注解")
    class NoAnnotationResolve {

        @Test
        @DisplayName("没有导出注解的类返回 null")
        void shouldReturnNullForClassWithoutAnnotation() {
            SheetMetadata metadata = ExportMetadataResolver.resolve(NoAnnotationVO.class);

            assertThat(metadata).isNull();
        }
    }

    @Nested
    @DisplayName("缓存")
    class CacheBehavior {

        @Test
        @DisplayName("重复调用返回缓存结果")
        void shouldReturnCachedResult() {
            SheetMetadata first = ExportMetadataResolver.resolve(TestExportVO.class);
            SheetMetadata second = ExportMetadataResolver.resolve(TestExportVO.class);

            assertThat(first).isSameAs(second);
        }

        @Test
        @DisplayName("清除缓存后重新解析")
        void shouldReResolveAfterCacheClear() {
            SheetMetadata first = ExportMetadataResolver.resolve(TestExportVO.class);
            ExportMetadataResolver.clearCache();
            SheetMetadata second = ExportMetadataResolver.resolve(TestExportVO.class);

            assertThat(first).isNotSameAs(second);
            assertThat(first.getSheetName()).isEqualTo(second.getSheetName());
        }
    }
}
