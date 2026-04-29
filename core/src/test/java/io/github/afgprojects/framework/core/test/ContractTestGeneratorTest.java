package io.github.afgprojects.framework.core.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.afgprojects.framework.core.support.BaseUnitTest;

/**
 * ContractTestGenerator 测试
 */
class ContractTestGeneratorTest extends BaseUnitTest {

    @TempDir
    Path tempDir;

    private Path outputDir;

    @BeforeEach
    void setUp() throws Exception {
        outputDir = tempDir.resolve("contracts");
        Files.createDirectories(outputDir);
    }

    @AfterEach
    void tearDown() {
        // TempDir 会在测试后自动清理
    }

    @Test
    @DisplayName("创建构建器")
    void shouldCreateBuilder() {
        ContractTestGenerator generator = ContractTestGenerator.builder()
                .outputDir(outputDir.toString())
                .version("1.0.0")
                .baseUrl("http://localhost:8080")
                .build();

        assertThat(generator).isNotNull();
    }

    @Test
    @DisplayName("生成单个 Controller 的契约文件")
    void shouldGenerateContractForController() throws Exception {
        ContractTestGenerator generator = ContractTestGenerator.builder()
                .outputDir(outputDir.toString())
                .build();

        generator.generate(TestController.class);

        File contractFile = outputDir.resolve("test-api.json").toFile();
        assertThat(contractFile).exists();

        // 验证生成的契约内容
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode contract = objectMapper.readTree(contractFile);

        assertThat(contract.has("openapi")).isTrue();
        assertThat(contract.get("openapi").asText()).isEqualTo("3.0.0");
        assertThat(contract.has("info")).isTrue();
        assertThat(contract.has("paths")).isTrue();
    }

    @Test
    @DisplayName("生成契约到指定文件")
    void shouldGenerateContractToSpecificFile() throws Exception {
        ContractTestGenerator generator = ContractTestGenerator.builder()
                .outputDir(outputDir.toString())
                .build();

        String outputPath = outputDir.resolve("custom-contract.json").toString();
        generator.generate(TestController.class, outputPath);

        File contractFile = new File(outputPath);
        assertThat(contractFile).exists();
    }

    @Test
    @DisplayName("契约包含正确的 API 标题")
    void shouldContainCorrectApiTitle() throws Exception {
        ContractTestGenerator generator = ContractTestGenerator.builder()
                .outputDir(outputDir.toString())
                .build();

        generator.generate(TestController.class);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode contract = objectMapper.readTree(outputDir.resolve("test-api.json").toFile());

        JsonNode info = contract.get("info");
        assertThat(info.get("title").asText()).isEqualTo("TestController API");
    }

    @Test
    @DisplayName("契约包含版本号")
    void shouldContainVersion() throws Exception {
        ContractTestGenerator generator = ContractTestGenerator.builder()
                .outputDir(outputDir.toString())
                .version("2.0.0")
                .build();

        generator.generate(TestController.class);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode contract = objectMapper.readTree(outputDir.resolve("test-api.json").toFile());

        assertThat(contract.get("info").get("version").asText()).isEqualTo("2.0.0");
    }

    @Test
    @DisplayName("契约包含服务器信息")
    void shouldContainServerInfo() throws Exception {
        ContractTestGenerator generator = ContractTestGenerator.builder()
                .outputDir(outputDir.toString())
                .baseUrl("https://api.example.com")
                .build();

        generator.generate(TestController.class);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode contract = objectMapper.readTree(outputDir.resolve("test-api.json").toFile());

        JsonNode servers = contract.get("servers");
        assertThat(servers.isArray()).isTrue();
        assertThat(servers.get(0).get("url").asText()).isEqualTo("https://api.example.com");
    }

    @Test
    @DisplayName("契约包含路径定义")
    void shouldContainPaths() throws Exception {
        ContractTestGenerator generator = ContractTestGenerator.builder()
                .outputDir(outputDir.toString())
                .build();

        generator.generate(TestController.class);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode contract = objectMapper.readTree(outputDir.resolve("test-api.json").toFile());

        JsonNode paths = contract.get("paths");
        assertThat(paths.has("/test/hello")).isTrue();
        assertThat(paths.has("/test/echo")).isTrue();
    }

    @Test
    @DisplayName("静态方法生成契约")
    void shouldGenerateContractStatically() throws Exception {
        String outputPath = outputDir.resolve("static-contract.json").toString();
        ContractTestGenerator.generateContract(TestController.class, outputPath);

        File contractFile = new File(outputPath);
        assertThat(contractFile).exists();
    }

    @Test
    @DisplayName("批量生成契约")
    void shouldGenerateContractsInBatch() throws Exception {
        ContractTestGenerator.generateContracts(outputDir.toString(), TestController.class, AnotherController.class);

        assertThat(outputDir.resolve("test-api.json").toFile()).exists();
        assertThat(outputDir.resolve("another-api.json").toFile()).exists();
    }

    @Test
    @DisplayName("契约包含 GET 方法")
    void shouldContainGetMethod() throws Exception {
        ContractTestGenerator generator = ContractTestGenerator.builder()
                .outputDir(outputDir.toString())
                .build();

        generator.generate(TestController.class);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode contract = objectMapper.readTree(outputDir.resolve("test-api.json").toFile());

        JsonNode helloPath = contract.get("paths").get("/test/hello");
        assertThat(helloPath.has("get")).isTrue();
    }

    @Test
    @DisplayName("契约包含 POST 方法")
    void shouldContainPostMethod() throws Exception {
        ContractTestGenerator generator = ContractTestGenerator.builder()
                .outputDir(outputDir.toString())
                .build();

        generator.generate(TestController.class);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode contract = objectMapper.readTree(outputDir.resolve("test-api.json").toFile());

        JsonNode echoPath = contract.get("paths").get("/test/echo");
        assertThat(echoPath.has("post")).isTrue();
    }

    /**
     * 测试 Controller
     */
    @RestController
    @RequestMapping("/test")
    static class TestController {

        @GetMapping("/hello")
        public String hello() {
            return "Hello, World!";
        }

        @PostMapping("/echo")
        public TestRequest echo(@RequestBody TestRequest request) {
            return request;
        }
    }

    /**
     * 另一个测试 Controller
     */
    @RestController
    @RequestMapping("/another")
    static class AnotherController {

        @GetMapping("/status")
        public String status() {
            return "OK";
        }
    }

    /**
     * 测试请求对象
     */
    static class TestRequest {
        private String name;
        private int value;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }
    }
}