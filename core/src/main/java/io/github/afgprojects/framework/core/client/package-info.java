/**
 * 声明式 HTTP 客户端包。
 *
 * <p>提供 Spring 6.1+ 声明式 HTTP 客户端的创建和管理能力。
 *
 * <p>核心类：
 * <ul>
 *   <li>{@link io.github.afgprojects.framework.core.client.HttpClientRegistry} - HTTP 客户端注册表</li>
 *   <li>{@link io.github.afgprojects.framework.core.client.TraceInterceptor} - 链路追踪拦截器，自动传递 traceId</li>
 * </ul>
 *
 * <p>使用示例：
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
 *
 * @since 1.0.0
 */
package io.github.afgprojects.framework.core.client;
