package io.github.afgprojects.framework.core.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * 虚拟线程配置属性
 *
 * <p>用于配置 Java 25 虚拟线程支持，提升并发性能
 * <p>配置前缀: afg.virtual-thread
 *
 * <h3>与 Spring Boot 配置的关系</h3>
 * <p>建议与 Spring Boot 的虚拟线程配置保持一致：
 * <pre>
 * spring:
 *   threads:
 *     virtual:
 *       enabled: true  # Spring Boot 原生配置
 *
 * afg:
 *   virtual-thread:
 *     enabled: true    # AFG 配置（应与 spring.threads.virtual.enabled 保持一致）
 *     name-prefix: afg-vt-
 * </pre>
 *
 * <p>Spring Boot 配置控制：
 * <ul>
 *   <li>TOMCAT - HTTP 请求处理使用虚拟线程</li>
 *   <li>ASYNC - @Async 注解使用虚拟线程</li>
 *   <li>EXECUTOR - 异步任务使用虚拟线程</li>
 * </ul>
 *
 * <p>AFG 配置提供：
 * <ul>
 *   <li>自定义线程名称前缀</li>
 *   <li>通用虚拟线程执行器（afgVirtualThreadExecutor）</li>
 * </ul>
 */
@Data
@ConfigurationProperties(prefix = "afg.virtual-thread")
public class VirtualThreadProperties {

    /**
     * 是否启用虚拟线程
     * <p>
     * 建议与 {@code spring.threads.virtual.enabled} 保持一致
     * <p>
     * 启用后，将提供自定义虚拟线程执行器
     */
    private boolean enabled = true;

    /**
     * 虚拟线程名前缀
     * <p>
     * 用于调试和监控时识别虚拟线程
     */
    private String namePrefix = "afg-vt-";
}
