package io.github.afgprojects.framework.core.web.security.sanitizer;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 输入清洗工具类
 */
public final class InputSanitizer {

    private static final List<Pattern> XSS_PATTERNS = List.of(
            Pattern.compile("<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
            Pattern.compile("javascript\\s*:", Pattern.CASE_INSENSITIVE),
            Pattern.compile("on\\w+\\s*=", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<iframe[^>]*>.*?</iframe>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
            Pattern.compile("<object[^>]*>.*?</object>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
            Pattern.compile("<embed[^>]*>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("expression\\s*\\(", Pattern.CASE_INSENSITIVE),
            Pattern.compile("vbscript\\s*:", Pattern.CASE_INSENSITIVE));

    private static final List<Pattern> SQL_INJECTION_PATTERNS = List.of(
            // OR/AND 注入
            Pattern.compile("'\\s*(?:OR|AND)\\s+['\"]?\\d+['\"]?", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\"\\s*(?:OR|AND)\\s+['\"]?\\d+['\"]?", Pattern.CASE_INSENSITIVE),
            // UNION SELECT 注入
            Pattern.compile("(?:UNION\\s+(?:ALL\\s+)?SELECT)", Pattern.CASE_INSENSITIVE),
            // DROP TABLE 注入
            Pattern.compile("(?:DROP\\s+TABLE)", Pattern.CASE_INSENSITIVE),
            // DELETE FROM 注入
            Pattern.compile("(?:DELETE\\s+FROM)", Pattern.CASE_INSENSITIVE),
            // INSERT INTO 注入
            Pattern.compile("(?:INSERT\\s+INTO)", Pattern.CASE_INSENSITIVE),
            // UPDATE SET 注入
            Pattern.compile("(?:UPDATE\\s+\\w+\\s+SET)", Pattern.CASE_INSENSITIVE),
            // ALTER/CREATE 注入
            Pattern.compile(";\\s*(?:DROP|DELETE|INSERT|UPDATE|ALTER|CREATE)", Pattern.CASE_INSENSITIVE),
            // 注释注入
            Pattern.compile("--\\s*$", Pattern.MULTILINE),
            Pattern.compile("/\\*.*\\*/", Pattern.DOTALL),
            // EXEC/EXECUTE 注入
            Pattern.compile("\\bEXEC(?:UTE)?\\s+", Pattern.CASE_INSENSITIVE),
            // xp_cmdshell 注入
            Pattern.compile("xp_cmdshell", Pattern.CASE_INSENSITIVE));

    private InputSanitizer() {}

    public static boolean containsXss(String input) {
        if (input == null || input.isBlank()) {
            return false;
        }
        return XSS_PATTERNS.stream().anyMatch(p -> p.matcher(input).find());
    }

    public static String sanitizeHtml(String input) {
        if (input == null) {
            return null;
        }
        String result = input;
        for (Pattern pattern : XSS_PATTERNS) {
            result = pattern.matcher(result).replaceAll("");
        }
        return result;
    }

    public static boolean containsSqlInjection(String input) {
        if (input == null || input.isBlank()) {
            return false;
        }
        return SQL_INJECTION_PATTERNS.stream().anyMatch(p -> p.matcher(input).find());
    }

    public static String sanitizeSql(String input) {
        if (input == null) {
            return null;
        }
        return input.replace("'", "''").replace(";", "").replace("--", "");
    }
}
