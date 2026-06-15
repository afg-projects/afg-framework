package io.github.afgprojects.framework.data.core.sensitive;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

/**
 * 默认脱敏策略实现（遵循 GB/T 35273 标准）。
 * <p>
 * 支持 PHONE、ID_CARD、EMAIL、BANK_CARD、NAME、ADDRESS 六种标准脱敏类型。
 * 对于 CUSTOM 类型，由调用方通过自定义 {@link MaskingStrategy} Bean 处理。
 *
 * <h3>脱敏规则</h3>
 * <table>
 *   <tr><th>类型</th><th>规则</th><th>示例</th></tr>
 *   <tr><td>PHONE</td><td>保留前3后4</td><td>138****5678</td></tr>
 *   <tr><td>ID_CARD</td><td>保留前6后4</td><td>110101********1234</td></tr>
 *   <tr><td>EMAIL</td><td>保留首字符+@域名</td><td>t***@example.com</td></tr>
 *   <tr><td>BANK_CARD</td><td>保留前6后4</td><td>622202*********0123</td></tr>
 *   <tr><td>NAME</td><td>保留前3字符</td><td>Zha***</td></tr>
 *   <tr><td>ADDRESS</td><td>保留前6字符</td><td>No.123***</td></tr>
 * </table>
 */
@Slf4j
public class DefaultMaskingStrategy implements MaskingStrategy {

    @Override
    public @Nullable String mask(@Nullable String value, String sensitiveType, @Nullable String fieldName) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        try {
            switch (sensitiveType) {
                case "PHONE" -> {
                    return maskPhone(value);
                }
                case "ID_CARD" -> {
                    return maskIdCard(value);
                }
                case "EMAIL" -> {
                    return maskEmail(value);
                }
                case "BANK_CARD" -> {
                    return maskBankCard(value);
                }
                case "NAME" -> {
                    return maskName(value);
                }
                case "ADDRESS" -> {
                    return maskAddress(value);
                }
                default -> {
                    return value; // CUSTOM or unknown type — caller should handle via custom strategy
                }
            }
        } catch (Exception e) {
            log.debug("Masking failed for field {} (type={}): {}", fieldName, sensitiveType, e.getMessage());
            return "***";
        }
    }

    /**
     * 手机号脱敏：保留前3后4，中间4位用 **** 替换。
     */
    private String maskPhone(String value) {
        if (value.length() < 7) {
            return value;
        }
        return value.charAt(0) + maskMiddle(value.substring(1), 2, 2, 4);
    }

    /**
     * 身份证号脱敏：保留前6后4，中间8位用 ******** 替换。
     */
    private String maskIdCard(String value) {
        if (value.length() < 10) {
            return maskAll(value);
        }
        return value.substring(0, 6) + repeat('*', 8) + value.substring(value.length() - 4);
    }

    /**
     * 邮箱脱敏：保留@前首字符 + ***@域名。
     */
    private String maskEmail(String value) {
        int atIndex = value.indexOf('@');
        if (atIndex <= 1) {
            return value;
        }
        return value.charAt(0) + "***" + value.substring(atIndex);
    }

    /**
     * 银行卡号脱敏：保留前6后4，中间用 * 填充。
     */
    private String maskBankCard(String value) {
        if (value.length() < 10) {
            return maskAll(value);
        }
        int maskLen = value.length() - 10;
        return value.substring(0, 6) + repeat('*', maskLen) + value.substring(value.length() - 4);
    }

    /**
     * 姓名脱敏：保留前3个字符，其余用 *** 替换。
     */
    private String maskName(String value) {
        if (value.length() <= 3) {
            return value.substring(0, 1) + "***";
        }
        return value.substring(0, 3) + "***";
    }

    /**
     * 地址脱敏：保留前6个字符，其余用 *** 替换。
     */
    private String maskAddress(String value) {
        if (value.length() <= 6) {
            return value.substring(0, 1) + "***";
        }
        return value.substring(0, 6) + "***";
    }

    /**
     * 全部脱敏：用 *** 替换整个值。
     */
    private String maskAll(String value) {
        return "***";
    }

    /**
     * 脱敏中间部分，保留前后各N个字符。
     */
    private String maskMiddle(String value, int keepPrefix, int keepSuffix, int maskCount) {
        int total = value.length();
        if (total <= keepPrefix + keepSuffix) {
            return value;
        }
        return value.substring(0, keepPrefix) + repeat('*', maskCount) + value.substring(total - keepSuffix);
    }

    private String repeat(char c, int count) {
        return String.valueOf(c).repeat(Math.max(0, count));
    }
}
