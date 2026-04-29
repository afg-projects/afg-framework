package io.github.afgprojects.framework.core.autoconfigure;

import jakarta.servlet.Servlet;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

import io.github.afgprojects.framework.core.web.context.RequestContextFilter;
import io.github.afgprojects.framework.core.web.exception.GlobalExceptionHandler;
import io.github.afgprojects.framework.core.web.trace.TraceContext;
import io.micrometer.tracing.Tracer;

/**
 * Web 自动配置类
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(Servlet.class)
public class WebAutoConfiguration {

    @Nullable @Autowired(required = false)
    private Tracer tracer;

    @Bean("afgRequestContextFilter")
    @ConditionalOnMissingBean
    public RequestContextFilter afgRequestContextFilter() {
        // 设置全局 Tracer 供 TraceContext 使用
        TraceContext.setTracer(tracer);
        return new RequestContextFilter(tracer);
    }

    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }
}
