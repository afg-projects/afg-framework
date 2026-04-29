package io.github.afgprojects.framework.starter;

import org.springframework.boot.autoconfigure.AutoConfiguration;

/**
 * AFG Platform Spring Boot Starter 自动配置类
 * <p>
 * 此 Starter 作为 AFG Platform 的聚合入口，引入后自动获得以下功能：
 * <ul>
 *   <li>核心配置：模块注册、配置中心</li>
 *   <li>Web 层：请求上下文、安全过滤器、API 版本管理</li>
 *   <li>HTTP 客户端：拦截器、熔断、重试</li>
 *   <li>可观测性：日志、指标、链路追踪、审计日志</li>
 *   <li>基础设施：缓存、分布式锁、健康检查、优雅关闭</li>
 *   <li>安全防护：XSS/SQL 注入防护、签名验证、限流</li>
 *   <li>数据访问：SQL 构建、JDBC 封装、多数据源</li>
 * </ul>
 * <p>
 * 可选模块（通过 Gradle optional 声明，按需引入）：
 * <ul>
 *   <li>afg-redis：Redis 集成（缓存、分布式锁、延迟队列、任务调度）</li>
 *   <li>afg-kafka：Kafka 事件发布</li>
 *   <li>afg-rabbitmq：RabbitMQ 事件发布</li>
 *   <li>afg-nacos/afg-apollo/afg-consul：配置中心</li>
 *   <li>afg-jdbc：审计日志数据库存储</li>
 * </ul>
 * <p>
 * 自动配置由各模块的 AutoConfiguration.imports 文件提供，
 * Spring Boot 会自动加载类路径上所有模块的自动配置。
 * <p>
 * 使用方式：
 * <pre>
 * // Maven
 * &lt;dependency&gt;
 *     &lt;groupId&gt;io.github.afgprojects&lt;/groupId&gt;
 *     &lt;artifactId&gt;spring-boot-starter&lt;/artifactId&gt;
 * &lt;/dependency&gt;
 *
 * // Gradle
 * implementation("io.github.afgprojects:spring-boot-starter")
 *
 * // 可选模块（按需引入）
 * implementation("io.github.afgprojects:afg-redis")
 * implementation("io.github.afgprojects:afg-kafka")
 * </pre>
 *
 * @since 1.0.0
 */
@AutoConfiguration
public class AfgStarterAutoConfiguration {
    // 自动配置由各模块的 AutoConfiguration.imports 文件提供
    // Spring Boot 会自动加载类路径上所有模块的自动配置
    // 此类作为 Starter 的标识，并提供文档说明
}
