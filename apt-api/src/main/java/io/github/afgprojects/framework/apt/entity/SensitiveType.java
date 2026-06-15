package io.github.afgprojects.framework.apt.entity;

/**
 * 敏感数据类型枚举，定义标准脱敏策略。
 *
 * <p>每种类型对应不同的脱敏规则（如手机号保留前3后4，邮箱保留前缀首字符+域名）。
 * CUSTOM 类型允许通过 {@link SensitiveField#strategy()} 指定自定义脱敏策略 Bean。
 *
 * <h3>标准脱敏规则（遵循 GB/T 35273）</h3>
 * <table>
 *   <tr><th>类型</th><th>输入示例</th><th>输出示例</th></tr>
 *   <tr><td>PHONE</td><td>13812345678</td><td>138****5678</td></tr>
 *   <tr><td>ID_CARD</td><td>110101199001011234</td><td>110101********1234</td></tr>
 *   <tr><td>EMAIL</td><td>test@example.com</td><td>t***@example.com</td></tr>
 *   <tr><td>BANK_CARD</td><td>6222021234567890123</td><td>622202*********0123</td></tr>
 *   <tr><td>NAME</td><td>ZhangSan</td><td>Zha***</td></tr>
 *   <tr><td>ADDRESS</td><td>No.123 Some Road, Beijing</td><td>No.123***</td></tr>
 *   <tr><td>CUSTOM</td><td>(自定义)</td><td>(由 strategy 决定)</td></tr>
 * </table>
 */
public enum SensitiveType {

    /** 手机号：保留前3后4，中间4位脱敏 */
    PHONE,

    /** 身份证号：保留前6后4，中间8位脱敏 */
    ID_CARD,

    /** 邮箱：保留前缀首字符 + @域名 */
    EMAIL,

    /** 银行卡号：保留前6后4，中间脱敏 */
    BANK_CARD,

    /** 姓名：保留前3个字符，其余脱敏 */
    NAME,

    /** 地址：保留前6个字符，其余脱敏 */
    ADDRESS,

    /** 自定义类型：通过 SensitiveField.strategy() 指定脱敏策略 Bean */
    CUSTOM
}
