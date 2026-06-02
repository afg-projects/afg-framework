package io.github.afgprojects.framework.ai.core.security;

import io.github.afgprojects.framework.ai.core.api.security.ContentSafetyChecker;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 默认内容安全检查器实现
 *
 * <p>基于敏感词库的内容安全检查器，适用于：
 * <ul>
 *   <li>敏感词过滤</li>
 *   <li>基础内容审核</li>
 *   <li>开发测试环境</li>
 * </ul>
 *
 * <p>生产环境建议集成专业内容审核服务（如阿里云内容安全、腾讯云天御）。
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class DefaultContentSafetyChecker implements ContentSafetyChecker {

    private static final Logger log = LoggerFactory.getLogger(DefaultContentSafetyChecker.class);

    private final Map<String, List<SensitiveWord>> sensitiveWords = new HashMap<>();
    private final Map<String, Pattern> patterns = new HashMap<>();

    /**
     * 创建默认内容安全检查器
     */
    public DefaultContentSafetyChecker() {
        // 初始化默认敏感词类别
        sensitiveWords.put("violence", new ArrayList<>());
        sensitiveWords.put("fraud", new ArrayList<>());
        sensitiveWords.put("adult", new ArrayList<>());
        sensitiveWords.put("politics", new ArrayList<>());
        sensitiveWords.put("custom", new ArrayList<>());

        // 添加一些示例敏感词
        addDefaultSensitiveWords();
    }

    private void addDefaultSensitiveWords() {
        // 添加一些基本的敏感词示例（实际使用时应该从配置或数据库加载）
        addSensitiveWord("暴力", "violence", Severity.HIGH);
        addSensitiveWord("诈骗", "fraud", Severity.CRITICAL);
        addSensitiveWord("赌博", "fraud", Severity.HIGH);
    }

    @Override
    @NonNull
    public SafetyCheckResult checkInput(@NonNull String content, @NonNull SafetyCheckContext context) {
        return checkContent(content, context, true);
    }

    @Override
    @NonNull
    public SafetyCheckResult checkOutput(@NonNull String content, @NonNull SafetyCheckContext context) {
        return checkContent(content, context, false);
    }

    @Override
    @NonNull
    public String filter(@NonNull String content, @NonNull SafetyCheckContext context) {
        String filtered = content;

        for (String category : getCategoriesToCheck(context)) {
            List<SensitiveWord> words = sensitiveWords.get(category);
            if (words == null) continue;

            for (SensitiveWord word : words) {
                Pattern pattern = getOrCreatePattern(word.word);
                Matcher matcher = pattern.matcher(filtered);
                filtered = matcher.replaceAll(getReplacement(word.severity));
            }
        }

        return filtered;
    }

    @Override
    public void addSensitiveWord(@NonNull String word, @NonNull String category, @NonNull Severity severity) {
        sensitiveWords.computeIfAbsent(category, k -> new ArrayList<>())
                .add(new SensitiveWord(word, category, severity));

        // 清除已有的 Pattern 缓存，以便重新编译
        patterns.remove(word.toLowerCase());

        log.info("Added sensitive word '{}' to category '{}' with severity {}", word, category, severity);
    }

    @Override
    public void removeSensitiveWord(@NonNull String word) {
        sensitiveWords.forEach((category, words) -> {
            words.removeIf(w -> w.word.equals(word));
        });

        patterns.remove(word.toLowerCase());

        log.info("Removed sensitive word '{}'", word);
    }

    @Override
    @NonNull
    public List<String> getCategories() {
        return new ArrayList<>(sensitiveWords.keySet());
    }

    private SafetyCheckResult checkContent(String content, SafetyCheckContext context, boolean isInput) {
        List<SafetyIssue> issues = new ArrayList<>();
        RiskLevel maxRiskLevel = RiskLevel.NONE;

        for (String category : getCategoriesToCheck(context)) {
            List<SensitiveWord> words = sensitiveWords.get(category);
            if (words == null) continue;

            for (SensitiveWord word : words) {
                Pattern pattern = getOrCreatePattern(word.word);
                Matcher matcher = pattern.matcher(content);

                while (matcher.find()) {
                    SafetyIssue issue = new DefaultSafetyIssue(
                            IssueType.SENSITIVE_WORD,
                            category,
                            word.severity,
                            "Found sensitive word: " + word.word,
                            matcher.start(),
                            matcher.end() - matcher.start(),
                            matcher.group()
                    );

                    issues.add(issue);

                    RiskLevel riskLevel = mapSeverityToRiskLevel(word.severity);
                    if (riskLevel.ordinal() > maxRiskLevel.ordinal()) {
                        maxRiskLevel = riskLevel;
                    }
                }
            }
        }

        HandlingSuggestion suggestion = determineSuggestion(maxRiskLevel, issues.size(), context.isStrictMode());
        String filteredContent = issues.isEmpty() ? content : filter(content, context);

        return new DefaultSafetyCheckResult(
                issues.isEmpty(),
                maxRiskLevel,
                issues,
                suggestion,
                filteredContent
        );
    }

    private List<String> getCategoriesToCheck(SafetyCheckContext context) {
        List<String> categories = context.getCheckCategories();
        if (categories.isEmpty()) {
            return getCategories();
        }
        return categories;
    }

    private Pattern getOrCreatePattern(String word) {
        return patterns.computeIfAbsent(word.toLowerCase(),
                w -> Pattern.compile(Pattern.quote(w), Pattern.CASE_INSENSITIVE));
    }

    private String getReplacement(Severity severity) {
        switch (severity) {
            case CRITICAL:
                return "***";
            case HIGH:
                return "**";
            case MEDIUM:
                return "*";
            default:
                return "*";
        }
    }

    private RiskLevel mapSeverityToRiskLevel(Severity severity) {
        switch (severity) {
            case CRITICAL:
                return RiskLevel.CRITICAL;
            case HIGH:
                return RiskLevel.HIGH;
            case MEDIUM:
                return RiskLevel.MEDIUM;
            case LOW:
                return RiskLevel.LOW;
            default:
                return RiskLevel.NONE;
        }
    }

    private HandlingSuggestion determineSuggestion(RiskLevel riskLevel, int issueCount, boolean strictMode) {
        if (riskLevel == RiskLevel.NONE) {
            return HandlingSuggestion.ALLOW;
        }

        if (riskLevel == RiskLevel.CRITICAL) {
            return HandlingSuggestion.REJECT;
        }

        if (riskLevel == RiskLevel.HIGH) {
            return strictMode ? HandlingSuggestion.REJECT : HandlingSuggestion.FILTER_AND_ALLOW;
        }

        if (riskLevel == RiskLevel.MEDIUM) {
            return strictMode ? HandlingSuggestion.FILTER_AND_ALLOW : HandlingSuggestion.WARN_USER;
        }

        return HandlingSuggestion.FILTER_AND_ALLOW;
    }

    /**
     * 敏感词
     */
    private static class SensitiveWord {
        final String word;
        final String category;
        final Severity severity;

        SensitiveWord(String word, String category, Severity severity) {
            this.word = word;
            this.category = category;
            this.severity = severity;
        }
    }

    /**
     * 默认安全检查结果
     */
    private static class DefaultSafetyCheckResult implements SafetyCheckResult {
        private final boolean safe;
        private final RiskLevel riskLevel;
        private final List<SafetyIssue> issues;
        private final HandlingSuggestion suggestion;
        private final String filteredContent;

        DefaultSafetyCheckResult(boolean safe, RiskLevel riskLevel, List<SafetyIssue> issues,
                                  HandlingSuggestion suggestion, String filteredContent) {
            this.safe = safe;
            this.riskLevel = riskLevel;
            this.issues = issues;
            this.suggestion = suggestion;
            this.filteredContent = filteredContent;
        }

        @Override
        public boolean isSafe() { return safe; }

        @Override
        @NonNull
        public RiskLevel getRiskLevel() { return riskLevel; }

        @Override
        @NonNull
        public List<SafetyIssue> getIssues() { return issues; }

        @Override
        @NonNull
        public HandlingSuggestion getSuggestion() { return suggestion; }

        @Override
        @Nullable
        public String getFilteredContent() { return filteredContent; }
    }

    /**
     * 默认安全问题
     */
    private static class DefaultSafetyIssue implements SafetyIssue {
        private final IssueType type;
        private final String category;
        private final Severity severity;
        private final String description;
        private final int position;
        private final int length;
        private final String matchedContent;

        DefaultSafetyIssue(IssueType type, String category, Severity severity, String description,
                           int position, int length, String matchedContent) {
            this.type = type;
            this.category = category;
            this.severity = severity;
            this.description = description;
            this.position = position;
            this.length = length;
            this.matchedContent = matchedContent;
        }

        @Override
        @NonNull
        public IssueType getType() { return type; }

        @Override
        @NonNull
        public String getCategory() { return category; }

        @Override
        @NonNull
        public Severity getSeverity() { return severity; }

        @Override
        @NonNull
        public String getDescription() { return description; }

        @Override
        public int getPosition() { return position; }

        @Override
        public int getLength() { return length; }

        @Override
        @Nullable
        public String getMatchedContent() { return matchedContent; }
    }
}