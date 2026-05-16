package io.github.afgprojects.framework.security.core.audit;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * 安全事件服务接口。
 *
 * @since 1.0.0
 */
public interface SecurityEventService {

    void recordEvent(@NonNull SecurityEventInfo event);

    List<SecurityEventInfo> getRecentEvents(@NonNull Duration duration);

    List<SecurityEventInfo> getEventsByType(@NonNull String eventType, @NonNull Duration duration);

    interface SecurityEventInfo {
        Long getId();
        String getEventType();
        String getUserId();
        String getTenantId();
        String getIp();
        String getDetails();
        Instant getCreatedAt();
    }
}
