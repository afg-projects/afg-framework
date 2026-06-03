package io.github.afgprojects.framework.ai.core;

import io.github.afgprojects.framework.data.core.DataManager;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * AI Web 层集成测试基类
 *
 * <p>使用真实的 MySQL 容器，启动完整的 Spring Boot Web 上下文，
 * 提供 RestClient 用于 API 端到端测试。
 *
 * <p>注意：不要在继承类上使用 @Transactional，因为 HTTP 请求在不同线程执行，
 * 事务不会共享。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
public abstract class AbstractAiWebTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
        .withDatabaseName("ai_test")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);
    }

    @LocalServerPort
    int port;

    @Autowired
    DataManager dataManager;

    @Autowired
    RestClient.Builder restClientBuilder;

    private RestClient restClient;

    /**
     * 获取预配置的 RestClient，baseUrl 指向本地服务器 + AI 模块 context path。
     *
     * <p>AI 模块的 context path 为 /ai-api（由 @AfgModuleAnnotation 配置）。
     */
    protected RestClient restClient() {
        if (restClient == null) {
            restClient = restClientBuilder
                .baseUrl("http://localhost:" + port + "/ai-api")
                .build();
        }
        return restClient;
    }

    /**
     * 检查本地 Ollama 是否可用。
     *
     * <p>在 @BeforeAll 或 AI 依赖测试开头调用，
     * 当 Ollama 未运行时自动跳过测试。
     */
    protected static boolean isOllamaAvailable() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:11434/api/tags"))
                .GET()
                .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 假设 Ollama 可用，否则跳过当前测试类。
     *
     * <p>在 @BeforeAll 中调用：
     * <pre>{@code
     * @BeforeAll
     * static void requireOllama() {
     *     assumeOllamaAvailable();
     * }
     * }</pre>
     */
    protected static void assumeOllamaAvailable() {
        assumeThat(isOllamaAvailable())
            .withFailMessage("Ollama not available at localhost:11434, skipping AI-dependent test")
            .isTrue();
    }
}
