package io.github.afgprojects.framework.security.auth.login;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import io.github.afgprojects.framework.security.auth.login.config.TokenProperties;
import io.github.afgprojects.framework.security.core.login.TokenService;
import io.github.afgprojects.framework.security.core.storage.AfgRefreshTokenStorage;
import io.github.afgprojects.framework.security.core.storage.AfgTokenBlacklist;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtParserBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;

/**
 * 基于 JJWT 的 JWT Token 服务实现。
 *
 * <p>使用 jjwt-api 库实现 TokenService 接口，支持 HS256 算法签名。
 *
 * <h3>功能特性</h3>
 * <ul>
 *   <li>生成 Access Token（包含 userId, username, roles, permissions, tenantId）</li>
 *   <li>生成 Refresh Token（保存到 AfgRefreshTokenStorage）</li>
 *   <li>验证 Access Token（检查签名、过期时间、黑名单）</li>
 *   <li>验证 Refresh Token（检查签名、过期时间、存储中是否存在）</li>
 *   <li>从 Token 中提取用户信息</li>
 *   <li>将 Token 加入黑名单</li>
 *   <li>使用户所有 Token 失效</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * TokenProperties properties = new TokenProperties();
 * properties.setSigningKey("signing-key-at-least-256-bits-long");
 * properties.setIssuer("https://auth.example.com");
 *
 * JwtTokenService tokenService = new JwtTokenService(
 *     properties,
 *     refreshTokenStorage,
 *     tokenBlacklist
 * );
 *
 * // 生成 Access Token
 * String accessToken = tokenService.generateAccessToken(
 *     "user-123",
 *     "johndoe",
 *     Set.of("ADMIN", "USER"),
 *     Set.of("read:user", "write:user"),
 *     "tenant-001"
 * );
 *
 * // 验证 Token
 * boolean valid = tokenService.validateAccessToken(accessToken);
 *
 * // 提取用户信息
 * String userId = tokenService.extractUserId(accessToken);
 * }</pre>
 *
 * @since 1.0.0
 */
@Slf4j
public class JwtTokenService implements TokenService {

    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_PERMISSIONS = "permissions";
    private static final String CLAIM_TENANT_ID = "tenant_id";
    private static final String CLAIM_TOKEN_TYPE = "token_type";
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";
    private static final String REVOCATION_REASON = "revoked";

    private final SecretKey secretKey;
    private final String issuer;
    private final Duration accessTokenTtl;
    private final Duration refreshTokenTtl;
    private final boolean includeRoles;
    private final boolean includePermissions;
    private final AfgRefreshTokenStorage refreshTokenStorage;
    private final AfgTokenBlacklist tokenBlacklist;

    /**
     * 构造函数。
     *
     * @param properties           Token 配置属性
     * @param refreshTokenStorage  Refresh Token 存储
     * @param tokenBlacklist       Token 黑名单
     * @throws IllegalArgumentException 如果签名密钥无效
     */
    public JwtTokenService(
            @NonNull TokenProperties properties,
            @NonNull AfgRefreshTokenStorage refreshTokenStorage,
            @NonNull AfgTokenBlacklist tokenBlacklist) {

        String signingKey = properties.getSigningKey();
        if (signingKey == null || signingKey.length() < 32) {
            throw new IllegalArgumentException("Signing key must be at least 256 bits (32 characters)");
        }

        this.secretKey = Keys.hmacShaKeyFor(signingKey.getBytes(StandardCharsets.UTF_8));
        this.issuer = properties.getIssuer();
        this.accessTokenTtl = properties.getAccessTokenTtl();
        this.refreshTokenTtl = properties.getRefreshTokenTtl();
        this.includeRoles = properties.isIncludeRoles();
        this.includePermissions = properties.isIncludePermissions();
        this.refreshTokenStorage = refreshTokenStorage;
        this.tokenBlacklist = tokenBlacklist;
    }

    @Override
    @NonNull
    public String generateAccessToken(
            @NonNull String userId,
            @NonNull String username,
            @NonNull Set<String> roles,
            @NonNull Set<String> permissions,
            @Nullable String tenantId) {

        Instant now = Instant.now();
        Instant expiration = now.plus(accessTokenTtl);

        JwtBuilder builder = Jwts.builder()
                .subject(userId)
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .claim(CLAIM_USERNAME, username)
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS);

        if (includeRoles) {
            builder.claim(CLAIM_ROLES, List.copyOf(roles));
        }

        if (includePermissions) {
            builder.claim(CLAIM_PERMISSIONS, List.copyOf(permissions));
        }

        if (tenantId != null) {
            builder.claim(CLAIM_TENANT_ID, tenantId);
        }

        return builder.signWith(secretKey).compact();
    }

    @Override
    @NonNull
    public String generateRefreshToken(@NonNull String userId, @Nullable String tenantId) {
        Instant now = Instant.now();
        Instant expiration = now.plus(refreshTokenTtl);
        String tokenId = UUID.randomUUID().toString();

        JwtBuilder builder = Jwts.builder()
                .id(tokenId)
                .subject(userId)
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_REFRESH);

        if (tenantId != null) {
            builder.claim(CLAIM_TENANT_ID, tenantId);
        }

        String token = builder.signWith(secretKey).compact();

        // 保存 Refresh Token 到存储
        String tokenHash = sha256(token);
        LocalDateTime expiresAt = LocalDateTime.ofInstant(expiration, ZoneId.systemDefault());

        refreshTokenStorage.save(
                tokenId,
                tokenHash,
                userId,
                tenantId,
                null, // clientId
                null, // deviceId
                expiresAt
        );

        return token;
    }

    @Override
    public boolean validateAccessToken(@NonNull String token) {
        try {
            // 检查黑名单
            String tokenHash = sha256(token);
            if (tokenBlacklist.isBlacklisted(tokenHash)) {
                log.debug("Token is blacklisted");
                return false;
            }

            // 验证签名和过期时间
            parseToken(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("Access token has expired: {}", e.getMessage());
            return false;
        } catch (SignatureException e) {
            log.debug("Access token signature verification failed: {}", e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            log.debug("Access token is malformed: {}", e.getMessage());
            return false;
        } catch (UnsupportedJwtException e) {
            log.debug("Access token is unsupported: {}", e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            log.debug("Access token validation failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean validateRefreshToken(@NonNull String refreshToken) {
        try {
            // 验证签名和过期时间
            parseToken(refreshToken);

            // 检查存储中是否存在
            String tokenHash = sha256(refreshToken);
            return refreshTokenStorage.findByTokenHash(tokenHash).isPresent();
        } catch (ExpiredJwtException e) {
            log.debug("Refresh token has expired: {}", e.getMessage());
            return false;
        } catch (SignatureException e) {
            log.debug("Refresh token signature verification failed: {}", e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            log.debug("Refresh token is malformed: {}", e.getMessage());
            return false;
        } catch (UnsupportedJwtException e) {
            log.debug("Refresh token is unsupported: {}", e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            log.debug("Refresh token validation failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    @Nullable
    public String extractUserId(@NonNull String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getSubject();
        } catch (Exception e) {
            log.debug("Failed to extract user ID from token: {}", e.getMessage());
            return null;
        }
    }

    @Override
    @Nullable
    public String extractUsername(@NonNull String token) {
        try {
            Claims claims = parseToken(token);
            return claims.get(CLAIM_USERNAME, String.class);
        } catch (Exception e) {
            log.debug("Failed to extract username from token: {}", e.getMessage());
            return null;
        }
    }

    @Override
    @NonNull
    @SuppressWarnings("unchecked")
    public Set<String> extractRoles(@NonNull String token) {
        try {
            Claims claims = parseToken(token);
            List<String> roles = claims.get(CLAIM_ROLES, List.class);
            return roles != null ? Set.copyOf(roles) : Set.of();
        } catch (Exception e) {
            log.debug("Failed to extract roles from token: {}", e.getMessage());
            return Set.of();
        }
    }

    @Override
    @NonNull
    @SuppressWarnings("unchecked")
    public Set<String> extractPermissions(@NonNull String token) {
        try {
            Claims claims = parseToken(token);
            List<String> permissions = claims.get(CLAIM_PERMISSIONS, List.class);
            return permissions != null ? Set.copyOf(permissions) : Set.of();
        } catch (Exception e) {
            log.debug("Failed to extract permissions from token: {}", e.getMessage());
            return Set.of();
        }
    }

    @Override
    @Nullable
    public String extractTenantId(@NonNull String token) {
        try {
            Claims claims = parseToken(token);
            return claims.get(CLAIM_TENANT_ID, String.class);
        } catch (Exception e) {
            log.debug("Failed to extract tenant ID from token: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public void invalidateToken(@NonNull String token) {
        try {
            String tokenHash = sha256(token);
            String userId = extractUserId(token);

            if (userId != null) {
                // 计算剩余有效期作为 TTL
                Claims claims = parseToken(token);
                Instant expiration = claims.getExpiration().toInstant();
                Duration ttl = Duration.between(Instant.now(), expiration);

                // 如果已过期，使用最小 TTL
                if (ttl.isNegative() || ttl.isZero()) {
                    ttl = Duration.ofSeconds(1);
                }

                tokenBlacklist.addToBlacklist(tokenHash, userId, REVOCATION_REASON, ttl);
            }
        } catch (Exception e) {
            log.warn("Failed to invalidate token: {}", e.getMessage());
        }
    }

    @Override
    public void invalidateAllTokens(@NonNull String userId) {
        // 将用户所有 Token 加入黑名单
        tokenBlacklist.blacklistAllUserTokens(userId, refreshTokenTtl);

        // 删除用户所有 Refresh Token
        refreshTokenStorage.deleteByUserId(userId);
    }

    @Override
    public long getAccessTokenTtl() {
        return accessTokenTtl.getSeconds();
    }

    @Override
    public long getRefreshTokenTtl() {
        return refreshTokenTtl.getSeconds();
    }

    /**
     * 解析 Token 获取 Claims。
     *
     * @param token JWT Token
     * @return Claims
     * @throws ExpiredJwtException      如果 Token 已过期
     * @throws SignatureException       如果签名验证失败
     * @throws MalformedJwtException    如果 Token 格式错误
     * @throws UnsupportedJwtException  如果 Token 不支持
     * @throws IllegalArgumentException 如果 Token 为空
     */
    private Claims parseToken(String token) {
        JwtParserBuilder parser = Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(issuer);

        return parser.build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 计算字符串的 SHA-256 哈希值。
     *
     * @param input 输入字符串
     * @return SHA-256 哈希值（十六进制字符串）
     */
    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
