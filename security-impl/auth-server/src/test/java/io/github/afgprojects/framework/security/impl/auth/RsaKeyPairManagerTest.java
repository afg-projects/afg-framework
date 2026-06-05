package io.github.afgprojects.framework.security.impl.auth;

import io.github.afgprojects.framework.security.auth.key.RsaKeyPairManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RsaKeyPairManager 测试
 *
 * <p>测试 RSA 密钥对生成和基本操作，不测试 PEM 持久化。
 */
@DisplayName("RsaKeyPairManager 测试")
class RsaKeyPairManagerTest {

    @Nested
    @DisplayName("密钥对生成")
    class KeyPairGenerationTests {

        @Test
        @DisplayName("应生成 2048 位 RSA 密钥对")
        void shouldGenerate2048BitRsaKeyPair() throws Exception {
            // 创建临时目录作为密钥存储路径
            Path tempDir = Files.createTempDirectory("afg-keys-test");
            String keyStorePath = "file:" + tempDir.toString();

            RsaKeyPairManager manager = new RsaKeyPairManager(keyStorePath);

            java.security.KeyPair keyPair = manager.getKeyPair();
            assertThat(keyPair).isNotNull();
            assertThat(keyPair.getPrivate()).isNotNull();
            assertThat(keyPair.getPublic()).isNotNull();

            // 清理
            Files.deleteIfExists(tempDir.resolve("private_key.pem"));
            Files.deleteIfExists(tempDir.resolve("public_key.pem"));
            Files.deleteIfExists(tempDir);
        }

        @Test
        @DisplayName("RSA 私钥应为 RSAPrivateKey 类型")
        void shouldReturnRsaPrivateKey() throws Exception {
            Path tempDir = Files.createTempDirectory("afg-keys-test");
            String keyStorePath = "file:" + tempDir.toString();

            RsaKeyPairManager manager = new RsaKeyPairManager(keyStorePath);

            RSAPrivateKey privateKey = manager.getPrivateKey();
            assertThat(privateKey).isNotNull();
            assertThat(privateKey.getModulus().bitLength()).isGreaterThanOrEqualTo(2048);

            // 清理
            Files.deleteIfExists(tempDir.resolve("private_key.pem"));
            Files.deleteIfExists(tempDir.resolve("public_key.pem"));
            Files.deleteIfExists(tempDir);
        }

        @Test
        @DisplayName("RSA 公钥应为 RSAPublicKey 类型")
        void shouldReturnRsaPublicKey() throws Exception {
            Path tempDir = Files.createTempDirectory("afg-keys-test");
            String keyStorePath = "file:" + tempDir.toString();

            RsaKeyPairManager manager = new RsaKeyPairManager(keyStorePath);

            RSAPublicKey publicKey = manager.getPublicKey();
            assertThat(publicKey).isNotNull();
            assertThat(publicKey.getModulus().bitLength()).isGreaterThanOrEqualTo(2048);

            // 清理
            Files.deleteIfExists(tempDir.resolve("private_key.pem"));
            Files.deleteIfExists(tempDir.resolve("public_key.pem"));
            Files.deleteIfExists(tempDir);
        }
    }

    @Nested
    @DisplayName("KeyId 生成")
    class KeyIdTests {

        @Test
        @DisplayName("KeyId 应为 UUID 格式")
        void shouldGenerateUuidKeyId() throws Exception {
            Path tempDir = Files.createTempDirectory("afg-keys-test");
            String keyStorePath = "file:" + tempDir.toString();

            RsaKeyPairManager manager = new RsaKeyPairManager(keyStorePath);

            String keyId = manager.getKeyId();
            assertThat(keyId).isNotNull();
            assertThat(keyId).isNotBlank();
            // UUID 格式: 8-4-4-4-12
            assertThat(keyId).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

            // 清理
            Files.deleteIfExists(tempDir.resolve("private_key.pem"));
            Files.deleteIfExists(tempDir.resolve("public_key.pem"));
            Files.deleteIfExists(tempDir);
        }

        @Test
        @DisplayName("每个实例的 KeyId 应唯一")
        void shouldGenerateUniqueKeyIdPerInstance() throws Exception {
            Path tempDir1 = Files.createTempDirectory("afg-keys-test-1");
            Path tempDir2 = Files.createTempDirectory("afg-keys-test-2");

            RsaKeyPairManager manager1 = new RsaKeyPairManager("file:" + tempDir1.toString());
            RsaKeyPairManager manager2 = new RsaKeyPairManager("file:" + tempDir2.toString());

            assertThat(manager1.getKeyId()).isNotEqualTo(manager2.getKeyId());

            // 清理
            Files.deleteIfExists(tempDir1.resolve("private_key.pem"));
            Files.deleteIfExists(tempDir1.resolve("public_key.pem"));
            Files.deleteIfExists(tempDir1);
            Files.deleteIfExists(tempDir2.resolve("private_key.pem"));
            Files.deleteIfExists(tempDir2.resolve("public_key.pem"));
            Files.deleteIfExists(tempDir2);
        }
    }

    @Nested
    @DisplayName("JWK Set 生成")
    class JwkSetTests {

        @Test
        @DisplayName("应生成有效的 JWK Set JSON")
        void shouldGenerateValidJwkSetJson() throws Exception {
            Path tempDir = Files.createTempDirectory("afg-keys-test");
            String keyStorePath = "file:" + tempDir.toString();

            RsaKeyPairManager manager = new RsaKeyPairManager(keyStorePath);

            String jwkSetJson = manager.getJwkSetJson();
            assertThat(jwkSetJson).isNotNull();
            assertThat(jwkSetJson).isNotBlank();
            assertThat(jwkSetJson).contains("keys");
            assertThat(jwkSetJson).contains("kty");
            assertThat(jwkSetJson).contains("RSA");
            assertThat(jwkSetJson).contains("kid");
            assertThat(jwkSetJson).contains(manager.getKeyId());

            // 清理
            Files.deleteIfExists(tempDir.resolve("private_key.pem"));
            Files.deleteIfExists(tempDir.resolve("public_key.pem"));
            Files.deleteIfExists(tempDir);
        }
    }

    @Nested
    @DisplayName("密钥文件持久化")
    class PersistenceTests {

        @Test
        @DisplayName("应将密钥持久化为 PEM 文件")
        void shouldPersistKeyPairAsPemFiles() throws Exception {
            Path tempDir = Files.createTempDirectory("afg-keys-test");
            String keyStorePath = "file:" + tempDir.toString();

            new RsaKeyPairManager(keyStorePath);

            Path privateKeyPath = tempDir.resolve("private_key.pem");
            Path publicKeyPath = tempDir.resolve("public_key.pem");

            assertThat(Files.exists(privateKeyPath)).isTrue();
            assertThat(Files.exists(publicKeyPath)).isTrue();

            String privateKeyContent = Files.readString(privateKeyPath);
            assertThat(privateKeyContent).contains("BEGIN PRIVATE KEY");
            assertThat(privateKeyContent).contains("END PRIVATE KEY");

            String publicKeyContent = Files.readString(publicKeyPath);
            assertThat(publicKeyContent).contains("BEGIN PUBLIC KEY");
            assertThat(publicKeyContent).contains("END PUBLIC KEY");

            // 清理
            Files.deleteIfExists(privateKeyPath);
            Files.deleteIfExists(publicKeyPath);
            Files.deleteIfExists(tempDir);
        }

        @Test
        @DisplayName("应能从已存在的 PEM 文件加载密钥")
        void shouldLoadKeyPairFromExistingPemFiles() throws Exception {
            Path tempDir = Files.createTempDirectory("afg-keys-test");
            String keyStorePath = "file:" + tempDir.toString();

            // 第一次创建（生成新密钥）
            RsaKeyPairManager manager1 = new RsaKeyPairManager(keyStorePath);
            RSAPublicKey publicKey1 = manager1.getPublicKey();
            String keyId1 = manager1.getKeyId();

            // 第二次创建（从 PEM 文件加载）
            RsaKeyPairManager manager2 = new RsaKeyPairManager(keyStorePath);
            RSAPublicKey publicKey2 = manager2.getPublicKey();

            // 加载的密钥应与原始密钥相同
            assertThat(publicKey2.getModulus()).isEqualTo(publicKey1.getModulus());
            assertThat(publicKey2.getPublicExponent()).isEqualTo(publicKey1.getPublicExponent());

            // 清理
            Files.deleteIfExists(tempDir.resolve("private_key.pem"));
            Files.deleteIfExists(tempDir.resolve("public_key.pem"));
            Files.deleteIfExists(tempDir);
        }
    }
}