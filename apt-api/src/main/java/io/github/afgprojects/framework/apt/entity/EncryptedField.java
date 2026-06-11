package io.github.afgprojects.framework.apt.entity;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 字段加密存储标记注解。
 * <p>
 * 标注在 String 字段上，APT 处理器会将加密标记写入元数据，
 * 运行时 JdbcDataManager 在写入/读取时自动加解密。
 *
 * <h2>基础用法</h2>
 * <pre>{@code
 * @AfEntity
 * @Table(name = "sys_user")
 * public class User extends SoftDeleteEntity {
 *
 *     @EncryptedField(algorithm = "AES", keyRef = "user-key")
 *     private String idCard;  // 身份证号加密存储
 *
 *     @EncryptedField(algorithm = "AES", keyRef = "phone-key")
 *     private String phone;   // 手机号加密存储
 * }
 * }</pre>
 *
 * <h2>使用默认算法</h2>
 * <pre>{@code
 * @EncryptedField  // 默认使用 AES 算法
 * private String bankAccount;
 * }</pre>
 *
 * <p>注意：此注解只能标注在 String 类型的字段上，标注在非 String 字段会导致编译错误。
 * <p>注意：运行时加解密行为在 data-jdbc 模块实现，此注解仅负责编译时元数据标记。
 *
 * @see AfEntity
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface EncryptedField {

    /**
     * 加密算法。
     * <p>
     * 指定字段使用的加密算法。目前支持 AES。
     *
     * @return 加密算法名称，默认 "AES"
     */
    String algorithm() default "AES";

    /**
     * 密钥引用名称。
     * <p>
     * 对应 afg.data.field-encryption.keys.{keyRef} 配置项。
     * 为空时使用默认密钥。
     *
     * <p>示例：
     * <pre>{@code
     * @EncryptedField(keyRef = "user-key")
     * }</pre>
     *
     * 对应配置：
     * <pre>
     * afg:
     *   data:
     *     field-encryption:
     *       keys:
     *         user-key: "base64-encoded-key"
     * </pre>
     *
     * @return 密钥引用名称，默认空字符串表示使用默认密钥
     */
    String keyRef() default "";
}
