package io.github.afgprojects.framework.security.auth.audit.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.github.afgprojects.framework.security.core.audit.LoginLogService.LoginLogQuery;
import org.jspecify.annotations.Nullable;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginLogQueryParam implements LoginLogQuery {
    @Nullable private String userId;
    @Nullable private String username;
    @Nullable private String tenantId;
    @Nullable private String ip;
    @Nullable private String result;
    @Nullable private Instant startTime;
    @Nullable private Instant endTime;
}
