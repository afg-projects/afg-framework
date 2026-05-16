package io.github.afgprojects.framework.security.auth.storage;

import lombok.extern.slf4j.Slf4j;
import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.security.auth.entity.AuthRefreshToken;
import io.github.afgprojects.framework.security.core.storage.AfgRefreshTokenStorage;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.Optional;

import static io.github.afgprojects.framework.data.core.condition.Conditions.*;

/**
 * 基于 DataManager 的 Refresh Token 存储。
 *
 * <p>将 Refresh Token 持久化到关系型数据库，支持：
 * <ul>
 *   <li>Token 保存与查询</li>
 *   <li>Token 撤销（删除）</li>
 *   <li>按用户删除所有 Token</li>
 *   <li>清理过期 Token</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Slf4j
public class JdbcRefreshTokenStorage implements AfgRefreshTokenStorage {

    private final DataManager dataManager;

    /**
     * 构造函数。
     *
     * @param dataManager 数据管理器
     */
    public JdbcRefreshTokenStorage(@NonNull DataManager dataManager) {
        this.dataManager = dataManager;
    }

    @Override
    public void save(
            @NonNull String tokenId,
            @NonNull String tokenHash,
            @NonNull String userId,
            @Nullable String tenantId,
            @Nullable String clientId,
            @Nullable String deviceId,
            @NonNull LocalDateTime expiresAt
    ) {
        LocalDateTime createdAt = LocalDateTime.now();

        AuthRefreshToken entity = new AuthRefreshToken();
        entity.setTokenId(tokenId);
        entity.setTokenHash(tokenHash);
        entity.setUserId(userId);
        entity.setTenantId(tenantId);
        entity.setClientId(clientId);
        entity.setDeviceId(deviceId);
        entity.setExpiresAt(expiresAt);
        entity.setCreatedAt(createdAt);

        dataManager.save(AuthRefreshToken.class, entity);
        log.debug("Saved refresh token: tokenId={}, userId={}", tokenId, userId);
    }

    @Override
    @NonNull
    public Optional<RefreshTokenInfo> findByTokenHash(@NonNull String tokenHash) {
        return dataManager.findOneByField(AuthRefreshToken.class,
                AuthRefreshToken::getTokenHash, tokenHash)
                .map(this::toRefreshTokenInfo);
    }

    @Override
    @NonNull
    public Optional<RefreshTokenInfo> findByTokenId(@NonNull String tokenId) {
        return dataManager.findOneByField(AuthRefreshToken.class,
                AuthRefreshToken::getTokenId, tokenId)
                .map(this::toRefreshTokenInfo);
    }

    @Override
    public void delete(@NonNull String tokenId) {
        dataManager.findOneByField(AuthRefreshToken.class,
                AuthRefreshToken::getTokenId, tokenId)
                .ifPresent(entity -> {
                    dataManager.deleteById(AuthRefreshToken.class, entity.getId());
                    log.debug("Deleted refresh token: tokenId={}", tokenId);
                });
    }

    @Override
    public void deleteByUserId(@NonNull String userId) {
        var entities = dataManager.findList(AuthRefreshToken.class,
                builder(AuthRefreshToken.class)
                        .eq(AuthRefreshToken::getUserId, userId)
                        .build());

        for (var entity : entities) {
            dataManager.deleteById(AuthRefreshToken.class, entity.getId());
        }
        log.debug("Deleted all refresh tokens for user: userId={}, count={}", userId, entities.size());
    }

    @Override
    public int deleteExpired() {
        LocalDateTime now = LocalDateTime.now();
        var entities = dataManager.findList(AuthRefreshToken.class,
                builder(AuthRefreshToken.class)
                        .lt(AuthRefreshToken::getExpiresAt, now)
                        .build());

        for (var entity : entities) {
            dataManager.deleteById(AuthRefreshToken.class, entity.getId());
        }

        int count = entities.size();
        if (count > 0) {
            log.info("Deleted expired refresh tokens: count={}", count);
        }
        return count;
    }

    /**
     * 将 AuthRefreshToken 转换为 RefreshTokenInfo。
     */
    private RefreshTokenInfo toRefreshTokenInfo(AuthRefreshToken entity) {
        return new RefreshTokenInfo(
                entity.getTokenId(),
                entity.getTokenHash(),
                entity.getUserId(),
                entity.getTenantId(),
                entity.getClientId(),
                entity.getDeviceId(),
                entity.getExpiresAt(),
                entity.getCreatedAt()
        );
    }
}