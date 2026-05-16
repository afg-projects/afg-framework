package io.github.afgprojects.framework.security.auth.oauth2;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import io.github.afgprojects.framework.security.core.oauth2.AuthorizationCodeStorage;
import io.github.afgprojects.framework.security.core.oauth2.model.AuthorizationCode;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存授权码存储实现。
 *
 * <p>适用于单机部署，使用 ConcurrentHashMap 存储授权码。
 * 分布式环境应使用 Redis 或 JDBC 实现。
 *
 * @since 1.0.0
 */
@Slf4j
public class InMemoryAuthorizationCodeStorage implements AuthorizationCodeStorage {

    private final ConcurrentHashMap<String, AuthorizationCode> storage = new ConcurrentHashMap<>();

    @Override
    public void save(@NonNull AuthorizationCode authorizationCode) {
        storage.put(authorizationCode.code(), authorizationCode);
        log.debug("Saved authorization code: code={}, clientId={}, userId={}",
                authorizationCode.code(), authorizationCode.clientId(), authorizationCode.userId());
    }

    @Override
    @Nullable
    public AuthorizationCode findByCode(@NonNull String code) {
        AuthorizationCode authCode = storage.get(code);
        if (authCode != null && authCode.isExpired()) {
            log.debug("Authorization code expired: code={}", code);
            storage.remove(code);
            return null;
        }
        return authCode;
    }

    @Override
    public void delete(@NonNull String code) {
        AuthorizationCode removed = storage.remove(code);
        if (removed != null) {
            log.debug("Deleted authorization code: code={}", code);
        }
    }

    @Override
    public void deleteByUserId(@NonNull String userId) {
        storage.entrySet().removeIf(entry -> {
            if (entry.getValue().userId().equals(userId)) {
                log.debug("Deleted authorization code for user: code={}, userId={}",
                        entry.getKey(), userId);
                return true;
            }
            return false;
        });
    }

    @Override
    public int deleteExpired() {
        Instant now = Instant.now();
        int count = 0;
        for (var entry : storage.entrySet()) {
            if (entry.getValue().expiresAt().isBefore(now)) {
                storage.remove(entry.getKey());
                count++;
            }
        }
        if (count > 0) {
            log.debug("Deleted expired authorization codes: count={}", count);
        }
        return count;
    }

    /**
     * 获取存储的授权码数量（用于测试）。
     *
     * @return 授权码数量
     */
    public int size() {
        return storage.size();
    }

    /**
     * 清空所有授权码（用于测试）。
     */
    public void clear() {
        storage.clear();
    }
}