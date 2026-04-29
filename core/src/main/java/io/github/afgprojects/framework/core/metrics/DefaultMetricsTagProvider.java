package io.github.afgprojects.framework.core.metrics;

import java.util.Collections;

import org.jspecify.annotations.NonNull;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;

/**
 * 默认指标标签提供者
 *
 * <p>提供基础的通用标签
 *
 * @since 1.0.0
 */
@SuppressWarnings("PMD.AvoidCatchingGenericException")
public class DefaultMetricsTagProvider implements MetricsTagProvider {

    @Override
    @NonNull
    public Iterable<Tag> getTags() {
        return Tags.of(
                Tag.of("application", getApplicationName()),
                Tag.of("host", getHostName()),
                Tag.of("version", getVersion()));
    }

    @NonNull
    private String getApplicationName() {
        String name = System.getProperty("spring.application.name");
        if (name != null) {
            return name;
        }
        name = System.getenv("SPRING_APPLICATION_NAME");
        if (name != null) {
            return name;
        }
        return "unknown";
    }

    @NonNull
    private String getHostName() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }

    @NonNull
    private String getVersion() {
        String version = getClass().getPackage().getImplementationVersion();
        return version != null ? version : "unknown";
    }
}