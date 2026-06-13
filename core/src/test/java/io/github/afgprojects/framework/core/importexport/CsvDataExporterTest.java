package io.github.afgprojects.framework.core.importexport;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.api.importexport.CsvDataExporter;
import io.github.afgprojects.framework.core.importexport.CsvDataExporterTest.TestUserVO;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

@DisplayName("CsvDataExporter")
class CsvDataExporterTest {

    @ExcelSheet(name = "用户列表")
    public static class TestUserVO {

        @ExcelColumn(name = "用户名", order = 1)
        private String username;

        @ExcelColumn(name = "年龄", order = 2)
        private Integer age;

        @ExcelColumn(name = "邮箱", order = 3)
        private String email;

        public TestUserVO() {}

        public TestUserVO(String username, Integer age, String email) {
            this.username = username;
            this.age = age;
            this.email = email;
        }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public Integer getAge() { return age; }
        public void setAge(Integer age) { this.age = age; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    private final CsvDataExporter exporter = new CsvDataExporter();

    @Nested
    @DisplayName("getFormat")
    class GetFormat {

        @Test
        @DisplayName("返回 csv 格式标识")
        void shouldReturnCsvFormat() {
            assertThat(exporter.getFormat()).isEqualTo("csv");
        }
    }

    @Nested
    @DisplayName("export - 字节数组")
    class ExportBytes {

        @Test
        @DisplayName("正确导出 header 和数据行")
        void shouldExportHeaderAndDataRows() {
            List<TestUserVO> data = List.of(
                    new TestUserVO("张三", 25, "zhangsan@test.com"),
                    new TestUserVO("李四", 30, "lisi@test.com")
            );

            byte[] bytes = exporter.export(data, TestUserVO.class);
            String csv = new String(bytes, StandardCharsets.UTF_8);

            String[] lines = csv.split("\n");
            assertThat(lines[0].trim()).isEqualTo("用户名,年龄,邮箱");
            assertThat(lines[1].trim()).isEqualTo("张三,25,zhangsan@test.com");
            assertThat(lines[2].trim()).isEqualTo("李四,30,lisi@test.com");
        }

        @Test
        @DisplayName("空数据列表只输出 header")
        void shouldExportOnlyHeader_whenEmptyData() {
            List<TestUserVO> data = List.of();

            byte[] bytes = exporter.export(data, TestUserVO.class);
            String csv = new String(bytes, StandardCharsets.UTF_8);

            String[] lines = csv.split("\n");
            assertThat(lines).hasSize(1);
            assertThat(lines[0].trim()).isEqualTo("用户名,年龄,邮箱");
        }

        @Test
        @DisplayName("null 字段值输出为空字符串")
        void shouldExportEmptyString_whenNullField() {
            List<TestUserVO> data = List.of(
                    new TestUserVO("王五", null, null)
            );

            byte[] bytes = exporter.export(data, TestUserVO.class);
            String csv = new String(bytes, StandardCharsets.UTF_8);

            String[] lines = csv.split("\n");
            assertThat(lines[1].trim()).isEqualTo("王五,,");
        }
    }

    @Nested
    @DisplayName("escapeCsv")
    class EscapeCsv {

        @Test
        @DisplayName("普通字符串不需要转义")
        void shouldNotEscape_plainString() {
            assertThat(CsvDataExporter.escapeCsv("hello")).isEqualTo("hello");
        }

        @Test
        @DisplayName("包含逗号的字符串需要双引号包裹")
        void shouldEscape_stringWithComma() {
            assertThat(CsvDataExporter.escapeCsv("a,b")).isEqualTo("\"a,b\"");
        }

        @Test
        @DisplayName("包含引号的字符串需要双引号包裹且内部引号双写")
        void shouldEscape_stringWithQuote() {
            assertThat(CsvDataExporter.escapeCsv("say \"hi\"")).isEqualTo("\"say \"\"hi\"\"\"");
        }

        @Test
        @DisplayName("包含换行符的字符串需要双引号包裹")
        void shouldEscape_stringWithNewline() {
            assertThat(CsvDataExporter.escapeCsv("line1\nline2")).isEqualTo("\"line1\nline2\"");
        }

        @Test
        @DisplayName("null 值返回空字符串")
        void shouldReturnEmpty_forNull() {
            assertThat(CsvDataExporter.escapeCsv(null)).isEmpty();
        }
    }
}
