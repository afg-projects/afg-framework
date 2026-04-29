package io.github.afgprojects.framework.core.trace;

import jakarta.servlet.Servlet;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import io.micrometer.tracing.Tracer;

/**
 * 追踪自动配置类
 * <p>
 * 自动配置分布式追踪相关的组件。
 * </p>
 *
 * <h3>配置属性</h3>
 * <pre>
 * afg:
 *   tracing:
 *     enabled: true
 *     annotations:
 *       enabled: true
 *     sampling:
 *       strategy: probability
 *       probability: 1.0
 *     baggage:
 *       enabled: true
 *       remote-fields: tenantId,userId,traceId
 *     # Zipkin 配置
 *     zipkin:
 *       enabled: true
 *       endpoint: http://localhost:9411/api/v2/spans
 *     # Jaeger 配置
 *     jaeger:
 *       enabled: false
 *       otlp-endpoint: http://localhost:4317
 * </pre>
 *
 * <h3>追踪后端选择</h3>
 * <p>
 * 项目支持两种追踪桥接器：
 * <ul>
 *   <li><b>Brave</b> - 兼容 Zipkin，推荐用于 Zipkin 后端</li>
 *   <li><b>OpenTelemetry</b> - 推荐用于 Jaeger、OTLP 后端</li>
 * </ul>
 * </p>
 *
 * <h3>依赖配置</h3>
 * <pre>
 * // Gradle - 使用 Brave + Zipkin
 * implementation("io.micrometer:micrometer-tracing-bridge-brave")
 * implementation("io.zipkin.reporter2:zipkin-reporter-brave")
 *
 * // Gradle - 使用 OpenTelemetry + Jaeger
 * implementation("io.micrometer:micrometer-tracing-bridge-otel")
 * implementation("io.opentelemetry:opentelemetry-exporter-zipkin")
 * </pre>
 */
@AutoConfiguration
@ConditionalOnClass(Tracer.class)
@EnableConfigurationProperties(TracingProperties.class)
@ConditionalOnProperty(prefix = "afg.tracing", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TracingAutoConfiguration {

    @Nullable @Autowired(required = false)
    private Tracer tracer;

    /**
     * 配置追踪采样器
     *
     * @param properties 追踪配置属性
     * @return 追踪采样器
     */
    @Bean
    @ConditionalOnMissingBean
    public TracingSampler tracingSampler(TracingProperties properties) {
        return new TracingSampler(properties.getSampling());
    }

    /**
     * 配置追踪切面
     *
     * @param properties 追踪配置属性
     * @return 追踪切面
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "afg.tracing.annotations", name = "enabled", havingValue = "true", matchIfMissing = true)
    public TracedAspect tracedAspect(TracingProperties properties) {
        return new TracedAspect(tracer, properties);
    }

    /**
     * 配置增强的 TraceInterceptor
     *
     * @param properties 追踪配置属性
     * @param sampler    追踪采样器
     * @return 增强的 TraceInterceptor
     */
    @Bean("afgEnhancedTraceInterceptor")
    @ConditionalOnMissingBean(EnhancedTraceInterceptor.class)
    public EnhancedTraceInterceptor enhancedTraceInterceptor(TracingProperties properties, TracingSampler sampler) {
        return new EnhancedTraceInterceptor(tracer, properties, sampler);
    }

    /**
     * 配置 Baggage 拦截器（用于接收端）
     *
     * @return Baggage 拦截器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(Servlet.class)
    @ConditionalOnProperty(prefix = "afg.tracing.baggage", name = "enabled", havingValue = "true", matchIfMissing = true)
    public BaggageInterceptor baggageInterceptor() {
        return new BaggageInterceptor();
    }

    /**
     * 配置 TraceContext 初始化器
     * <p>
     * 将全局 Tracer 注入到 TraceContext 工具类
     * </p>
     *
     * @return TraceContext 初始化器
     */
    @Bean
    @ConditionalOnMissingBean
    public TraceContextInitializer traceContextInitializer() {
        return new TraceContextInitializer(tracer);
    }
}