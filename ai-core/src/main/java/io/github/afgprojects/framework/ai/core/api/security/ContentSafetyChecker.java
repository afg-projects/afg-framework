package io.github.afgprojects.framework.ai.core.api.security;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * 内容安全检查器接口
 *
 * <p>用于检查 AI 输入和输出的内容安全：
 * <ul>
 *   <li>敏感词过滤</li>
 *   <li>内容审核</li>
 *   <li>有害内容检测</li>
 *   <li>合规检查</li>
 * </ul>
 *
 * @author afg-projects
 * @since 1.0.0
 */
public interface ContentSafetyChecker {

    /**
     * 检查输入内容
     *
     * @param content 输入内容
     * @param context 检查上下文
     * @return 检查结果
     */
    @NonNull
    SafetyCheckResult checkInput(@NonNull String content, @NonNull SafetyCheckContext context);

    /**
     * 检查输出内容
     *
     * @param content 输出内容
     * @param context 检查上下文
     * @return 检查结果
     */
    @NonNull
    SafetyCheckResult checkOutput(@NonNull String content, @NonNull SafetyCheckContext context);

    /**
     * 过滤敏感内容
     *
     * @param content 原始内容
     * @param context 检查上下文
     * @return 过滤后的内容
     */
    @NonNull
    String filter(@NonNull String content, @NonNull SafetyCheckContext context);

    /**
     * 添加敏感词
     *
     * @param word      敏感词
     * @param category  类别
     * @param severity  严重程度
     */
    void addSensitiveWord(@NonNull String word, @NonNull String category, @NonNull Severity severity);

    /**
     * 移除敏感词
     *
     * @param word 敏感词
     */
    void removeSensitiveWord(@NonNull String word);

    /**
     * 获取所有敏感词类别
     *
     * @return 类别列表
     */
    @NonNull
    List<String> getCategories();

    /**
     * 检查结果接口
     */
    interface SafetyCheckResult {

        /**
         * 是否安全
         *
         * @return 是否安全
         */
        boolean isSafe();

        /**
         * 是否被阻止
         *
         * @return 是否被阻止
         */
        default boolean isBlocked() {
            return !isSafe();
        }

        /**
         * 获取阻止原因
         *
         * @return 阻止原因，如果未被阻止返回 null
         */
        default @Nullable String getReason() {
            if (!isBlocked()) {
                return null;
            }
            return "Content blocked due to: " + getRiskLevel();
        }

        /**
         * 获取风险等级
         *
         * @return 风险等级
         */
        @NonNull
        RiskLevel getRiskLevel();

        /**
         * 获取检测到的安全问题
         *
         * @return 安全问题列表
         */
        @NonNull
        List<SafetyIssue> getIssues();

        /**
         * 获取建议的处理方式
         *
         * @return 处理建议
         */
        @NonNull
        HandlingSuggestion getSuggestion();

        /**
         * 获取过滤后的内容
         *
         * @return 过滤后的内容，如果不需要过滤返回原始内容
         */
        @Nullable
        String getFilteredContent();
    }

    /**
     * 安全问题接口
     */
    interface SafetyIssue {

        /**
         * 获取问题类型
         *
         * @return 问题类型
         */
        @NonNull
        IssueType getType();

        /**
         * 获取问题类别
         *
         * @return 问题类别
         */
        @NonNull
        String getCategory();

        /**
         * 获取严重程度
         *
         * @return 严重程度
         */
        @NonNull
        Severity getSeverity();

        /**
         * 获取问题描述
         *
         * @return 问题描述
         */
        @NonNull
        String getDescription();

        /**
         * 获取问题位置（在原文中的位置）
         *
         * @return 问题位置（起始索引），如果无法定位返回 -1
         */
        int getPosition();

        /**
         * 获取问题长度
         *
         * @return 问题长度（字符数）
         */
        int getLength();

        /**
         * 获取匹配的内容片段
         *
         * @return 内容片段
         */
        @Nullable
        String getMatchedContent();
    }

    /**
     * 检查上下文接口
     */
    interface SafetyCheckContext {

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
         * 获取模型名称
         *
         * @return 模型名称
         */
        @Nullable
        String getModelName();

        /**
         * 获取操作类型
         *
         * @return 操作类型（chat, completion, embedding 等）
         */
        @NonNull
        String getOperationType();

        /**
         * 是否启用严格模式
         *
         * @return 是否启用严格模式
         */
        boolean isStrictMode();

        /**
         * 获取检查类别（只检查特定类别）
         *
         * @return 检查类别列表，如果为空则检查所有类别
         */
        @NonNull
        List<String> getCheckCategories();
    }

    /**
     * 风险等级
     */
    enum RiskLevel {
        /**
         * 无风险
         */
        NONE,
        /**
         * 低风险
         */
        LOW,
        /**
         * 中风险
         */
        MEDIUM,
        /**
         * 高风险
         */
        HIGH,
        /**
         * 严重风险
         */
        CRITICAL
    }

    /**
     * 严重程度
     */
    enum Severity {
        /**
         * 低
         */
        LOW,
        /**
         * 中
         */
        MEDIUM,
        /**
         * 高
         */
        HIGH,
        /**
         * 严重
         */
        CRITICAL
    }

    /**
     * 问题类型
     */
    enum IssueType {
        /**
         * 敏感词
         */
        SENSITIVE_WORD,
        /**
         * 个人信息
         */
        PII,
        /**
         * 有害内容
         */
        HARMFUL_CONTENT,
        /**
         * 不当言论
         */
        INAPPROPRIATE_LANGUAGE,
        /**
         * 欺诈内容
         */
        FRAUD,
        /**
         * 暴力内容
         */
        VIOLENCE,
        /**
         * 成人内容
         */
        ADULT_CONTENT,
        /**
         * 合规违规
         */
        COMPLIANCE_VIOLATION
    }

    /**
     * 处理建议
     */
    enum HandlingSuggestion {
        /**
         * 允许通过
         */
        ALLOW,
        /**
         * 过滤后允许
         */
        FILTER_AND_ALLOW,
        /**
         * 需要人工审核
         */
        REQUIRE_REVIEW,
        /**
         * 拒绝请求
         */
        REJECT,
        /**
         * 需要警告用户
         */
        WARN_USER
    }
}