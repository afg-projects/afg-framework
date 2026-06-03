package io.github.afgprojects.framework.core.properties.logging;

import lombok.Data;

/**
 * MDC 配置。
 */
@Data
public class AfgCoreLoggingMdcProperties {

    private boolean enabled = true;
    private String[] fields = {"traceId", "tenantId", "userId", "requestPath"};
}
