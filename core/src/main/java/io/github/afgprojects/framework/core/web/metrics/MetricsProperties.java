package io.github.afgprojects.framework.core.web.metrics;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * 指标监控配置属性
 * <p>
 * 配置项前缀: afg.metrics
 * </p>
 *
 * <pre>
 * afg.metrics.enabled=true
 * afg.metrics.annotations.enabled=true
 * afg.metrics.tags.key1=value1
 * </pre>
 */
@Data
@ConfigurationProperties(prefix = "afg.metrics")
public class MetricsProperties {

    private boolean enabled = true;
    private final Annotations annotations = new Annotations();
    private final Map<String, String> tags = new HashMap<>();

    @Data
    public static class Annotations {
        private boolean enabled = true;
    }
}
