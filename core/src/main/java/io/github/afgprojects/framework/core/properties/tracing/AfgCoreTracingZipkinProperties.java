package io.github.afgprojects.framework.core.properties.tracing;

import lombok.Data;

/**
 * Zipkin 配置。
 */
@Data
public class AfgCoreTracingZipkinProperties {

    private boolean enabled = false;
    private String endpoint = "http://localhost:9411/api/v2/spans";
    private int connectTimeout = 5000;
    private int readTimeout = 10000;
    private boolean compressionEnabled = true;
    private int sendInterval = 5000;
}
