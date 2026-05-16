package io.github.afgprojects.framework.security.core.audit;

import org.jspecify.annotations.NonNull;
import java.util.List;

/**
 * 告警服务接口。
 *
 * @since 1.0.0
 */
public interface AlertService {

    void checkAndAlert(@NonNull SecurityEventService.SecurityEventInfo event);

    void sendAlert(@NonNull SecurityEventService.SecurityEventInfo event, @NonNull List<String> channels);
}
