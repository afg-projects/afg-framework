package io.github.afgprojects.framework.core.trace;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.afgprojects.framework.core.web.trace.TraceContext;
import io.micrometer.tracing.Tracer;
import jakarta.annotation.PostConstruct;

/**
 * TraceContext 初始化器
 * <p>
 * 将 Micrometer Tracer 注入到 TraceContext 工具类，
 * 使其能够在没有活跃 HTTP 请求时也能获取 traceId。
 * </p>
 *
 * <h3>功能特性</h3>
 * <ul>
 *   <li>自动注入 Tracer 到 TraceContext</li>
 *   <li>支持动态更新 Tracer</li>
 *   <li>提供 Tracer 可用性检查</li>
 * </ul>
 */
public class TraceContextInitializer {

    private static final Logger log = LoggerFactory.getLogger(TraceContextInitializer.class);

    private final @Nullable Tracer tracer;

    /**
     * 创建 TraceContext 初始化器
     *
     * @param tracer Micrometer Tracer（可为 null）
     */
    public TraceContextInitializer(@Nullable Tracer tracer) {
        this.tracer = tracer;
    }

    @PostConstruct
    public void init() {
        if (tracer != null) {
            TraceContext.setTracer(tracer);
            log.info("Micrometer Tracer initialized for TraceContext: {}", tracer.getClass().getSimpleName());
        } else {
            log.debug("No Micrometer Tracer available, TraceContext will use manual mode");
        }
    }
}
