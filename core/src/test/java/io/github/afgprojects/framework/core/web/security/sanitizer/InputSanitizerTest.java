package io.github.afgprojects.framework.core.web.security.sanitizer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class InputSanitizerTest {

    // --- containsXss ---

    @Nested
    @DisplayName("containsXss tests")
    class ContainsXssTests {

        @Test
        @DisplayName("Should detect XSS in script tag")
        void should_detectXss_when_scriptTag() {
            assertThat(InputSanitizer.containsXss("<script>alert('xss')</script>"))
                    .isTrue();
        }

        @Test
        @DisplayName("Should detect XSS in event handler")
        void should_detectXss_when_eventHandler() {
            assertThat(InputSanitizer.containsXss("onmouseover=alert(1)")).isTrue();
        }

        @Test
        @DisplayName("Should detect XSS in javascript protocol")
        void should_detectXss_when_javascriptProtocol() {
            assertThat(InputSanitizer.containsXss("javascript:alert(1)")).isTrue();
        }

        @Test
        @DisplayName("Should not detect XSS in normal text")
        void should_notDetectXss_when_normalText() {
            assertThat(InputSanitizer.containsXss("Hello World")).isFalse();
        }

        @Test
        @DisplayName("Should not detect XSS when null")
        void should_notDetectXss_when_null() {
            assertThat(InputSanitizer.containsXss(null)).isFalse();
        }

        @Test
        @DisplayName("Should not detect XSS when empty string")
        void should_notDetectXss_when_emptyString() {
            assertThat(InputSanitizer.containsXss("")).isFalse();
        }

        @Test
        @DisplayName("Should not detect XSS when whitespace only")
        void should_notDetectXss_when_whitespaceOnly() {
            assertThat(InputSanitizer.containsXss("   ")).isFalse();
        }

        @Test
        @DisplayName("Should detect XSS in iframe tag")
        void should_detectXss_when_iframeTag() {
            assertThat(InputSanitizer.containsXss("<iframe src='evil.com'></iframe>"))
                    .isTrue();
        }

        @Test
        @DisplayName("Should detect XSS in object tag")
        void should_detectXss_when_objectTag() {
            assertThat(InputSanitizer.containsXss("<object data='evil.swf'></object>"))
                    .isTrue();
        }

        @Test
        @DisplayName("Should detect XSS in embed tag")
        void should_detectXss_when_embedTag() {
            assertThat(InputSanitizer.containsXss("<embed src='evil.swf'>")).isTrue();
        }

        @Test
        @DisplayName("Should detect XSS in vbscript protocol")
        void should_detectXss_when_vbscriptProtocol() {
            assertThat(InputSanitizer.containsXss("vbscript:msgbox(1)")).isTrue();
        }

        @Test
        @DisplayName("Should detect XSS in CSS expression")
        void should_detectXss_when_cssExpression() {
            assertThat(InputSanitizer.containsXss("expression(alert(1))")).isTrue();
        }

        @Test
        @DisplayName("Should handle very long input efficiently")
        void should_handleVeryLongInput() {
            String longInput = "a".repeat(10000) + "<script>alert(1)</script>";
            assertThat(InputSanitizer.containsXss(longInput)).isTrue();
        }
    }

    // --- sanitizeHtml ---

    @Nested
    @DisplayName("sanitizeHtml tests")
    class SanitizeHtmlTests {

        @Test
        @DisplayName("Should remove script tag from HTML")
        void should_sanitizeHtml_when_scriptTag() {
            assertThat(InputSanitizer.sanitizeHtml("<script>alert('xss')</script>"))
                    .doesNotContain("<script>");
        }

        @Test
        @DisplayName("Should return null when input is null")
        void should_sanitizeHtml_when_null() {
            assertThat(InputSanitizer.sanitizeHtml(null)).isNull();
        }

        @Test
        @DisplayName("Should return empty string unchanged")
        void should_sanitizeHtml_when_emptyString() {
            assertThat(InputSanitizer.sanitizeHtml("")).isEmpty();
        }

        @Test
        @DisplayName("Should preserve normal text")
        void should_sanitizeHtml_when_normalText() {
            String normal = "Hello World, this is a test.";
            assertThat(InputSanitizer.sanitizeHtml(normal)).isEqualTo(normal);
        }

        @Test
        @DisplayName("Should handle multiple XSS patterns")
        void should_sanitizeHtml_when_multiplePatterns() {
            String input = "<script>xss</script>normal text<iframe>evil</iframe>";
            String result = InputSanitizer.sanitizeHtml(input);
            assertThat(result).doesNotContain("<script>", "<iframe>");
            assertThat(result).contains("normal text");
        }
    }

    // --- containsSqlInjection ---

    @Nested
    @DisplayName("containsSqlInjection tests")
    class ContainsSqlInjectionTests {

        @Test
        @DisplayName("Should detect SQL injection with OR condition")
        void should_detectSqlInjection_when_orCondition() {
            assertThat(InputSanitizer.containsSqlInjection("' OR 1=1 --")).isTrue();
        }

        @Test
        @DisplayName("Should detect SQL injection with UNION SELECT")
        void should_detectSqlInjection_when_unionSelect() {
            assertThat(InputSanitizer.containsSqlInjection("UNION SELECT * FROM users"))
                    .isTrue();
        }

        @Test
        @DisplayName("Should detect SQL injection with DROP TABLE")
        void should_detectSqlInjection_when_dropTable() {
            assertThat(InputSanitizer.containsSqlInjection("DROP TABLE users")).isTrue();
        }

        @Test
        @DisplayName("Should not detect SQL injection in normal text")
        void should_notDetectSqlInjection_when_normalText() {
            assertThat(InputSanitizer.containsSqlInjection("Hello World")).isFalse();
        }

        @Test
        @DisplayName("Should not detect SQL injection when null")
        void should_notDetectSqlInjection_when_null() {
            assertThat(InputSanitizer.containsSqlInjection(null)).isFalse();
        }

        @Test
        @DisplayName("Should not detect SQL injection when empty string")
        void should_notDetectSqlInjection_when_emptyString() {
            assertThat(InputSanitizer.containsSqlInjection("")).isFalse();
        }

        @Test
        @DisplayName("Should not detect SQL injection when whitespace only")
        void should_notDetectSqlInjection_when_whitespaceOnly() {
            assertThat(InputSanitizer.containsSqlInjection("   ")).isFalse();
        }

        @Test
        @DisplayName("Should detect SQL injection with DELETE FROM")
        void should_detectSqlInjection_when_deleteFrom() {
            assertThat(InputSanitizer.containsSqlInjection("DELETE FROM users")).isTrue();
        }

        @Test
        @DisplayName("Should detect SQL injection with INSERT INTO")
        void should_detectSqlInjection_when_insertInto() {
            assertThat(InputSanitizer.containsSqlInjection("INSERT INTO users VALUES (1)"))
                    .isTrue();
        }

        @Test
        @DisplayName("Should detect SQL injection with UPDATE SET")
        void should_detectSqlInjection_when_updateSet() {
            assertThat(InputSanitizer.containsSqlInjection("UPDATE users SET password='hacked'"))
                    .isTrue();
        }

        @Test
        @DisplayName("Should detect SQL injection with comment")
        void should_detectSqlInjection_when_sqlComment() {
            assertThat(InputSanitizer.containsSqlInjection("admin'--")).isTrue();
        }

        @Test
        @DisplayName("Should handle very long input efficiently")
        void should_handleVeryLongInput() {
            String longInput = "a".repeat(10000) + "' OR 1=1 --";
            assertThat(InputSanitizer.containsSqlInjection(longInput)).isTrue();
        }
    }

    // --- sanitizeSql ---

    @Nested
    @DisplayName("sanitizeSql tests")
    class SanitizeSqlTests {

        @Test
        @DisplayName("Should escape single quotes")
        void should_sanitizeSql_when_singleQuote() {
            String result = InputSanitizer.sanitizeSql("it's a test");
            assertThat(result).isEqualTo("it''s a test");
        }

        @Test
        @DisplayName("Should return null when input is null")
        void should_sanitizeSql_when_null() {
            assertThat(InputSanitizer.sanitizeSql(null)).isNull();
        }

        @Test
        @DisplayName("Should return empty string unchanged")
        void should_sanitizeSql_when_emptyString() {
            assertThat(InputSanitizer.sanitizeSql("")).isEmpty();
        }

        @Test
        @DisplayName("Should remove semicolons")
        void should_sanitizeSql_when_semicolon() {
            assertThat(InputSanitizer.sanitizeSql("SELECT * FROM users;")).isEqualTo("SELECT * FROM users");
        }

        @Test
        @DisplayName("Should remove SQL comments")
        void should_sanitizeSql_when_sqlComment() {
            assertThat(InputSanitizer.sanitizeSql("admin'--")).isEqualTo("admin''");
        }

        @Test
        @DisplayName("Should preserve normal text")
        void should_sanitizeSql_when_normalText() {
            String normal = "Hello World";
            assertThat(InputSanitizer.sanitizeSql(normal)).isEqualTo(normal);
        }

        @Test
        @DisplayName("Should handle multiple special characters")
        void should_sanitizeSql_when_multipleSpecialChars() {
            String result = InputSanitizer.sanitizeSql("it's; test--value");
            assertThat(result).doesNotContain(";", "--");
            assertThat(result).contains("''");
        }
    }
}
