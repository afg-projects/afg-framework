package io.github.afgprojects.framework.core.importexport;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.api.importexport.CsvDataImporter;
import io.github.afgprojects.framework.core.api.importexport.ImportError;
import io.github.afgprojects.framework.core.api.importexport.ImportResult;
import io.github.afgprojects.framework.core.importexport.CsvDataImporterTest.TestUserVO;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@DisplayName("CsvDataImporter")
class CsvDataImporterTest {

    @CsvSheet(name = "用户列表")
    public static class TestUserVO {

        @ExcelColumn(name = "用户名", order = 1, required = true)
        private String username;

        @ExcelColumn(name = "年龄", order = 2)
        private Integer age;

        @ExcelColumn(name = "邮箱", order = 3)
        private String email;

        public TestUserVO() {}

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public Integer getAge() { return age; }
        public void setAge(Integer age) { this.age = age; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    private final CsvDataImporter importer = new CsvDataImporter();

    @Nested
    @DisplayName("getFormat")
    class GetFormat {

        @Test
        @DisplayName("返回 csv 格式标识")
        void shouldReturnCsvFormat() {
            assertThat(importer.getFormat()).isEqualTo("csv");
        }
    }

    @Nested
    @DisplayName("importAs")
    class ImportAs {

        @Test
        @DisplayName("正确导入 CSV 数据")
        void shouldImportCsvData() {
            String csv = "用户名,年龄,邮箱\n张三,25,zhangsan@test.com\n李四,30,lisi@test.com\n";
            ByteArrayInputStream input = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

            ImportResult<TestUserVO> result = importer.importAs(input, TestUserVO.class);

            assertThat(result.hasErrors()).isFalse();
            assertThat(result.getSuccessCount()).isEqualTo(2);
            assertThat(result.getData()).hasSize(2);
            assertThat(result.getData().get(0).getUsername()).isEqualTo("张三");
            assertThat(result.getData().get(0).getAge()).isEqualTo(25);
            assertThat(result.getData().get(1).getUsername()).isEqualTo("李四");
            assertThat(result.getData().get(1).getAge()).isEqualTo(30);
        }

        @Test
        @DisplayName("必填字段为空时收集错误")
        void shouldCollectError_whenRequiredFieldEmpty() {
            String csv = "用户名,年龄,邮箱\n,25,test@test.com\n";
            ByteArrayInputStream input = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

            ImportResult<TestUserVO> result = importer.importAs(input, TestUserVO.class);

            assertThat(result.hasErrors()).isTrue();
            assertThat(result.getFailureCount()).isEqualTo(1);
            assertThat(result.getErrors()).hasSize(1);
            ImportError error = result.getErrors().get(0);
            assertThat(error.getRow()).isEqualTo(1);
            assertThat(error.getField()).isEqualTo("username");
            assertThat(error.getMessage()).contains("必填字段不能为空");
        }

        @Test
        @DisplayName("空输入流返回空结果")
        void shouldReturnEmptyResult_forEmptyInput() {
            ByteArrayInputStream input = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));

            ImportResult<TestUserVO> result = importer.importAs(input, TestUserVO.class);

            assertThat(result.getTotalCount()).isEqualTo(0);
            assertThat(result.getSuccessCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("仅标题行返回空数据")
        void shouldReturnEmptyData_forHeaderOnly() {
            String csv = "用户名,年龄,邮箱\n";
            ByteArrayInputStream input = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

            ImportResult<TestUserVO> result = importer.importAs(input, TestUserVO.class);

            assertThat(result.getTotalCount()).isEqualTo(0);
            assertThat(result.getData()).isEmpty();
        }
    }

    @Nested
    @DisplayName("parseCsvLine")
    class ParseCsvLine {

        @Test
        @DisplayName("解析普通 CSV 行")
        void shouldParseSimpleLine() {
            List<String> result = CsvDataImporter.parseCsvLine("a,b,c", ',');

            assertThat(result).containsExactly("a", "b", "c");
        }

        @Test
        @DisplayName("解析含引号的 CSV 行")
        void shouldParseQuotedLine() {
            List<String> result = CsvDataImporter.parseCsvLine("\"a,b\",c", ',');

            assertThat(result).containsExactly("a,b", "c");
        }

        @Test
        @DisplayName("解析含双写引号的 CSV 行")
        void shouldParseDoubleQuotedLine() {
            List<String> result = CsvDataImporter.parseCsvLine("\"say \"\"hi\"\"\",c", ',');

            assertThat(result).containsExactly("say \"hi\"", "c");
        }
    }
}
