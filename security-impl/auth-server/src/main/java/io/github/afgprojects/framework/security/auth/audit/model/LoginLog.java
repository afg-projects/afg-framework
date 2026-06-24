package io.github.afgprojects.framework.security.auth.audit.model;

import io.github.afgprojects.framework.apt.entity.AfEntity;
import io.github.afgprojects.framework.data.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import io.github.afgprojects.framework.security.core.audit.LoginLogService.LoginLogInfo;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@AfEntity
@Table(name = "auth_login_log")
public class LoginLog extends BaseEntity implements LoginLogInfo {

    @Column(name = "user_id")
    private String userId;

    @Column(name = "username", length = 100)
    private String username;

    @Column(name = "tenant_id")
    private String tenantId;

    @Column(name = "ip", length = 50)
    private String ip;

    @Column(name = "device_id")
    private String deviceId;

    @Column(name = "device_name")
    private String deviceName;

    @Column(name = "browser")
    private String browser;

    @Column(name = "os")
    private String os;

    @Column(name = "location")
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", length = 20)
    @Getter(AccessLevel.NONE)
    private LoginResult result;

    @Column(name = "fail_reason")
    private String failReason;

    @Column(name = "login_time")
    private Instant loginTime;

    @Column(name = "logout_time")
    private Instant logoutTime;

    @Override
    public String getResult() {
        return result != null ? result.name() : null;
    }
}
