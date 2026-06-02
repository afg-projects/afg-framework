package io.github.afgprojects.framework.ai.core.etl.cleaner;

import org.jspecify.annotations.NonNull;

import java.util.regex.Pattern;

/**
 * HTML 标签清洗器。
 *
 * <p>移除文本中的 HTML 标签。
 *
 * @author AFG Projects
 * @since 1.0.0
 */
public class HtmlTagCleaner implements ContentCleaner {

    // HTML 标签正则表达式
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");
    // HTML 实体正则表达式
    private static final Pattern HTML_ENTITY_PATTERN = Pattern.compile("&[a-zA-Z]+;|&#\\d+;");

    @Override
    public @NonNull String clean(@NonNull String content) {
        // 移除 HTML 标签
        String result = HTML_TAG_PATTERN.matcher(content).replaceAll("");

        // 替换常见的 HTML 实体
        result = result.replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'");

        // 移除其他 HTML 实体
        result = HTML_ENTITY_PATTERN.matcher(result).replaceAll("");

        return result;
    }

    @Override
    public @NonNull String getName() {
        return "HtmlTagCleaner";
    }
}
