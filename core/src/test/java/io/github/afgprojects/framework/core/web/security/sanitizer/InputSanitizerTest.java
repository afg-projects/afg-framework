package io.github.afgprojects.framework.core.web.security.sanitizer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InputSanitizer")
class InputSanitizerTest {

    @Nested
    @DisplayName("containsXss")
    class ContainsXss {

        @Test
        @DisplayName("should return false when input is null")
        void shouldReturnFalse_whenInputIsNull() {
            assertThat(InputSanitizer.containsXss(null)).isFalse();
        }

        @Test
        @DisplayName("should return false when input is blank")
        void shouldReturnFalse_whenInputIsBlank() {
            assertThat(InputSanitizer.containsXss("")).isFalse();
            assertThat(InputSanitizer.containsXss("   ")).isFalse();
        }

        @Test
        @DisplayName("should return false for safe text")
        void shouldReturnFalse_forSafeText() {
            assertThat(InputSanitizer.containsXss("Hello World")).isFalse();
            assertThat(InputSanitizer.containsXss("用户名123")).isFalse();
        }

        @Nested
        @DisplayName("script tag detection")
        class ScriptTag {

            @Test
            @DisplayName("should detect script tag")
            void shouldDetectScriptTag() {
                assertThat(InputSanitizer.containsXss("<script>alert('xss')</script>")).isTrue();
            }

            @Test
            @DisplayName("should detect script tag with attributes")
            void shouldDetectScriptTagWithAttributes() {
                assertThat(InputSanitizer.containsXss("<script src=\"evil.js\"></script>")).isTrue();
            }

            @Test
            @DisplayName("should detect script tag case insensitive")
            void shouldDetectScriptTagCaseInsensitive() {
                assertThat(InputSanitizer.containsXss("<SCRIPT>alert('xss')</SCRIPT>")).isTrue();
                assertThat(InputSanitizer.containsXss("<Script>alert('xss')</Script>")).isTrue();
            }
        }

        @Nested
        @DisplayName("javascript protocol detection")
        class JavascriptProtocol {

            @Test
            @DisplayName("should detect javascript: protocol")
            void shouldDetectJavascriptProtocol() {
                assertThat(InputSanitizer.containsXss("javascript:alert('xss')")).isTrue();
            }

            @Test
            @DisplayName("should detect javascript: with spaces")
            void shouldDetectJavascriptProtocolWithSpaces() {
                assertThat(InputSanitizer.containsXss("javascript :alert('xss')")).isTrue();
            }

            @Test
            @DisplayName("should detect javascript: case insensitive")
            void shouldDetectJavascriptProtocolCaseInsensitive() {
                assertThat(InputSanitizer.containsXss("JAVASCRIPT:alert('xss')")).isTrue();
            }
        }

        @Nested
        @DisplayName("event handler detection")
        class EventHandler {

            @Test
            @DisplayName("should detect onclick handler")
            void shouldDetectOnclickHandler() {
                assertThat(InputSanitizer.containsXss("<div onclick=\"alert('xss')\">click</div>")).isTrue();
            }

            @Test
            @DisplayName("should detect onerror handler")
            void shouldDetectOnerrorHandler() {
                assertThat(InputSanitizer.containsXss("<img onerror=\"alert('xss')\">")).isTrue();
            }

            @Test
            @DisplayName("should detect onload handler")
            void shouldDetectOnloadHandler() {
                assertThat(InputSanitizer.containsXss("<body onload=\"alert('xss')\">")).isTrue();
            }

            @Test
            @DisplayName("should detect event handler case insensitive")
            void shouldDetectEventHandlerCaseInsensitive() {
                assertThat(InputSanitizer.containsXss("ONCLICK=\"alert('xss')\"")).isTrue();
            }
        }

        @Nested
        @DisplayName("iframe/object/embed detection")
        class IframeObjectEmbed {

            @Test
            @DisplayName("should detect iframe tag")
            void shouldDetectIframeTag() {
                assertThat(InputSanitizer.containsXss("<iframe src=\"evil.html\"></iframe>")).isTrue();
            }

            @Test
            @DisplayName("should detect object tag")
            void shouldDetectObjectTag() {
                assertThat(InputSanitizer.containsXss("<object data=\"evil.swf\"></object>")).isTrue();
            }

            @Test
            @DisplayName("should detect embed tag")
            void shouldDetectEmbedTag() {
                assertThat(InputSanitizer.containsXss("<embed src=\"evil.swf\">")).isTrue();
            }
        }

        @Nested
        @DisplayName("CSS expression detection")
        class CssExpression {

            @Test
            @DisplayName("should detect CSS expression")
            void shouldDetectCssExpression() {
                assertThat(InputSanitizer.containsXss("expression(alert('xss'))")).isTrue();
            }

            @Test
            @DisplayName("should detect vbscript protocol")
            void shouldDetectVbscriptProtocol() {
                assertThat(InputSanitizer.containsXss("vbscript:msgbox")).isTrue();
            }
        }

        @Nested
        @DisplayName("SVG/MATH tag detection")
        class SvgMathTag {

            @Test
            @DisplayName("should detect SVG tag")
            void shouldDetectSvgTag() {
                assertThat(InputSanitizer.containsXss("<svg onload=\"alert('xss')\"></svg>")).isTrue();
            }

            @Test
            @DisplayName("should detect self-closing SVG tag")
            void shouldDetectSelfClosingSvgTag() {
                assertThat(InputSanitizer.containsXss("<svg/>")).isTrue();
            }

            @Test
            @DisplayName("should detect MATH tag")
            void shouldDetectMathTag() {
                assertThat(InputSanitizer.containsXss("<math><mtext>test</mtext></math>")).isTrue();
            }
        }

        @Nested
        @DisplayName("data: protocol detection")
        class DataProtocol {

            @Test
            @DisplayName("should detect data:text/html protocol")
            void shouldDetectDataTextHtml() {
                assertThat(InputSanitizer.containsXss("data:text/html,<script>alert('xss')</script>")).isTrue();
            }

            @Test
            @DisplayName("should detect data:image/svg+xml protocol")
            void shouldDetectDataImageSvg() {
                assertThat(InputSanitizer.containsXss("data:image/svg+xml,<svg></svg>")).isTrue();
            }
        }
    }

    @Nested
    @DisplayName("sanitizeHtml")
    class SanitizeHtml {

        @Test
        @DisplayName("should return null when input is null")
        void shouldReturnNull_whenInputIsNull() {
            assertThat(InputSanitizer.sanitizeHtml(null)).isNull();
        }

        @Test
        @DisplayName("should return safe text unchanged")
        void shouldReturnSafeTextUnchanged() {
            assertThat(InputSanitizer.sanitizeHtml("Hello World")).isEqualTo("Hello World");
        }

        @Test
        @DisplayName("should remove script tags")
        void shouldRemoveScriptTags() {
            String result = InputSanitizer.sanitizeHtml("<script>alert('xss')</script>Hello");
            assertThat(result).isEqualTo("Hello");
        }

        @Test
        @DisplayName("should remove event handlers")
        void shouldRemoveEventHandlers() {
            String result = InputSanitizer.sanitizeHtml("<div onclick=\"alert('xss')\">click</div>");
            assertThat(result).doesNotContain("onclick");
        }

        @Test
        @DisplayName("should remove javascript protocol")
        void shouldRemoveJavascriptProtocol() {
            String result = InputSanitizer.sanitizeHtml("<a href=\"javascript:alert('xss')\">link</a>");
            assertThat(result).doesNotContain("javascript");
        }

        @Test
        @DisplayName("should remove iframe tags")
        void shouldRemoveIframeTags() {
            String result = InputSanitizer.sanitizeHtml("<iframe src=\"evil.html\"></iframe>content");
            assertThat(result).isEqualTo("content");
        }
    }

    @Nested
    @DisplayName("containsSqlInjection")
    class ContainsSqlInjection {

        @Test
        @DisplayName("should return false when input is null")
        void shouldReturnFalse_whenInputIsNull() {
            assertThat(InputSanitizer.containsSqlInjection(null)).isFalse();
        }

        @Test
        @DisplayName("should return false when input is blank")
        void shouldReturnFalse_whenInputIsBlank() {
            assertThat(InputSanitizer.containsSqlInjection("")).isFalse();
            assertThat(InputSanitizer.containsSqlInjection("   ")).isFalse();
        }

        @Test
        @DisplayName("should return false for safe text")
        void shouldReturnFalse_forSafeText() {
            assertThat(InputSanitizer.containsSqlInjection("Hello World")).isFalse();
            assertThat(InputSanitizer.containsSqlInjection("用户名123")).isFalse();
        }

        @Nested
        @DisplayName("OR/AND injection")
        class OrAndInjection {

            @Test
            @DisplayName("should detect OR injection with single quote")
            void shouldDetectOrInjectionSingleQuote() {
                assertThat(InputSanitizer.containsSqlInjection("' OR 1=1")).isTrue();
                assertThat(InputSanitizer.containsSqlInjection("' OR '1'='1")).isTrue();
            }

            @Test
            @DisplayName("should detect AND injection with single quote")
            void shouldDetectAndInjectionSingleQuote() {
                assertThat(InputSanitizer.containsSqlInjection("' AND 1=1")).isTrue();
            }

            @Test
            @DisplayName("should detect OR injection with double quote")
            void shouldDetectOrInjectionDoubleQuote() {
                assertThat(InputSanitizer.containsSqlInjection("\" OR 1=1")).isTrue();
            }
        }

        @Nested
        @DisplayName("UNION SELECT injection")
        class UnionSelect {

            @Test
            @DisplayName("should detect UNION SELECT")
            void shouldDetectUnionSelect() {
                assertThat(InputSanitizer.containsSqlInjection("UNION SELECT * FROM users")).isTrue();
            }

            @Test
            @DisplayName("should detect UNION ALL SELECT")
            void shouldDetectUnionAllSelect() {
                assertThat(InputSanitizer.containsSqlInjection("UNION ALL SELECT * FROM users")).isTrue();
            }

            @Test
            @DisplayName("should detect UNION SELECT case insensitive")
            void shouldDetectUnionSelectCaseInsensitive() {
                assertThat(InputSanitizer.containsSqlInjection("union select * from users")).isTrue();
            }
        }

        @Nested
        @DisplayName("DDL/DML injection")
        class DdlDmlInjection {

            @Test
            @DisplayName("should detect DROP TABLE")
            void shouldDetectDropTable() {
                assertThat(InputSanitizer.containsSqlInjection("DROP TABLE users")).isTrue();
            }

            @Test
            @DisplayName("should detect DELETE FROM")
            void shouldDetectDeleteFrom() {
                assertThat(InputSanitizer.containsSqlInjection("DELETE FROM users")).isTrue();
            }

            @Test
            @DisplayName("should detect INSERT INTO")
            void shouldDetectInsertInto() {
                assertThat(InputSanitizer.containsSqlInjection("INSERT INTO users VALUES (1)")).isTrue();
            }

            @Test
            @DisplayName("should detect UPDATE SET")
            void shouldDetectUpdateSet() {
                assertThat(InputSanitizer.containsSqlInjection("UPDATE users SET password='hack'")).isTrue();
            }

            @Test
            @DisplayName("should detect semicolon-prefixed DDL/DML")
            void shouldDetectSemicolonPrefixedDdl() {
                assertThat(InputSanitizer.containsSqlInjection("; DROP TABLE users")).isTrue();
                assertThat(InputSanitizer.containsSqlInjection("; DELETE FROM users")).isTrue();
            }
        }

        @Nested
        @DisplayName("comment injection")
        class CommentInjection {

            @Test
            @DisplayName("should detect SQL comment --")
            void shouldDetectSqlComment() {
                assertThat(InputSanitizer.containsSqlInjection("admin'--")).isTrue();
            }

            @Test
            @DisplayName("should detect block comment")
            void shouldDetectBlockComment() {
                assertThat(InputSanitizer.containsSqlInjection("/* comment */")).isTrue();
            }
        }

        @Nested
        @DisplayName("stored procedure injection")
        class StoredProcedure {

            @Test
            @DisplayName("should detect EXEC")
            void shouldDetectExec() {
                assertThat(InputSanitizer.containsSqlInjection("EXEC xp_cmdshell 'dir'")).isTrue();
            }

            @Test
            @DisplayName("should detect EXECUTE")
            void shouldDetectExecute() {
                assertThat(InputSanitizer.containsSqlInjection("EXECUTE xp_cmdshell 'dir'")).isTrue();
            }

            @Test
            @DisplayName("should detect xp_cmdshell")
            void shouldDetectXpCmdshell() {
                assertThat(InputSanitizer.containsSqlInjection("xp_cmdshell")).isTrue();
            }
        }

        @Nested
        @DisplayName("blind injection")
        class BlindInjection {

            @Test
            @DisplayName("should detect SLEEP injection")
            void shouldDetectSleepInjection() {
                assertThat(InputSanitizer.containsSqlInjection("SLEEP(5)")).isTrue();
            }

            @Test
            @DisplayName("should detect BENCHMARK injection")
            void shouldDetectBenchmarkInjection() {
                assertThat(InputSanitizer.containsSqlInjection("BENCHMARK(1000000, MD5('a'))")).isTrue();
            }

            @Test
            @DisplayName("should detect HAVING injection")
            void shouldDetectHavingInjection() {
                assertThat(InputSanitizer.containsSqlInjection("HAVING 1=1")).isTrue();
            }
        }
    }

    @Nested
    @DisplayName("sanitizeSql")
    class SanitizeSql {

        @Test
        @DisplayName("should return null when input is null")
        void shouldReturnNull_whenInputIsNull() {
            assertThat(InputSanitizer.sanitizeSql(null)).isNull();
        }

        @Test
        @DisplayName("should escape single quotes")
        void shouldEscapeSingleQuotes() {
            assertThat(InputSanitizer.sanitizeSql("it's a test")).isEqualTo("it''s a test");
        }

        @Test
        @DisplayName("should remove semicolons")
        void shouldRemoveSemicolons() {
            assertThat(InputSanitizer.sanitizeSql("a;b")).isEqualTo("ab");
        }

        @Test
        @DisplayName("should remove SQL comments")
        void shouldRemoveSqlComments() {
            assertThat(InputSanitizer.sanitizeSql("admin'--")).isEqualTo("admin''");
        }

        @Test
        @DisplayName("should handle all patterns together")
        void shouldHandleAllPatternsTogether() {
            // "it's; a--test" -> replace("'", "''") -> "it''s; a--test"
            // -> replace(";", "") -> "it''s a--test"
            // -> replace("--", "") -> "it''s atest"
            String result = InputSanitizer.sanitizeSql("it's; a--test");
            assertThat(result).isEqualTo("it''s atest");
        }
    }
}
