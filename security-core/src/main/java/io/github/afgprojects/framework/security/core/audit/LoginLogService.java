package io.github.afgprojects.framework.security.core.audit;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;

/**
 * 登录日志服务接口。
 *
 * @since 1.0.0
 */
public interface LoginLogService {

    void recordLogin(@NonNull LoginLogInfo log);

    void recordLogout(@NonNull String userId, @Nullable String tenantId, @NonNull String ip);

    Page<LoginLogInfo> queryLogs(@NonNull LoginLogQuery query, @NonNull Pageable pageable);

    interface LoginLogInfo {
        String getUserId();
        String getUsername();
        String getTenantId();
        String getIp();
        String getDeviceId();
        String getDeviceName();
        String getBrowser();
        String getOs();
        String getLocation();
        String getResult();
        String getFailReason();
        Instant getLoginTime();
        Instant getLogoutTime();
    }

    interface LoginLogQuery {
        @Nullable String getUserId();
        @Nullable String getUsername();
        @Nullable String getTenantId();
        @Nullable String getIp();
        @Nullable String getResult();
        @Nullable Instant getStartTime();
        @Nullable Instant getEndTime();
    }
}
