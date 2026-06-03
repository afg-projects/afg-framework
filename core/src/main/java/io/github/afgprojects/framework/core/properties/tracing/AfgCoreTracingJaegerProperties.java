package io.github.afgprojects.framework.core.properties.tracing;

import org.jspecify.annotations.Nullable;

import lombok.Data;

/**
 * Jaeger 配置。
 */
@Data
public class AfgCoreTracingJaegerProperties {

    private boolean enabled = false;
    private @Nullable String endpoint = "http://localhost:14268/api/traces";
    private @Nullable String otlpEndpoint;
    private boolean useOtlp = true;
    private int connectTimeout = 5000;
    private int readTimeout = 10000;
}
