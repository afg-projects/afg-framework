package io.github.afgprojects.framework.ai.core.security;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * PII（个人身份信息）检测器接口
 *
 * <p>用于检测和处理文本中的个人信息：
 * <ul>
 *   <li>检测 PII 类型</li>
 *   <li>脱敏处理</li>
 *   <li>还原脱敏内容</li>
 * </ul>
 *
 * @author afg-projects
 * @since 1.0.0
 */
public interface PiiDetector {

    /**
     * 检测文本中的 PII
     *
     * @param text    原始文本
     * @param context 检测上下文
     * @return 检测结果
     */
    @NonNull
    PiiDetectionResult detect(@NonNull String text, @NonNull PiiContext context);

    /**
     * 脱敏文本
     *
     * @param text    原始文本
     * @param context 检测上下文
     * @return 脱敏结果
     */
    @NonNull
    PiiMaskingResult mask(@NonNull String text, @NonNull PiiContext context);

    /**
     * 脱敏文本（指定类型）
     *
     * @param text        原始文本
     * @param piiTypes    要脱敏的 PII 类型
     * @param context     检测上下文
     * @return 脱敏结果
     */
    @NonNull
    PiiMaskingResult mask(@NonNull String text, @NonNull List<PiiType> piiTypes, @NonNull PiiContext context);

    /**
     * 还原脱敏内容
     *
     * @param maskedText   脱敏后的文本
     * @param maskingToken 脱敏令牌（包含原始值映射）
     * @return 还原后的文本
     */
    @NonNull
    String unmask(@NonNull String maskedText, @NonNull MaskingToken maskingToken);

    /**
     * 检测结果接口
     */
    interface PiiDetectionResult {

        /**
         * 是否检测到 PII
         *
         * @return 是否检测到 PII
         */
        boolean hasPii();

        /**
         * 获取检测到的 PII 列表
         *
         * @return PII 列表
         */
        @NonNull
        List<PiiMatch> getPiiMatches();

        /**
         * 获取检测到的 PII 类型
         *
         * @return PII 类型列表
         */
        @NonNull
        List<PiiType> getPiiTypes();

        /**
         * 获取风险等级
         *
         * @return 风险等级
         */
        ContentSafetyChecker.@NonNull RiskLevel getRiskLevel();
    }

    /**
     * PII 匹配接口
     */
    interface PiiMatch {

        /**
         * 获取 PII 类型
         *
         * @return PII 类型
         */
        @NonNull
        PiiType getType();

        /**
         * 获取匹配的文本
         *
         * @return 匹配的文本
         */
        @NonNull
        String getText();

        /**
         * 获取起始位置
         *
         * @return 起始位置
         */
        int getStart();

        /**
         * 获取结束位置
         *
         * @return 结束位置
         */
        int getEnd();

        /**
         * 获取置信度
         *
         * @return 置信度（0-1）
         */
        double getConfidence();

        /**
         * 获取脱敏后的值
         *
         * @return 脱敏后的值
         */
        @NonNull
        String getMaskedValue();
    }

    /**
     * 脱敏结果接口
     */
    interface PiiMaskingResult {

        /**
         * 获取脱敏后的文本
         *
         * @return 脱敏后的文本
         */
        @NonNull
        String getMaskedText();

        /**
         * 获取脱敏令牌（用于还原）
         *
         * @return 脱敏令牌
         */
        @NonNull
        MaskingToken getMaskingToken();

        /**
         * 获取脱敏的 PII 数量
         *
         * @return 脱敏数量
         */
        int getMaskedCount();

        /**
         * 获取脱敏的 PII 类型
         *
         * @return PII 类型列表
         */
        @NonNull
        List<PiiType> getMaskedTypes();
    }

    /**
     * 脱敏令牌接口
     */
    interface MaskingToken {

        /**
         * 获取令牌 ID
         *
         * @return 令牌 ID
         */
        @NonNull
        String getTokenId();

        /**
         * 获取原始值到脱敏值的映射
         *
         * @return 映射
         */
        @NonNull
        Map<String, String> getMappings();

        /**
         * 获取脱敏值到原始值的映射
         *
         * @return 映射
         */
        @NonNull
        Map<String, String> getReverseMappings();

        /**
         * 获取创建时间
         *
         * @return 创建时间（毫秒时间戳）
         */
        long getCreatedAt();

        /**
         * 是否已过期
         *
         * @return 是否已过期
         */
        boolean isExpired();
    }

    /**
     * 检测上下文接口
     */
    interface PiiContext {

        /**
         * 获取用户 ID
         *
         * @return 用户 ID
         */
        @Nullable
        String getUserId();

        /**
         * 获取租户 ID
         *
         * @return 租户 ID
         */
        @Nullable
        String getTenantId();

        /**
         * 获取要检测的 PII 类型
         *
         * @return PII 类型列表，如果为空则检测所有类型
         */
        @NonNull
        List<PiiType> getDetectTypes();

        /**
         * 获取脱敏策略
         *
         * @return 脱敏策略
         */
        @NonNull
        MaskingStrategy getMaskingStrategy();

        /**
         * 获取最小置信度阈值
         *
         * @return 最小置信度（0-1）
         */
        double getMinConfidence();
    }

    /**
     * PII 类型
     */
    enum PiiType {
        /**
         * 姓名
         */
        NAME,
        /**
         * 电子邮件
         */
        EMAIL,
        /**
         * 电话号码
         */
        PHONE,
        /**
         * 身份证号
         */
        ID_NUMBER,
        /**
         * 信用卡号
         */
        CREDIT_CARD,
        /**
         * 银行账号
         */
        BANK_ACCOUNT,
        /**
         * 地址
         */
        ADDRESS,
        /**
         * 社保号
         */
        SOCIAL_SECURITY_NUMBER,
        /**
         * 护照号
         */
        PASSPORT_NUMBER,
        /**
         * 驾照号
         */
        DRIVER_LICENSE,
        /**
         * IP 地址
         */
        IP_ADDRESS,
        /**
         * MAC 地址
         */
        MAC_ADDRESS,
        /**
         * 出生日期
         */
        BIRTH_DATE,
        /**
         * 年龄
         */
        AGE,
        /**
         * 性别
         */
        GENDER,
        /**
         * 用户名
         */
        USERNAME,
        /**
         * 密码
         */
        PASSWORD,
        /**
         * API Key
         */
        API_KEY,
        /**
         * 其他
         */
        OTHER
    }

    /**
     * 脱敏策略
     */
    enum MaskingStrategy {
        /**
         * 完全替换（如 ***）
         */
        FULL_MASK,
        /**
         * 部分替换（如 138****1234）
         */
        PARTIAL_MASK,
        /**
         * 哈希替换
         */
        HASH_MASK,
        /**
         * 随机替换
         */
        RANDOM_MASK,
        /**
         * 类型标签（如 [PHONE]）
         */
        TYPE_LABEL
    }
}