package io.github.afgprojects.framework.apt.entity;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 敏感字段标记注解，用于数据脱敏。
 * <p>
 * 标注在实体字段上，APT 处理器将脱敏标记写入元数据，
 * 运行时 Jackson 序列化层根据此标记动态应用脱敏策略。
 *
 * <h2>用法示例</h2>
 * <pre>{@code
 * @AfEntity
 * @Table(name = "sys_user")
 * public class User extends SoftDeleteEntity {
 *
 *     @SensitiveField(type = SensitiveType.PHONE)
 *     private String phone;
 *
 *     @SensitiveField(type = SensitiveType.ID_CARD)
 *     private String idCard;
 *
 *     @SensitiveField(type = SensitiveType.EMAIL)
 *     private String email;
 * }
 * }</pre>
 *
 * <p>注意：此注解保留策略为 RUNTIME，以便 Jackson 序列化层在运行时读取。
 * <p>注意：脱敏仅影响 Jackson 序列化输出和导出，实体对象本身持有真实值。
 * <p>注意：此注解只能标注在 String 类型的字段上。
 *
 * @see AfEntity
 * @see io.github.afgprojects.framework.data.core.sensitive.SensitiveType
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SensitiveField {

    /**
     * 敏感数据类型。
     * <p>
     * 指定字段的敏感数据类型，决定使用的脱敏策略。
     * CUSTOM 类型允许自定义脱敏逻辑。
     *
     * @return 敏感数据类型
     */
    SensitiveType type() default SensitiveType.CUSTOM;

    /**
     * 自定义脱敏策略 Bean 名称。
     * <p>
     * 仅当 type 为 CUSTOM 时生效。指定 Spring 容器中的
     * {@link io.github.afgprojects.framework.data.core.sensitive.MaskingStrategy} Bean 名称。
     *
     * @return 自定义策略 Bean 名称
     */
    String strategy() default "";
}
