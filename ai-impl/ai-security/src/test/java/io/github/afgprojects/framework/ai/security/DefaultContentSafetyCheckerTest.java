package io.github.afgprojects.framework.ai.security;

import io.github.afgprojects.framework.ai.core.security.ContentSafetyChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DefaultContentSafetyChecker 单元测试
 */
class DefaultContentSafetyCheckerTest {

    private DefaultContentSafetyChecker checker;

    @BeforeEach
    void setUp() {
        checker = new DefaultContentSafetyChecker();
    }

    @Test
    @DisplayName("检查安全内容")
    void checkInput_safeContent() {
        ContentSafetyChecker.SafetyCheckContext context = createContext();

        ContentSafetyChecker.SafetyCheckResult result = checker.checkInput("Hello, how are you?", context);

        assertThat(result.isSafe()).isTrue();
        assertThat(result.getRiskLevel()).isEqualTo(ContentSafetyChecker.RiskLevel.NONE);
        assertThat(result.getIssues()).isEmpty();
    }

    @Test
    @DisplayName("检查包含敏感词的内容")
    void checkInput_sensitiveContent() {
        checker.addSensitiveWord("test-word", "custom", ContentSafetyChecker.Severity.HIGH);
        ContentSafetyChecker.SafetyCheckContext context = createContext();

        ContentSafetyChecker.SafetyCheckResult result = checker.checkInput("This contains test-word in the text", context);

        assertThat(result.isSafe()).isFalse();
        assertThat(result.getRiskLevel()).isEqualTo(ContentSafetyChecker.RiskLevel.HIGH);
        assertThat(result.getIssues()).hasSize(1);
    }

    @Test
    @DisplayName("过滤敏感内容")
    void filter() {
        checker.addSensitiveWord("secret", "custom", ContentSafetyChecker.Severity.HIGH);
        ContentSafetyChecker.SafetyCheckContext context = createContext();

        String filtered = checker.filter("This is a secret message", context);

        assertThat(filtered).isEqualTo("This is a ** message");
    }

    @Test
    @DisplayName("添加和移除敏感词")
    void addAndRemoveSensitiveWord() {
        checker.addSensitiveWord("badword", "custom", ContentSafetyChecker.Severity.MEDIUM);
        ContentSafetyChecker.SafetyCheckContext context = createContext();

        ContentSafetyChecker.SafetyCheckResult result1 = checker.checkInput("badword is here", context);
        assertThat(result1.getIssues()).hasSize(1);

        checker.removeSensitiveWord("badword");

        ContentSafetyChecker.SafetyCheckResult result2 = checker.checkInput("badword is here", context);
        assertThat(result2.getIssues()).isEmpty();
    }

    @Test
    @DisplayName("获取所有类别")
    void getCategories() {
        List<String> categories = checker.getCategories();

        assertThat(categories).contains("violence", "fraud", "adult", "politics", "custom");
    }

    @Test
    @DisplayName("严格模式拒绝高风险内容")
    void checkInput_strictMode() {
        checker.addSensitiveWord("dangerous", "custom", ContentSafetyChecker.Severity.HIGH);
        ContentSafetyChecker.SafetyCheckContext context = new ContentSafetyChecker.SafetyCheckContext() {
            @Override
            public String getUserId() { return "user-001"; }
            @Override
            public String getTenantId() { return "tenant-001"; }
            @Override
            public String getModelName() { return "gpt-4"; }
            @Override
            public String getOperationType() { return "chat"; }
            @Override
            public boolean isStrictMode() { return true; }
            @Override
            public List<String> getCheckCategories() { return List.of(); }
        };

        ContentSafetyChecker.SafetyCheckResult result = checker.checkInput("dangerous content", context);

        assertThat(result.getSuggestion()).isEqualTo(ContentSafetyChecker.HandlingSuggestion.REJECT);
    }

    @Test
    @DisplayName("检查输出内容")
    void checkOutput() {
        checker.addSensitiveWord("forbidden", "custom", ContentSafetyChecker.Severity.CRITICAL);
        ContentSafetyChecker.SafetyCheckContext context = createContext();

        ContentSafetyChecker.SafetyCheckResult result = checker.checkOutput("This is forbidden content", context);

        assertThat(result.isSafe()).isFalse();
        assertThat(result.getRiskLevel()).isEqualTo(ContentSafetyChecker.RiskLevel.CRITICAL);
        assertThat(result.getSuggestion()).isEqualTo(ContentSafetyChecker.HandlingSuggestion.REJECT);
    }

    private ContentSafetyChecker.SafetyCheckContext createContext() {
        return new ContentSafetyChecker.SafetyCheckContext() {
            @Override
            public String getUserId() { return "user-001"; }
            @Override
            public String getTenantId() { return "tenant-001"; }
            @Override
            public String getModelName() { return "gpt-4"; }
            @Override
            public String getOperationType() { return "chat"; }
            @Override
            public boolean isStrictMode() { return false; }
            @Override
            public List<String> getCheckCategories() { return List.of(); }
        };
    }
}