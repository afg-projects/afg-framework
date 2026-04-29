package io.github.afgprojects.framework.core.web.security.util;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 敏感数据脱敏工具
 * <p>
 * 支持通过 {@link #registerSensitiveFields(Set)} 方法动态注册额外的敏感字段。
 * </p>
 */
public final class SensitiveDataMasker {

    /**
     * 默认敏感字段集合（不可变）
     */
    private static final Set<String> DEFAULT_SENSITIVE_FIELDS = Set.of(
            // 认证凭证类
            "password",
            "pwd",
            "token",
            "secret",
            "apikey",
            "credential",
            "accesstoken",
            "refreshtoken",
            "privatekey",
            "sessionid",
            // 个人身份信息类
            "ssn",           // 社会安全号
            "idcard",        // 身份证号
            "idnumber",      // 证件号码
            "passport",      // 护照号
            "driverlicense", // 驾驶证号
            "licensenumber", // 证照号码
            "birthday",      // 出生日期
            "birthdate",     // 出生日期
            // 金融信息类
            "creditcard",    // 信用卡号
            "bankcard",      // 银行卡号
            "bankaccount",   // 银行账户
            "accountnumber", // 账号
            "taxid",         // 税号
            // 联系方式类
            "phone",         // 手机号
            "mobile",        // 手机号
            "telephone",     // 电话号码
            "email",         // 邮箱
            "address",       // 地址
            "homeaddress",   // 家庭住址
            "workaddress",   // 工作地址
            // 其他敏感信息
            "salary",        // 薪资
            "income",        // 收入
            "realname",      // 真实姓名
            "truename"       // 真实姓名
    );

    /**
     * 扩展敏感字段集合（可通过配置动态添加）
     */
    private static final Set<String> EXTENDED_SENSITIVE_FIELDS = ConcurrentHashMap.newKeySet();

    private static final int VISIBLE_LENGTH = 3;
    private static final String MASK = "***";

    private SensitiveDataMasker() {}

    /**
     * 注册额外的敏感字段
     *
     * @param fields 敏感字段名集合（不区分大小写，下划线会被忽略）
     */
    public static void registerSensitiveFields(Set<String> fields) {
        if (fields == null || fields.isEmpty()) {
            return;
        }
        for (String field : fields) {
            if (field != null && !field.isBlank()) {
                EXTENDED_SENSITIVE_FIELDS.add(field.toLowerCase(Locale.ROOT).replace("_", ""));
            }
        }
    }

    /**
     * 清除扩展的敏感字段（用于测试）
     */
    public static void clearExtendedFields() {
        EXTENDED_SENSITIVE_FIELDS.clear();
    }

    /**
     * 判断字段名是否为敏感字段
     *
     * @param fieldName 字段名
     * @return 如果是敏感字段返回 true
     */
    public static boolean isSensitive(String fieldName) {
        if (fieldName == null || fieldName.isBlank()) {
            return false;
        }
        String normalized = fieldName.toLowerCase(Locale.ROOT).replace("_", "");
        return DEFAULT_SENSITIVE_FIELDS.contains(normalized) || EXTENDED_SENSITIVE_FIELDS.contains(normalized);
    }

    /**
     * 对字段值进行脱敏
     *
     * @param fieldName 字段名
     * @param value     字段值
     * @return 脱敏后的值
     */
    public static String mask(String fieldName, String value) {
        if (value == null) {
            return null;
        }
        if (value.isEmpty()) {
            return value;
        }
        if (!isSensitive(fieldName)) {
            return value;
        }

        if (value.length() <= VISIBLE_LENGTH) {
            return MASK;
        }

        return value.substring(0, VISIBLE_LENGTH) + MASK;
    }
}
