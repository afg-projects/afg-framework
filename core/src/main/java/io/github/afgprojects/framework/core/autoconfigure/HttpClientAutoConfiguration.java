package io.github.afgprojects.framework.core.autoconfigure;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestClient;

import io.github.afgprojects.framework.core.client.AsyncResilienceInterceptor;
import io.github.afgprojects.framework.core.client.HttpClientProperties;
import io.github.afgprojects.framework.core.client.HttpClientRegistry;
import io.github.afgprojects.framework.core.client.ResilienceInterceptor;
import io.github.afgprojects.framework.core.client.TraceInterceptor;
import io.micrometer.tracing.Tracer;

/**
 * HTTP 客户端自动配置类
 *
 * <p>支持 Spring 6.1+ @HttpExchange 声明式 HTTP 客户端
 * <p>支持重试和熔断功能
 * <p>提供同步和异步两种弹性拦截器
 */
@AutoConfiguration
@ConditionalOnClass(RestClient.class)
@EnableConfigurationProperties(HttpClientProperties.class)
public class HttpClientAutoConfiguration {

    @Nullable @Autowired(required = false)
    private Tracer tracer;

    /**
     * 创建同步 RestClient 构建器
     */
    @Bean
    @ConditionalOnMissingBean
    public RestClient.Builder restClientBuilder(HttpClientProperties properties) {
        RestClient.Builder builder = RestClient.builder();

        // 添加 TraceInterceptor 用于传播链路追踪信息
        builder.requestInterceptor(new TraceInterceptor(tracer));

        // 添加 ResilienceInterceptor 用于重试和熔断
        if (properties.getRetry().isEnabled() || properties.getCircuitBreaker().isEnabled()) {
            builder.requestInterceptor(new ResilienceInterceptor(properties));
        }

        return builder;
    }

    /**
     * 创建异步弹性拦截器调度器
     */
    @Bean
    @ConditionalOnMissingBean
    public ScheduledExecutorService resilienceScheduler() {
        return Executors.newScheduledThreadPool(4, r -> {
            Thread thread = new Thread(r, "async-resilience-scheduler");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * 创建异步弹性拦截器
     * <p>
     * 用于非阻塞异步 HTTP 请求，支持重试和熔断
     */
    @Bean
    @ConditionalOnMissingBean
    public AsyncResilienceInterceptor asyncResilienceInterceptor(
            HttpClientProperties properties,
            ScheduledExecutorService resilienceScheduler) {
        return new AsyncResilienceInterceptor(properties, resilienceScheduler);
    }

    @Bean
    @ConditionalOnMissingBean
    public HttpClientRegistry httpClientRegistry(
            Environment environment, RestClient.Builder builder, HttpClientProperties properties) {
        return new HttpClientRegistry(environment, builder, tracer, properties);
    }
}
