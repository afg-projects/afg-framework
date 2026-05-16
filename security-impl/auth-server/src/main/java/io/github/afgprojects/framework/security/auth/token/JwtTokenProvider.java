package io.github.afgprojects.framework.security.auth.token;

import lombok.extern.slf4j.Slf4j;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

/**
 * JWT Token 提供者
 *
 * <p>提供 JWT Token 的生成、验证和解析功能
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * JwtTokenProvider provider = new JwtTokenProvider(
 *     "signing-key",
 *     "https://auth.example.com",
 *     Duration.ofHours(2)
 * );
 *
 * // 生成 Access Token
 * String token = provider.generateAccessToken(
 *     "user-123",
 *     List.of("ADMIN"),
 *     List.of("read:user"),
 *     Map.of("client_id", "my-client")
 * );
 *
 * // 验证 Token
 * boolean valid = provider.validateToken(token);
 *
 * // 解析 Token
 * JWTClaimsSet claims = provider.parseToken(token);
 * }</pre>
 *
 * @since 1.0.0
 */
@Slf4j
public class JwtTokenProvider {
    private final JWSSigner signer;
    private final JWSVerifier verifier;
    private final String issuer;
    private final Duration accessTokenTtl;

    /**
     * 构造函数
     *
     * @param signingKey     签名密钥（至少 256 位）
     * @param issuer         Token issuer
     * @param accessTokenTtl Access Token 有效期
     */
    public JwtTokenProvider(String signingKey, String issuer, Duration accessTokenTtl) {
        try {
            this.signer = new MACSigner(signingKey);
            this.verifier = new MACVerifier(signingKey);
        } catch (JOSEException e) {
            throw new IllegalArgumentException("Failed to initialize JWT signer", e);
        }
        this.issuer = issuer;
        this.accessTokenTtl = accessTokenTtl;
    }

    /**
     * 生成 Access Token
     *
     * @param subject           用户标识
     * @param roles             用户角色列表
     * @param permissions       用户权限列表
     * @param additionalClaims 额外的 claims
     * @return JWT Access Token
     */
    public String generateAccessToken(
            String subject,
            List<String> roles,
            List<String> permissions,
            Map<String, Object> additionalClaims) {

        Instant now = Instant.now();
        Instant expiration = now.plus(accessTokenTtl);

        JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                .subject(subject)
                .issuer(issuer)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(expiration))
                .claim("roles", roles)
                .claim("permissions", permissions);

        additionalClaims.forEach(claimsBuilder::claim);

        JWTClaimsSet claims = claimsBuilder.build();
        return signToken(claims);
    }

    /**
     * 生成 Refresh Token
     *
     * @param subject 用户标识
     * @return JWT Refresh Token
     */
    public String generateRefreshToken(String subject) {
        Instant now = Instant.now();
        // Refresh Token 有效期通常是 Access Token 的 2-3 倍，这里默认 7 天
        Instant expiration = now.plus(Duration.ofDays(7));

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(subject)
                .issuer(issuer)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(expiration))
                .claim("token_type", "refresh")
                .build();

        return signToken(claims);
    }

    /**
     * 验证 Token 是否有效
     *
     * @param token JWT Token
     * @return 如果 Token 有效返回 true
     */
    public boolean validateToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);

            if (!signedJWT.verify(verifier)) {
                log.debug("Token signature verification failed");
                return false;
            }

            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            Instant expiration = claims.getExpirationTime() != null
                    ? claims.getExpirationTime().toInstant()
                    : null;

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
     * 解析 Token 获取 Claims
     *
     * @param token JWT Token
     * @return JWT Claims
     * @throws Exception 如果 Token 解析失败
     */
    public JWTClaimsSet parseToken(String token) throws Exception {
        SignedJWT signedJWT = SignedJWT.parse(token);

        if (!signedJWT.verify(verifier)) {
            throw new JOSEException("Token signature verification failed");
        }

        return signedJWT.getJWTClaimsSet();
    }

    /**
     * 从 Token 中提取 subject
     *
     * @param token JWT Token
     * @return subject，如果提取失败返回 null
     */
    @Nullable
    public String extractSubject(String token) {
        try {
            JWTClaimsSet claims = parseToken(token);
            return claims.getSubject();
        } catch (Exception e) {
            log.debug("Failed to extract subject from token: {}", e.getMessage());
            return null;
        }
    }

    private String signToken(JWTClaimsSet claims) {
        try {
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.HS256)
                    .build();

            SignedJWT signedJWT = new SignedJWT(header, claims);
            signedJWT.sign(signer);

            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException("Failed to sign JWT token", e);
        }
    }
}