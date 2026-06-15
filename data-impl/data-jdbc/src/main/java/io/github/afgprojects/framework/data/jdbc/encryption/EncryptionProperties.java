package io.github.afgprojects.framework.data.jdbc.encryption;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * 字段加密配置属性
 * <p>
 * 配置前缀：{@code afg.data.encryption}
 *
 * <h3>配置示例</h3>
 * <pre>
 * afg:
 *   data:
 *     encryption:
 *       enabled: true
 *       strict-mode: true
 *       default-key: "base64-encoded-32-byte-key"
 *       keys:
 *         user-key: "base64-encoded-32-byte-key"
 *         phone-key: "base64-encoded-32-byte-key"
 * </pre>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "afg.data.encryption")
public class EncryptionProperties {

    /**
     * 是否启用字段加密
     */
    private boolean enabled = true;

    /**
     * 严格模式：当存在 @EncryptedField 标注的实体但无真实 FieldEncryptor 时，
     * 应用启动拒绝（抛 BeanCreationException）。
     * <p>
     * 设为 false 时，仅记录 ERROR 日志但仍启动（向后兼容）。
     */
    private boolean strictMode = true;

    /**
     * 默认加密密钥（Base64 编码的 AES-256 密钥，32 字节）
     * <p>
     * 当 @EncryptedField(keyRef="") 时使用此密钥。
     * 必须是有效的 Base64 编码字符串，解码后长度为 32 字节。
     */
    private String defaultKey;

    /**
     * 按密钥引用名称的密钥映射
     * <p>
     * key: keyRef 名称（对应 @EncryptedField(keyRef)）
     * value: Base64 编码的 AES-256 密钥（32 字节）
     */
    private Map<String, String> keys = new HashMap<>();
}
