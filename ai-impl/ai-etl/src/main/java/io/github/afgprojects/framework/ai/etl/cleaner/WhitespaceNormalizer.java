package io.github.afgprojects.framework.ai.etl.cleaner;

import org.jspecify.annotations.NonNull;

import java.util.regex.Pattern;

/**
 * 空白字符规范化器。
 *
 * <p>规范化文本中的空白字符，包括：
 * <ul>
 *   <li>将多个连续空格替换为单个空格</li>
 *   <li>将多个连续换行符替换为最多两个</li>
 *   <li>移除行首行尾的空白字符</li>
 * </ul>
 *
 * @author AFG Projects
 * @since 1.0.0
 */
public class WhitespaceNormalizer implements ContentCleaner {

    // 多个空格
    private static final Pattern MULTIPLE_SPACES = Pattern.compile(" +");
    // 多个换行符
    private static final Pattern MULTIPLE_NEWLINES = Pattern.compile("\n{3,}");

    @Override
    public @NonNull String clean(@NonNull String content) {
        if (content.isEmpty()) {
            return content;
        }

        // 先处理换行符，保留段落结构
        String result = MULTIPLE_NEWLINES.matcher(content).replaceAll("\n\n");

        // 处理行首行尾空白
        String[] lines = result.split("\n");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            sb.append(line);
            if (i < lines.length - 1) {
                sb.append("\n");
            }
        }
        result = sb.toString();

        // 将多个空格替换为单个空格（保留换行）
        result = MULTIPLE_SPACES.matcher(result).replaceAll(" ");

        return result.trim();
    }

    @Override
    public @NonNull String getName() {
        return "WhitespaceNormalizer";
    }
}
