package io.github.afgprojects.framework.core.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import io.micrometer.tracing.Tracer;

/**
 * HTTP 客户端工厂
 * 用于创建 Spring 6.1+ @HttpExchange 声明式 HTTP 客户端
 *
 * <p>使用示例:
 * <pre>{@code
 * @HttpExchange("/api")
 * public interface UserClient {
 *     @GetExchange("/users/{id}")
 *     User getUser(@PathVariable String id);
 * }
 *
 * @Configuration
 * public class HttpClientsConfig {
 *     @Bean
 *     public UserClient userClient(HttpClientRegistry registry) {
 *         return registry.createClient(UserClient.class, "https://api.example.com");
 *     }
 * }
 * }</pre>
 */
public class HttpClientRegistry {

    private final Map<String, Object> clients = new ConcurrentHashMap<>();
    private final Environment environment;
    private final RestClient.Builder restClientBuilder;
    private final @Nullable Tracer tracer;
    private final @Nullable HttpClientProperties properties;

    /**
     * 构造函数（无弹性配置）
     *
     * @param environment         Spring 环境
     * @param restClientBuilder   RestClient 构建器
     * @param tracer              链路追踪器（可选）
     */
    public HttpClientRegistry(Environment environment, RestClient.Builder restClientBuilder, @Nullable Tracer tracer) {
        this(environment, restClientBuilder, tracer, null);
    }

    /**
     * 构造函数（完整配置）
     *
     * @param environment         Spring 环境
     * @param restClientBuilder   RestClient 构建器
     * @param tracer              链路追踪器（可选）
     * @param properties          HTTP 客户端配置（可选，用于重试和熔断）
     */
    public HttpClientRegistry(
            Environment environment,
            RestClient.Builder restClientBuilder,
            @Nullable Tracer tracer,
            @Nullable HttpClientProperties properties) {
        this.environment = environment;
        this.restClientBuilder = restClientBuilder;
        this.tracer = tracer;
        this.properties = properties;
    }

    /**
     * 创建 HTTP 客户端实例
     *
     * @param clientInterface 客户端接口（需使用 @HttpExchange 注解）
     * @param baseUrl         基础 URL，支持配置占位符如 ${api.user.url}
     * @param <T>             客户端类型
     * @return 客户端实例
     */
    @NonNull public <T> T createClient(@NonNull Class<T> clientInterface, @NonNull String baseUrl) {
        String resolvedUrl = environment.resolvePlaceholders(baseUrl);

        RestClient.Builder builder = restClientBuilder
                .baseUrl(resolvedUrl)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .requestInterceptor(new TraceInterceptor(tracer));

        // 添加 ResilienceInterceptor 用于重试和熔断（如果配置启用）
        if (properties != null
                && (properties.getRetry().isEnabled() || properties.getCircuitBreaker().isEnabled())) {
            builder.requestInterceptor(new ResilienceInterceptor(properties));
        }

        RestClient restClient = builder.build();

        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
                .build();

        return factory.createClient(clientInterface);
    }

    /**
     * 创建并注册 HTTP 客户端
     *
     * @param name            客户端名称（用于后续查找）
     * @param clientInterface 客户端接口
     * @param baseUrl         基础 URL
     * @param <T>             客户端类型
     * @return 客户端实例
     */
    @NonNull public <T> T register(@NonNull String name, @NonNull Class<T> clientInterface, @NonNull String baseUrl) {
        T client = createClient(clientInterface, baseUrl);
        clients.put(name, client);
        return client;
    }

    /**
     * 获取已注册的 HTTP 客户端
     *
     * @param name  客户端名称
     * @param clazz 客户端接口类型
     * @param <T>   客户端类型
     * @return 客户端实例，如果不存在则返回 null
     */
    @SuppressWarnings("unchecked")
    @Nullable public <T> T getClient(@NonNull String name, @NonNull Class<T> clazz) {
        Object client = clients.get(name);
        if (client == null) {
            return null;
        }
        if (!clazz.isInstance(client)) {
            throw new ClassCastException(
                    "HTTP client '" + name + "' is " + client.getClass().getName() + ", not " + clazz.getName());
        }
        return (T) client;
    }

    /**
     * 检查客户端是否存在
     *
     * @param name 客户端名称
     * @return 如果存在返回 true
     */
    public boolean hasClient(@NonNull String name) {
        return clients.containsKey(name);
    }
}
