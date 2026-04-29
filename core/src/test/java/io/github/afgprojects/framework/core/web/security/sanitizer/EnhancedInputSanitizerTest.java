package io.github.afgprojects.framework.core.web.security.sanitizer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.web.security.autoconfigure.AfgSecurityProperties;

/**
 * EnhancedInputSanitizer 测试
 * <p>
 * 使用 OWASP AntiSamy 进行 XSS 防护测试
 */
@DisplayName("EnhancedInputSanitizer 测试")
class EnhancedInputSanitizerTest {

    private EnhancedInputSanitizer sanitizer;
    private AfgSecurityProperties properties;

    @BeforeEach
    void setUp() {
        properties = createDefaultProperties();
        sanitizer = new EnhancedInputSanitizer(properties);
    }

    private AfgSecurityProperties createDefaultProperties() {
        AfgSecurityProperties props = new AfgSecurityProperties();
        props.getXss().setEnabled(true);
        props.getXss().setRichTextMode(false);
        props.getXss().setAllowedTags(Set.of("p", "b", "i", "a"));
        props.getXss().setAllowedAttributes(Set.of("href", "class"));
        props.getSqlInjection().setEnabled(true);
        return props;
    }

    @Nested
    @DisplayName("containsXss 测试")
    class ContainsXssTests {

        @Test
        @DisplayName("应该检测 script 标签")
        void shouldDetectScriptTag() {
            assertThat(sanitizer.containsXss("<script>alert('xss')</script>")).isTrue();
            assertThat(sanitizer.containsXss("<SCRIPT>alert('xss')</SCRIPT>")).isTrue();
        }

        @Test
        @DisplayName("应该检测事件处理器")
        void shouldDetectEventHandlers() {
            assertThat(sanitizer.containsXss("<img onerror='alert(1)'>")).isTrue();
            assertThat(sanitizer.containsXss("<body onload='alert(1)'>")).isTrue();
        }

        @Test
        @DisplayName("应该检测 iframe 标签")
        void shouldDetectIframe() {
            assertThat(sanitizer.containsXss("<iframe src='evil.com'></iframe>"))
                    .isTrue();
        }

        @Test
        @DisplayName("应该检测 object 标签")
        void shouldDetectObject() {
            assertThat(sanitizer.containsXss("<object data='evil.swf'></object>"))
                    .isTrue();
        }

        @Test
        @DisplayName("应该检测 SVG onload")
        void shouldDetectSvgOnload() {
            assertThat(sanitizer.containsXss("<svg onload='alert(1)'>")).isTrue();
        }

        @Test
        @DisplayName("安全输入应该返回 false")
        void shouldReturnFalseForSafeInput() {
            assertThat(sanitizer.containsXss("Hello World")).isFalse();
            // 注意：AntiSamy slashdot 策略会清洗某些标签，所以即使看起来安全的 HTML 也可能被检测为 XSS
        }

        @Test
        @DisplayName("null 和空字符串应该返回 false")
        void shouldReturnFalseForNullOrEmpty() {
            assertThat(sanitizer.containsXss(null)).isFalse();
            assertThat(sanitizer.containsXss("")).isFalse();
            assertThat(sanitizer.containsXss("   ")).isFalse();
        }
    }

    @Nested
    @DisplayName("containsSqlInjection 测试")
    class ContainsSqlInjectionTests {

        @Test
        @DisplayName("应该检测 OR 注入")
        void shouldDetectOrInjection() {
            assertThat(sanitizer.containsSqlInjection("' OR '1'='1")).isTrue();
            assertThat(sanitizer.containsSqlInjection("\" OR \"1\"=\"1")).isTrue();
        }

        @Test
        @DisplayName("应该检测 UNION SELECT")
        void shouldDetectUnionSelect() {
            assertThat(sanitizer.containsSqlInjection("UNION SELECT * FROM users"))
                    .isTrue();
            assertThat(sanitizer.containsSqlInjection("union all select * from users"))
                    .isTrue();
        }

        @Test
        @DisplayName("应该检测 DROP TABLE")
        void shouldDetectDropTable() {
            assertThat(sanitizer.containsSqlInjection("DROP TABLE users")).isTrue();
        }

        @Test
        @DisplayName("应该检测 DELETE FROM")
        void shouldDetectDeleteFrom() {
            assertThat(sanitizer.containsSqlInjection("DELETE FROM users")).isTrue();
        }

        @Test
        @DisplayName("应该检测 INSERT INTO")
        void shouldDetectInsertInto() {
            assertThat(sanitizer.containsSqlInjection("INSERT INTO users VALUES (1)"))
                    .isTrue();
        }

        @Test
        @DisplayName("应该检测 UPDATE SET")
        void shouldDetectUpdateSet() {
            assertThat(sanitizer.containsSqlInjection("UPDATE users SET password='x'"))
                    .isTrue();
        }

        @Test
        @DisplayName("应该检测注释注入")
        void shouldDetectCommentInjection() {
            assertThat(sanitizer.containsSqlInjection("admin'--")).isTrue();
            assertThat(sanitizer.containsSqlInjection("/* comment */")).isTrue();
        }

        @Test
        @DisplayName("应该检测 xp_cmdshell")
        void shouldDetectXpCmdshell() {
            assertThat(sanitizer.containsSqlInjection("xp_cmdshell 'dir'")).isTrue();
        }

        @Test
        @DisplayName("安全输入应该返回 false")
        void shouldReturnFalseForSafeInput() {
            assertThat(sanitizer.containsSqlInjection("SELECT name FROM products WHERE id = ?"))
                    .isFalse();
            assertThat(sanitizer.containsSqlInjection("Normal text input")).isFalse();
        }

        @Test
        @DisplayName("null 和空字符串应该返回 false")
        void shouldReturnFalseForNullOrEmpty() {
            assertThat(sanitizer.containsSqlInjection(null)).isFalse();
            assertThat(sanitizer.containsSqlInjection("")).isFalse();
        }
    }

    @Nested
    @DisplayName("sanitizeHtml 测试")
    class SanitizeHtmlTests {

        @Test
        @DisplayName("应该移除 script 标签")
        void shouldRemoveScriptTags() {
            // given
            String input = "<p>Hello</p><script>alert('xss')</script>";

            // when
            String result = sanitizer.sanitizeHtml(input);

            // then
            assertThat(result).doesNotContain("<script>");
        }

        @Test
        @DisplayName("null 输入应该返回 null")
        void shouldReturnNullForNullInput() {
            assertThat(sanitizer.sanitizeHtml(null)).isNull();
        }

        @Test
        @DisplayName("空字符串应该返回原值")
        void shouldReturnEmptyString() {
            assertThat(sanitizer.sanitizeHtml("")).isEqualTo("");
        }

        @Test
        @DisplayName("空白字符串应该返回原值")
        void shouldReturnBlankString() {
            assertThat(sanitizer.sanitizeHtml("   ")).isEqualTo("   ");
        }

        @Test
        @DisplayName("应该清洗危险内容")
        void shouldSanitizeDangerousContent() {
            // given
            String input = "<script>alert('xss')</script>Hello";

            // when
            String result = sanitizer.sanitizeHtml(input);

            // then
            assertThat(result).doesNotContain("<script>");
            assertThat(result).contains("Hello");
        }
    }

    @Nested
    @DisplayName("富文本模式测试")
    class RichTextModeTests {

        @BeforeEach
        void enableRichTextMode() {
            properties.getXss().setRichTextMode(true);
            properties.getXss().setAllowedTags(Set.of("p", "b", "i", "a", "img"));
            properties.getXss().setAllowedAttributes(Set.of("href", "src", "alt"));
            sanitizer = new EnhancedInputSanitizer(properties);
        }

        @Test
        @DisplayName("应该保留允许的标签")
        void shouldPreserveAllowedTags() {
            // given
            String input = "<p>This is <b>bold</b> and <i>italic</i></p>";

            // when
            String result = sanitizer.sanitizeHtml(input);

            // then - tinymce 策略允许这些标签
            assertThat(result).contains("<p>");
            assertThat(result).contains("<b>");
            assertThat(result).contains("<i>");
        }

        @Test
        @DisplayName("应该移除危险标签")
        void shouldRemoveDangerousTags() {
            // given
            String input = "<script>alert(1)</script><p>Safe</p>";

            // when
            String result = sanitizer.sanitizeHtml(input);

            // then
            assertThat(result).doesNotContain("<script>");
        }
    }

    @Nested
    @DisplayName("sanitizeSql 测试")
    class SanitizeSqlTests {

        @Test
        @DisplayName("应该转义单引号")
        void shouldEscapeSingleQuotes() {
            // given
            String input = "O'Brien's test";

            // when
            String result = sanitizer.sanitizeSql(input);

            // then
            assertThat(result).isEqualTo("O''Brien''s test");
        }

        @Test
        @DisplayName("应该移除分号")
        void shouldRemoveSemicolons() {
            // given
            String input = "test; value";

            // when
            String result = sanitizer.sanitizeSql(input);

            // then
            assertThat(result).isEqualTo("test value");
        }

        @Test
        @DisplayName("应该移除注释")
        void shouldRemoveComments() {
            // given
            String input = "test--comment";

            // when
            String result = sanitizer.sanitizeSql(input);

            // then
            assertThat(result).doesNotContain("--");
        }

        @Test
        @DisplayName("null 输入应该返回 null")
        void shouldReturnNullForNullInput() {
            assertThat(sanitizer.sanitizeSql(null)).isNull();
        }

        @Test
        @DisplayName("空字符串应该返回原值")
        void shouldReturnEmptyString() {
            assertThat(sanitizer.sanitizeSql("")).isEqualTo("");
        }
    }

    @Nested
    @DisplayName("checkSecurity 测试")
    class CheckSecurityTests {

        @Test
        @DisplayName("XSS 攻击应该返回 XSS")
        void shouldReturnXssForXssAttack() {
            assertThat(sanitizer.checkSecurity("<script>alert(1)</script>")).isEqualTo("XSS");
        }

        @Test
        @DisplayName("SQL 注入应该返回 SQL_INJECTION")
        void shouldReturnSqlInjectionForSqlInjection() {
            assertThat(sanitizer.checkSecurity("' OR '1'='1")).isEqualTo("SQL_INJECTION");
        }

        @Test
        @DisplayName("安全输入应该返回 null")
        void shouldReturnNullForSafeInput() {
            assertThat(sanitizer.checkSecurity("Normal text")).isNull();
        }

        @Test
        @DisplayName("null 输入应该返回 null")
        void shouldReturnNullForNullInput() {
            assertThat(sanitizer.checkSecurity(null)).isNull();
        }

        @Test
        @DisplayName("XSS 禁用时不应该检测 XSS")
        void shouldNotDetectXssWhenDisabled() {
            // given
            properties.getXss().setEnabled(false);
            sanitizer = new EnhancedInputSanitizer(properties);

            // then
            assertThat(sanitizer.checkSecurity("<script>alert(1)</script>")).isNull();
        }

        @Test
        @DisplayName("SQL 注入禁用时不应该检测 SQL 注入")
        void shouldNotDetectSqlInjectionWhenDisabled() {
            // given
            properties.getSqlInjection().setEnabled(false);
            sanitizer = new EnhancedInputSanitizer(properties);

            // then
            assertThat(sanitizer.checkSecurity("' OR '1'='1")).isNull();
        }
    }

    @Nested
    @DisplayName("sanitize 测试")
    class SanitizeTests {

        @Test
        @DisplayName("应该清洗 XSS 攻击")
        void shouldSanitizeXss() {
            // given
            String input = "<p>Hello</p><script>alert(1)</script>";

            // when
            String result = sanitizer.sanitize(input);

            // then
            assertThat(result).doesNotContain("<script>");
        }

        @Test
        @DisplayName("null 输入应该返回 null")
        void shouldReturnNullForNullInput() {
            assertThat(sanitizer.sanitize(null)).isNull();
        }

        @Test
        @DisplayName("空字符串应该返回原值")
        void shouldReturnEmptyString() {
            assertThat(sanitizer.sanitize("")).isEqualTo("");
        }

        @Test
        @DisplayName("XSS 禁用时不应该清洗")
        void shouldNotSanitizeWhenDisabled() {
            // given
            properties.getXss().setEnabled(false);
            sanitizer = new EnhancedInputSanitizer(properties);
            String input = "<script>alert(1)</script>";

            // when
            String result = sanitizer.sanitize(input);

            // then
            assertThat(result).isEqualTo(input);
        }
    }

    @Nested
    @DisplayName("AntiSamy 特性测试")
    class AntiSamyFeatureTests {

        @Test
        @DisplayName("AntiSamy 应该正确处理嵌套攻击")
        void shouldHandleNestedAttacks() {
            // given - 尝试通过嵌套绕过
            String input = "<scr<script>ipt>alert(1)</scr</script>ipt>";

            // when
            String result = sanitizer.sanitizeHtml(input);

            // then - AntiSamy 会正确处理
            assertThat(result).doesNotContain("<script>");
        }

        @Test
        @DisplayName("AntiSamy 应该处理编码攻击")
        void shouldHandleEncodingAttacks() {
            // given - HTML 实体编码攻击
            String input = "&#60;script&#62;alert(1)&#60;/script&#62;";

            // when
            String result = sanitizer.sanitizeHtml(input);

            // then - AntiSamy 会解码后清洗
            assertThat(result).doesNotContain("<script>");
        }

        @Test
        @DisplayName("富文本模式应该允许安全链接")
        void shouldAllowSafeLinksInRichTextMode() {
            // given
            properties.getXss().setRichTextMode(true);
            sanitizer = new EnhancedInputSanitizer(properties);
            String input = "<a href=\"https://example.com\">Safe Link</a>";

            // when
            String result = sanitizer.sanitizeHtml(input);

            // then - tinymce 策略允许安全链接
            assertThat(result).contains("href");
            assertThat(result).contains("Safe Link");
        }
    }
}
