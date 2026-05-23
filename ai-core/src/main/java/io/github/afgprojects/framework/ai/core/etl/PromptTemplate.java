package io.github.afgprojects.framework.ai.core.etl;

import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Prompt 模板。
 *
 * <p>支持内置模板和自定义模板，用于 LLM 驱动的 Transformer。
 *
 * @author AFG Projects
 * @since 1.0.0
 */
public class PromptTemplate {

    private final String template;
    private final PromptType type;
    private final Map<String, String> defaultVariables;

    private PromptTemplate(@NonNull String template, @NonNull PromptType type, @NonNull Map<String, String> defaultVariables) {
        this.template = template;
        this.type = type;
        this.defaultVariables = defaultVariables;
    }

    // ===== 内置模板 =====

    /**
     * 创建摘要模板。
     */
    @NonNull
    public static PromptTemplate summarize() {
        return new PromptTemplate(
            "请用简洁的语言总结以下内容：\n\n{content}",
            PromptType.SUMMARIZE,
            Map.of()
        );
    }

    /**
     * 创建自定义摘要模板。
     */
    @NonNull
    public static PromptTemplate summarize(@NonNull String customTemplate) {
        return new PromptTemplate(customTemplate, PromptType.SUMMARIZE, Map.of());
    }

    /**
     * 创建翻译模板。
     */
    @NonNull
    public static PromptTemplate translate(@NonNull String targetLanguage) {
        return new PromptTemplate(
            "请将以下内容翻译为{language}：\n\n{content}",
            PromptType.TRANSLATE,
            Map.of("language", targetLanguage)
        );
    }

    /**
     * 创建关键词提取模板。
     */
    @NonNull
    public static PromptTemplate extractKeywords(int count) {
        return new PromptTemplate(
            "请从以下内容中提取{count}个关键词，用逗号分隔：\n\n{content}",
            PromptType.EXTRACT_KEYWORDS,
            Map.of("count", String.valueOf(count))
        );
    }

    /**
     * 创建语言检测模板。
     */
    @NonNull
    public static PromptTemplate detectLanguage() {
        return new PromptTemplate(
            "请识别以下内容的语言，只返回语言名称（如：中文、英文、日文）：\n\n{content}",
            PromptType.DETECT_LANGUAGE,
            Map.of()
        );
    }

    /**
     * 创建自定义模板。
     */
    @NonNull
    public static PromptTemplate custom(@NonNull String template) {
        return new PromptTemplate(template, PromptType.CUSTOM, Map.of());
    }

    // ===== 渲染方法 =====

    /**
     * 渲染模板。
     *
     * @param variables 变量映射
     * @return 渲染后的字符串
     */
    @NonNull
    public String render(@NonNull Map<String, Object> variables) {
        String result = template;
        Map<String, Object> allVariables = new HashMap<>(defaultVariables);
        allVariables.putAll(variables);

        for (Map.Entry<String, Object> entry : allVariables.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }

        return result;
    }

    /**
     * 获取原始模板字符串。
     */
    @NonNull
    public String getTemplate() {
        return template;
    }

    /**
     * 获取 Prompt 类型。
     */
    @NonNull
    public PromptType getType() {
        return type;
    }
}