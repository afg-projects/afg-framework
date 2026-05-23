package io.github.afgprojects.framework.security.auth.key;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.WritableResource;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * RSA 密钥对管理服务。
 *
 * <p>负责 RSA 密钥对的生成、持久化和加载。用于 JWT RS256 签名。
 *
 * <h3>密钥存储</h3>
 * <p>密钥对存储在配置目录下：
 * <ul>
 *   <li>私钥：{keyStorePath}/private_key.pem</li>
 *   <li>公钥：{keyStorePath}/public_key.pem</li>
 * </ul>
 *
 * <h3>密钥格式</h3>
 * <p>使用 PEM 格式存储：
 * <ul>
 *   <li>私钥：PKCS#8 格式</li>
 *   <li>公钥：X.509 格式</li>
 * </ul>
 *
 * <h3>自动生成</h3>
 * <p>如果密钥文件不存在，启动时自动生成 2048 位 RSA 密钥对。
 *
 * @since 1.1.0
 */
@Slf4j
public class RsaKeyPairManager {

    private static final String PRIVATE_KEY_FILE = "private_key.pem";
    private static final String PUBLIC_KEY_FILE = "public_key.pem";
    private static final int KEY_SIZE = 2048;

    private final KeyPair keyPair;
    private final String keyId;
    private final ResourceLoader resourceLoader;

    /**
     * 构造函数。
     *
     * @param keyStorePath 密钥存储路径（如 file:/var/keys 或 classpath:keys）
     */
    public RsaKeyPairManager(@NonNull String keyStorePath) {
        this.keyId = UUID.randomUUID().toString();
        this.resourceLoader = new DefaultResourceLoader();

        // 尝试加载现有密钥
        KeyPair loaded = loadKeyPair(keyStorePath);

        if (loaded != null) {
            this.keyPair = loaded;
            log.info("Loaded existing RSA key pair from: {}", keyStorePath);
        } else {
            // 生成新密钥对
            this.keyPair = generateKeyPair();
            saveKeyPair(keyStorePath);
            log.info("Generated and saved new RSA key pair to: {}", keyStorePath);
        }
    }

    /**
     * 获取 RSA 密钥对。
     *
     * @return RSA 密钥对
     */
    @NonNull
    public KeyPair getKeyPair() {
        return keyPair;
    }

    /**
     * 获取 RSA 私钥。
     */
    @NonNull
    public RSAPrivateKey getPrivateKey() {
        return (RSAPrivateKey) keyPair.getPrivate();
    }

    /**
     * 获取 RSA 公钥。
     */
    @NonNull
    public RSAPublicKey getPublicKey() {
        return (RSAPublicKey) keyPair.getPublic();
    }

    /**
     * 获取密钥 ID。
     */
    @NonNull
    public String getKeyId() {
        return keyId;
    }

    /**
     * 获取 JWK Set（用于 /.well-known/jwks.json 端点）。
     *
     * @return JWK Set JSON 字符串
     */
    @NonNull
    public String getJwkSetJson() {
        RSAKey rsaKey = new RSAKey.Builder(getPublicKey())
                .keyID(keyId)
                .algorithm(JWSAlgorithm.RS256)
                .keyUse(KeyUse.SIGNATURE)
                .issueTime(Date.from(Instant.now()))
                .build();

        JWKSet jwkSet = new JWKSet(List.of(rsaKey));
        return jwkSet.toString();
    }

    /**
     * 从文件加载密钥对。
     */
    private KeyPair loadKeyPair(String keyStorePath) {
        try {
            Resource privateKeyResource = resourceLoader.getResource(keyStorePath + "/" + PRIVATE_KEY_FILE);
            Resource publicKeyResource = resourceLoader.getResource(keyStorePath + "/" + PUBLIC_KEY_FILE);

            if (!privateKeyResource.exists() || !publicKeyResource.exists()) {
                log.debug("Key files not found in: {}", keyStorePath);
                return null;
            }

            // 读取并解析私钥
            String privateKeyPem = new String(privateKeyResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            byte[] privateKeyBytes = parsePem(privateKeyPem);
            PKCS8EncodedKeySpec privateSpec = new PKCS8EncodedKeySpec(privateKeyBytes);

            // 读取并解析公钥
            String publicKeyPem = new String(publicKeyResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            byte[] publicKeyBytes = parsePem(publicKeyPem);
            X509EncodedKeySpec publicSpec = new X509EncodedKeySpec(publicKeyBytes);

            // 生成密钥对
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return new KeyPair(
                    keyFactory.generatePublic(publicSpec),
                    keyFactory.generatePrivate(privateSpec)
            );
        } catch (Exception e) {
            log.warn("Failed to load key pair from {}: {}", keyStorePath, e.getMessage());
            return null;
        }
    }

    /**
     * 生成 RSA 密钥对。
     */
    private KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(KEY_SIZE);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("RSA algorithm not available", e);
        }
    }

    /**
     * 保存密钥对到文件。
     */
    private void saveKeyPair(String keyStorePath) {
        try {
            // 处理 file: 协议
            if (keyStorePath.startsWith("file:")) {
                Path basePath = Path.of(keyStorePath.substring(5));

                // 确保目录存在
                if (!Files.exists(basePath)) {
                    Files.createDirectories(basePath);
                }

                // 保存私钥
                Path privateKeyPath = basePath.resolve(PRIVATE_KEY_FILE);
                String privateKeyPem = toPem(keyPair.getPrivate().getEncoded(), "PRIVATE KEY");
                Files.writeString(privateKeyPath, privateKeyPem);

                // 保存公钥
                Path publicKeyPath = basePath.resolve(PUBLIC_KEY_FILE);
                String publicKeyPem = toPem(keyPair.getPublic().getEncoded(), "PUBLIC KEY");
                Files.writeString(publicKeyPath, publicKeyPem);

                log.info("Saved RSA key pair to: {}", basePath);
            } else {
                log.warn("Unsupported key store path protocol: {}. Only file: protocol is supported for writing.", keyStorePath);
            }
        } catch (IOException e) {
            log.error("Failed to save key pair to {}: {}", keyStorePath, e.getMessage());
            throw new RuntimeException("Failed to save key pair", e);
        }
    }

    /**
     * 解析 PEM 格式密钥。
     */
    private byte[] parsePem(String pem) {
        String content = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(content);
    }

    /**
     * 转换为 PEM 格式。
     */
    private String toPem(byte[] keyBytes, String label) {
        String base64 = Base64.getEncoder().encodeToString(keyBytes);
        StringBuilder pem = new StringBuilder();
        pem.append("-----BEGIN ").append(label).append("-----\n");

        // 每行 64 字符
        for (int i = 0; i < base64.length(); i += 64) {
            int end = Math.min(i + 64, base64.length());
            pem.append(base64, i, end).append("\n");
        }

        pem.append("-----END ").append(label).append("-----\n");
        return pem.toString();
    }
}
