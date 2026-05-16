package io.github.afgprojects.framework.security.auth.audit.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.github.afgprojects.framework.security.core.audit.LoginLogService.LoginLogInfo;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginLog implements LoginLogInfo {
    private Long id;
    private String userId;
    private String username;
    private String tenantId;
    private String ip;
    private String deviceId;
    private String deviceName;
    private String browser;
    private String os;
    private String location;
    private LoginResult result;
    private String failReason;
    private Instant loginTime;
    private Instant logoutTime;

    @Override
    public String getResult() {
        return result != null ? result.name() : null;
    }
}
