package io.github.afgprojects.framework.core.properties.tracing;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Data;

/**
 * Baggage 配置。
 */
@Data
public class AfgCoreTracingBaggageProperties {

    private boolean enabled = true;
    private List<String> remoteFields = List.of("tenantId", "userId", "traceId");
    private List<String> localFields = List.of();
    private Map<String, String> fieldMappings = new HashMap<>();
}
