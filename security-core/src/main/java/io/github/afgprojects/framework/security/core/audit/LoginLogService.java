package io.github.afgprojects.framework.security.core.audit;

import io.github.afgprojects.framework.commons.model.PageData;
import io.github.afgprojects.framework.data.core.page.PageRequest;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * 登录日志服务接口。
 *
 * @since 1.0.0
 */
public interface LoginLogService {

    void recordLogin(@NonNull LoginLogInfo log);

    void recordLogout(@NonNull String userId, @Nullable String tenantId, @NonNull String ip);

    PageData<LoginLogInfo> queryLogs(@NonNull LoginLogQuery query, @NonNull PageRequest pageRequest);

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
