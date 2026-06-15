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
 * <h2>启用盲索引（支持加密字段等值查询）</h2>
 * <pre>{@code
 * @EncryptedField(blindIndexColumn = "phone_hash")
 * @Column(name = "phone")
 * private String phone;  // phone 列存密文，phone_hash 列存 HMAC 盲索引
 *
 * // 查询时自动走盲索引列：
 * // Conditions.builder(User.class).eq(User::getPhone, "13800001234")
 * // → WHERE phone_hash = hmac("13800001234")
 * }</pre>
 *
 * <p>注意：此注解只能标注在 String 类型的字段上，标注在非 String 字段会导致编译错误。
 * <p>注意：运行时加解密行为在 data-jdbc 模块实现，此注解仅负责编译时元数据标记。
 * <p>注意：启用盲索引后，数据库表必须包含对应的盲索引列（通过 Liquibase 迁移添加）。
 *       盲索引列不支持 LIKE/范围查询，仅支持 EQ/NE/IN/NOT_IN 操作符。
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
     * 对应 afg.data.encryption.keys.{keyRef} 配置项。
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
     *     encryption:
     *       keys:
     *         user-key: "base64-encoded-key"
     * </pre>
     *
     * @return 密钥引用名称，默认空字符串表示使用默认密钥
     */
    String keyRef() default "";

    /**
     * 盲索引列名（数据库列名）。
     * <p>
     * 为空时表示不使用盲索引列。启用后，框架在写入时计算 HMAC-SHA256 盲索引值
     * 并写入该列，查询时自动将条件值转为 HMAC 值走盲索引匹配。
     *
     * <p>盲索引列命名约定：
     * <ul>
     *   <li>默认（不指定）：不使用盲索引</li>
     *   <li>自定义：指定数据库列名，如 "phone_hash"</li>
     * </ul>
     *
     * <p>注意：盲索引列需要在数据库迁移中手动添加，框架不自动创建。
     *
     * @return 盲索引列名，默认空字符串表示不使用盲索引
     */
    String blindIndexColumn() default "";
}
