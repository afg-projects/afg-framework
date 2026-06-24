package io.github.afgprojects.framework.security.auth.audit.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.github.afgprojects.framework.security.core.audit.SecurityEventService.SecurityEventInfo;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityEvent implements SecurityEventInfo {
    private String id;
    private SecurityEventType eventType;
    private String userId;
    private String tenantId;
    private String ip;
    private String details;
    private Instant createdAt;

    @Override
    public String getEventType() {
        return eventType != null ? eventType.name() : null;
    }
}
