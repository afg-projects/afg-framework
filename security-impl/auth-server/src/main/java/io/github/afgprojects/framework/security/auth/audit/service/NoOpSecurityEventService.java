package io.github.afgprojects.framework.security.auth.audit.service;

import io.github.afgprojects.framework.security.core.audit.SecurityEventService;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * 无操作的安全事件服务实现。
 *
 * <p>当 DataManager 不是 JdbcDataManager 时使用此实现。
 *
 * @since 1.0.0
 */
@Slf4j
public class NoOpSecurityEventService implements SecurityEventService {

    @Override
    public void recordEvent(@NonNull SecurityEventInfo event) {
        log.debug("NoOpSecurityEventService: event not recorded - {}", event.getEventType());
    }

    @Override
    public List<SecurityEventInfo> getRecentEvents(@NonNull Duration duration) {
        return Collections.emptyList();
    }

    @Override
    public List<SecurityEventInfo> getEventsByType(@NonNull String eventType, @NonNull Duration duration) {
        return Collections.emptyList();
    }
}
