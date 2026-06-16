package io.github.afgprojects.framework.security.auth.token;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.github.afgprojects.framework.security.auth.key.RsaKeyPairManager;
import io.github.afgprojects.framework.security.core.login.TokenService;
import io.github.afgprojects.framework.security.core.storage.AfgRefreshTokenStorage;
import io.github.afgprojects.framework.security.core.storage.AfgTokenBlacklist;
import io.github.afgprojects.framework.security.core.token.JwtClaimsConfig;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 默认令牌服务实现。
 *
 * <p>基于 Nimbus JOSE JWT 库实现 TokenService 接口，使用 RS256 算法签名。
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
 * <h3>安全特性</h3>
 * <ul>
 *   <li>使用 RS256 非对称加密签名</li>
 *   <li>私钥仅存储在认证服务器</li>
 *   <li>公钥通过 /.well-known/jwks.json 端点公开</li>
 *   <li>自动生成和管理 RSA 密钥对</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Slf4j
public class DefaultTokenService implements TokenService {
    private static final String CLAIM_USERNAME = JwtClaimsConfig.DEFAULT_USERNAME_CLAIM;
    private static final String CLAIM_ROLES = JwtClaimsConfig.DEFAULT_ROLES_CLAIM;
    private static final String CLAIM_PERMISSIONS = JwtClaimsConfig.DEFAULT_PERMISSIONS_CLAIM;
    private static final String CLAIM_TENANT_ID = JwtClaimsConfig.DEFAULT_TENANT_ID_CLAIM;
    private static final String CLAIM_TOKEN_TYPE = JwtClaimsConfig.DEFAULT_TOKEN_TYPE_CLAIM;
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";
    private static final String REVOCATION_REASON = "revoked";

    private final JWSSigner signer;
    private final JWSVerifier verifier;
    private final String keyId;
    private final String issuer;
    private final Duration accessTokenTtl;
    private final Duration refreshTokenTtl;
    private final AfgRefreshTokenStorage refreshTokenStorage;
    private final AfgTokenBlacklist tokenBlacklist;

    /**
     * 构造函数。
     *
     * @param keyPairManager      RSA 密钥对管理器
     * @param issuer              Token issuer
     * @param accessTokenTtl      Access Token 有效期
     * @param refreshTokenTtl     Refresh Token 有效期
     * @param refreshTokenStorage Refresh Token 存储
     * @param tokenBlacklist      Token 黑名单
     */
    public DefaultTokenService(
            @NonNull RsaKeyPairManager keyPairManager,
            @NonNull String issuer,
            @NonNull Duration accessTokenTtl,
            @NonNull Duration refreshTokenTtl,
            @NonNull AfgRefreshTokenStorage refreshTokenStorage,
            @NonNull AfgTokenBlacklist tokenBlacklist) {

        RSAPrivateKey privateKey = keyPairManager.getPrivateKey();
        RSAPublicKey publicKey = keyPairManager.getPublicKey();

        this.signer = new RSASSASigner(privateKey);
        this.verifier = new RSASSAVerifier(publicKey);

        this.keyId = keyPairManager.getKeyId();
        this.issuer = issuer;
        this.accessTokenTtl = accessTokenTtl;
        this.refreshTokenTtl = refreshTokenTtl;
        this.refreshTokenStorage = refreshTokenStorage;
        this.tokenBlacklist = tokenBlacklist;

        log.info("Initialized DefaultTokenService with RS256 signing, keyId: {}", keyId);
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

        JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                .subject(userId)
                .issuer(issuer)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(expiration))
                .claim(CLAIM_USERNAME, username)
                .claim(CLAIM_ROLES, List.copyOf(roles))
                .claim(CLAIM_PERMISSIONS, List.copyOf(permissions))
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS);

        if (tenantId != null) {
            claimsBuilder.claim(CLAIM_TENANT_ID, tenantId);
        }

        JWTClaimsSet claims = claimsBuilder.build();
        return signToken(claims);
    }

    @Override
    @NonNull
    public String generateRefreshToken(@NonNull String userId, @Nullable String tenantId) {
        Instant now = Instant.now();
        Instant expiration = now.plus(refreshTokenTtl);
        String tokenId = UUID.randomUUID().toString();

        JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                .jwtID(tokenId)
                .subject(userId)
                .issuer(issuer)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(expiration))
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_REFRESH);

        if (tenantId != null) {
            claimsBuilder.claim(CLAIM_TENANT_ID, tenantId);
        }

        JWTClaimsSet claims = claimsBuilder.build();
        String token = signToken(claims);

        // 保存 Refresh Token 到存储
        String tokenHash = sha256(token);

        refreshTokenStorage.save(
                tokenId,
                tokenHash,
                userId,
                tenantId,
                null, // clientId
                null, // deviceId
                expiration
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
            return validateTokenSignatureAndExpiration(token);
        } catch (Exception e) {
            log.debug("Access token validation failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean validateRefreshToken(@NonNull String refreshToken) {
        try {
            // 验证签名和过期时间
            if (!validateTokenSignatureAndExpiration(refreshToken)) {
                return false;
            }

            // 检查存储中是否存在
            String tokenHash = sha256(refreshToken);
            return refreshTokenStorage.findByTokenHash(tokenHash).isPresent();
        } catch (Exception e) {
            log.debug("Refresh token validation failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    @Nullable
    public String extractUserId(@NonNull String token) {
        try {
            JWTClaimsSet claims = parseToken(token);
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
            JWTClaimsSet claims = parseToken(token);
            return claims.getStringClaim(CLAIM_USERNAME);
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
            JWTClaimsSet claims = parseToken(token);
            List<String> roles = (List<String>) claims.getClaim(CLAIM_ROLES);
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
            JWTClaimsSet claims = parseToken(token);
            List<String> permissions = (List<String>) claims.getClaim(CLAIM_PERMISSIONS);
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
            JWTClaimsSet claims = parseToken(token);
            return claims.getStringClaim(CLAIM_TENANT_ID);
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
                JWTClaimsSet claims = parseToken(token);
                Instant expiration = claims.getExpirationTime().toInstant();
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
    public void invalidateRefreshToken(@NonNull String refreshToken) {
        try {
            String tokenHash = sha256(refreshToken);
            refreshTokenStorage.deleteByTokenHash(tokenHash);
            log.debug("Invalidated refresh token");
        } catch (Exception e) {
            log.warn("Failed to invalidate refresh token: {}", e.getMessage());
        }
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
     * 验证 Token 签名和过期时间。
     */
    @SuppressWarnings("PMD.ReplaceJavaUtilDate")
    private boolean validateTokenSignatureAndExpiration(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);

            // 验证签名
            if (!signedJWT.verify(verifier)) {
                log.debug("Token signature verification failed");
                return false;
            }

            // 验证过期时间
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            Instant expiration = claims.getExpirationTime().toInstant();

            if (expiration != null && expiration.isBefore(Instant.now())) {
                log.debug("Token has expired");
                return false;
            }

            return true;
        } catch (Exception e) {
            log.debug("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 解析 Token 获取 Claims。
     */
    private JWTClaimsSet parseToken(String token) throws Exception {
        SignedJWT signedJWT = SignedJWT.parse(token);

        if (!signedJWT.verify(verifier)) {
            throw new JOSEException("Token signature verification failed");
        }

        return signedJWT.getJWTClaimsSet();
    }

    /**
     * 签名 Token（使用 RS256）。
     */
    private String signToken(JWTClaimsSet claims) {
        try {
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .keyID(keyId)
                    .build();

            SignedJWT signedJWT = new SignedJWT(header, claims);
            signedJWT.sign(signer);
            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException("Failed to sign JWT token", e);
        }
    }

    /**
     * 计算字符串的 SHA-256 哈希值。
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
