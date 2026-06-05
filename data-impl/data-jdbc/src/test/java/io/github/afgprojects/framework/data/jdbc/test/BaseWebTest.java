package io.github.afgprojects.framework.data.jdbc.test;

import io.github.afgprojects.framework.data.jdbc.JdbcDataTestConfiguration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.client.RestClient;

/**
 * Web 层测试基类
 *
 * <p>使用 RANDOM_PORT 启动嵌入式 Web 服务器进行端到端测试。
 * 继承 BasePostgresTest 共享 PostgreSQL 容器和 Spring 上下文缓存。
 *
 * <p>注意：Web 测试不能使用 @Transactional 自动回滚，
 * 因为 HTTP 请求在不同线程执行。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = JdbcDataTestConfiguration.class)
public abstract class BaseWebTest extends BasePostgresTest {

    @LocalServerPort
    protected int port;

    @Autowired
    protected RestClient.Builder restClientBuilder;

    protected RestClient restClient() {
        return restClientBuilder.baseUrl("http://localhost:" + port).build();
    }
}
