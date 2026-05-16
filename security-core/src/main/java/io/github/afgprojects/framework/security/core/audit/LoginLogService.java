package io.github.afgprojects.framework.security.core.audit;

import io.github.afgprojects.framework.security.core.audit.model.LoginLog;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 登录日志服务接口。
 *
 * <p>提供登录日志的记录功能，用于审计和安全分析。
 *
 * <p>实现类可以选择不同的存储方式，如 JDBC、Redis、日志文件等。
 *
 * @author afg-projects
 * @since 1.0.0
 */
public interface LoginLogService {

    /**
     * 记录登录日志。
     *
     * <p>无论是登录成功还是失败，都应调用此方法记录日志。
     *
     * @param log 登录日志，永不为 null
     */
    void recordLogin(@NonNull LoginLog log);

    /**
     * 记录登出日志。
     *
     * <p>更新指定用户的登录记录，设置登出时间。
     *
     * @param userId 用户 ID，永不为 null
     * @param tenantId 租户 ID，单租户场景可为 null
     * @param ip 登出 IP 地址，永不为 null
     */
    void recordLogout(@NonNull String userId, @Nullable String tenantId, @NonNull String ip);
}