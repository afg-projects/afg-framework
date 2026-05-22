package io.github.afgprojects.framework.security.auth.audit;

import io.github.afgprojects.framework.core.model.result.PageData;
import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.data.core.condition.TypedConditionBuilder;
import io.github.afgprojects.framework.data.core.page.PageRequest;
import io.github.afgprojects.framework.data.core.query.Condition;
import io.github.afgprojects.framework.data.core.query.Page;
import io.github.afgprojects.framework.data.core.query.Sort;
import io.github.afgprojects.framework.security.auth.audit.model.LoginLog;
import io.github.afgprojects.framework.security.auth.audit.model.LoginResult;
import io.github.afgprojects.framework.security.core.audit.LoginLogService;

import lombok.extern.slf4j.Slf4j;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.github.afgprojects.framework.data.core.condition.Conditions.builder;

/**
 * 基于 DataManager 的登录日志服务实现。
 *
 * <p>使用 {@link DataManager} 将登录日志持久化到数据库。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
public class JdbcLoginLogService implements LoginLogService {

    private final DataManager dataManager;

    /**
     * 创建 JdbcLoginLogService 实例。
     *
     * @param dataManager 数据管理器，永不为 null
     */
    public JdbcLoginLogService(@NonNull DataManager dataManager) {
        this.dataManager = dataManager;
    }

    @Override
    public void recordLogin(@NonNull LoginLogInfo loginLog) {
        LoginLog entity = LoginLog.builder()
                .userId(loginLog.getUserId())
                .username(loginLog.getUsername())
                .tenantId(loginLog.getTenantId())
                .ip(loginLog.getIp())
                .deviceId(loginLog.getDeviceId())
                .deviceName(loginLog.getDeviceName())
                .browser(loginLog.getBrowser())
                .os(loginLog.getOs())
                .location(loginLog.getLocation())
                .result(loginLog.getResult() != null ? LoginResult.valueOf(loginLog.getResult()) : null)
                .failReason(loginLog.getFailReason())
                .loginTime(loginLog.getLoginTime() != null ? loginLog.getLoginTime() : Instant.now())
                .logoutTime(loginLog.getLogoutTime())
                .build();

        dataManager.save(LoginLog.class, entity);

        log.debug("Recorded login log: username={}, result={}", loginLog.getUsername(), loginLog.getResult());
    }

    @Override
    public void recordLogout(@NonNull String userId, @Nullable String tenantId, @NonNull String ip) {
        // 构建查询条件：查找用户最近的未登出记录
        TypedConditionBuilder<LoginLog> builder = builder(LoginLog.class)
                .eq(LoginLog::getUserId, userId)
                .isNull(LoginLog::getLogoutTime);

        // 处理 tenantId 为 null 的情况
        if (tenantId == null) {
            builder.isNull(LoginLog::getTenantId);
        } else {
            builder.eq(LoginLog::getTenantId, tenantId);
        }

        Condition condition = builder.build();

        // 查找最近一条登录记录
        List<LoginLog> logs = dataManager.entity(LoginLog.class)
                .query()
                .where(condition)
                .orderBy(Sort.desc(LoginLog::getLoginTime))
                .limit(1)
                .list();

        if (!logs.isEmpty()) {
            LoginLog loginLog = logs.getFirst();
            loginLog.setLogoutTime(Instant.now());
            dataManager.save(LoginLog.class, loginLog);
            log.debug("Recorded logout for user: userId={}, tenantId={}, ip={}", userId, tenantId, ip);
        } else {
            log.debug("No active login session found for user: userId={}, tenantId={}, ip={}", userId, tenantId, ip);
        }
    }

    @Override
    public PageData<LoginLogInfo> queryLogs(@NonNull LoginLogQuery query, @NonNull PageRequest pageRequest) {
        // 构建查询条件
        TypedConditionBuilder<LoginLog> builder = builder(LoginLog.class);

        if (query.getUserId() != null) {
            builder.eq(LoginLog::getUserId, query.getUserId());
        }
        if (query.getUsername() != null) {
            builder.like(LoginLog::getUsername, query.getUsername());
        }
        if (query.getTenantId() != null) {
            builder.eq(LoginLog::getTenantId, query.getTenantId());
        }
        if (query.getIp() != null) {
            builder.eq(LoginLog::getIp, query.getIp());
        }
        if (query.getResult() != null) {
            builder.eq(LoginLog::getResult, LoginResult.valueOf(query.getResult()));
        }
        if (query.getStartTime() != null) {
            builder.ge(LoginLog::getLoginTime, query.getStartTime());
        }
        if (query.getEndTime() != null) {
            builder.le(LoginLog::getLoginTime, query.getEndTime());
        }

        Condition condition = builder.build();

        // 分页查询
        Page<LoginLog> page;
        if (pageRequest.hasSort()) {
            page = dataManager.entity(LoginLog.class)
                    .query()
                    .where(condition)
                    .orderBy(pageRequest.sort())
                    .page(pageRequest);
        } else {
            // 默认按登录时间降序
            page = dataManager.entity(LoginLog.class)
                    .query()
                    .where(condition)
                    .orderBy(Sort.desc(LoginLog::getLoginTime))
                    .page(pageRequest);
        }

        // 转换结果
        List<LoginLogInfo> logs = page.getContent().stream()
                .map(log -> (LoginLogInfo) log)
                .toList();

        return PageData.of(logs, page.getTotal(), page.getPage(), page.getSize());
    }
}