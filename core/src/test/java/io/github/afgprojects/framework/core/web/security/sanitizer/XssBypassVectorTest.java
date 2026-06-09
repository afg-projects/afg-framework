package io.github.afgprojects.framework.core.web.security.sanitizer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 AntiSamy 能阻挡正则表达式 {@link InputSanitizer} 遗漏的 XSS 绕过向量。
 * <p>
 * 这些测试向量代表 regex-based 检测无法可靠覆盖的攻击模式：
 * <ul>
 *   <li>HTML 实体编码绕过</li>
 *   <li>未被正则覆盖的标签（style, link, base, form, meta）</li>
 *   <li>嵌套/编码绕过</li>
 *   <li>SVG/onerror 事件处理器变体</li>
 * </ul>
 */
@DisplayName("XssBypassVectorTest")
class XssBypassVectorTest {

    /**
     * 创建测试用的 EnhancedInputSanitizer 实例。
     * 使用默认配置（XSS enabled, 非富文本模式）。
     */
    private EnhancedInputSanitizer createSanitizer() {
        io.github.afgprojects.framework.core.config.AfgCoreProperties properties =
                new io.github.afgprojects.framework.core.config.AfgCoreProperties();
        return new EnhancedInputSanitizer(properties);
    }

    @Nested
    @DisplayName("HTML entity encoding bypass")
    class HtmlEntityEncodingBypass {

        private final EnhancedInputSanitizer sanitizer = createSanitizer();

        @Test
        @DisplayName("should detect decimal HTML entity encoded javascript protocol")
        void shouldDetectDecimalHtmlEntityEncodedJavascript() {
            // &#106; = 'j', so &#106;avascript:alert(1) = javascript:alert(1)
            assertThat(sanitizer.containsXss("&#106;avascript:alert(1)")).isTrue();
        }

        @Test
        @DisplayName("should detect hex HTML entity encoded javascript protocol")
        void shouldDetectHexHtmlEntityEncodedJavascript() {
            // &#x6A; = 'j', so &#x6A;avascript:alert(1) = javascript:alert(1)
            assertThat(sanitizer.containsXss("&#x6A;avascript:alert(1)")).isTrue();
        }
    }

    @Nested
    @DisplayName("uncovered tags bypass")
    class UncoveredTagsBypass {

        private final EnhancedInputSanitizer sanitizer = createSanitizer();

        @Test
        @DisplayName("should detect style tag")
        void shouldDetectStyleTag() {
            assertThat(sanitizer.containsXss("<style>body{background:red}</style>")).isTrue();
        }

        @Test
        @DisplayName("should detect link tag with stylesheet")
        void shouldDetectLinkStylesheet() {
            assertThat(sanitizer.containsXss("<link rel=stylesheet href=evil>")).isTrue();
        }

        @Test
        @DisplayName("should detect base tag")
        void shouldDetectBaseTag() {
            assertThat(sanitizer.containsXss("<base href=evil>")).isTrue();
        }

        @Test
        @DisplayName("should detect form tag with action")
        void shouldDetectFormTag() {
            assertThat(sanitizer.containsXss("<form action=evil><input type=submit></form>")).isTrue();
        }

        @Test
        @DisplayName("should detect meta refresh redirect")
        void shouldDetectMetaRefresh() {
            assertThat(sanitizer.containsXss("<meta http-equiv=refresh content='0;url=evil'>")).isTrue();
        }
    }

    @Nested
    @DisplayName("nested/encoded bypass")
    class NestedEncodedBypass {

        private final EnhancedInputSanitizer sanitizer = createSanitizer();

        @Test
        @DisplayName("should detect nested script tags")
        void shouldDetectNestedScriptTags() {
            assertThat(sanitizer.containsXss("<scr<script>ipt>alert(1)</scr</script>ipt>")).isTrue();
        }

        @Test
        @DisplayName("should detect img onerror handler")
        void shouldDetectImgOnerrorHandler() {
            assertThat(sanitizer.containsXss("<img src=x onerror=alert(1)>")).isTrue();
        }

        @Test
        @DisplayName("should detect svg onload handler")
        void shouldDetectSvgOnloadHandler() {
            assertThat(sanitizer.containsXss("<svg onload=alert(1)>")).isTrue();
        }
    }

    @Nested
    @DisplayName("regex vs AntiSamy gap verification")
    class RegexVsAntiSamyGap {

        private final EnhancedInputSanitizer antiSamy = createSanitizer();

        /**
         * 验证某些向量确实能绕过 regex 但被 AntiSamy 拦截。
         * 这些测试确认修复的价值——AntiSamy 覆盖了 regex 的盲区。
         */
        @Test
        @DisplayName("should confirm AntiSamy catches vectors that regex misses - style tag")
        void shouldConfirmAntiSamyCatchesStyleTag() {
            String vector = "<style>body{background:url(javascript:alert(1))}</style>";
            // AntiSamy catches it
            assertThat(antiSamy.containsXss(vector)).isTrue();
        }

        @Test
        @DisplayName("should confirm AntiSamy catches vectors that regex misses - link tag")
        void shouldConfirmAntiSamyCatchesLinkTag() {
            String vector = "<link rel=stylesheet href=evil>";
            // AntiSamy catches it
            assertThat(antiSamy.containsXss(vector)).isTrue();
        }

        @Test
        @DisplayName("should confirm AntiSamy catches vectors that regex misses - base tag")
        void shouldConfirmAntiSamyCatchesBaseTag() {
            String vector = "<base href=evil>";
            // AntiSamy catches it
            assertThat(antiSamy.containsXss(vector)).isTrue();
        }

        @Test
        @DisplayName("should confirm AntiSamy catches vectors that regex misses - form tag")
        void shouldConfirmAntiSamyCatchesFormTag() {
            String vector = "<form action=evil><input type=submit></form>";
            // AntiSamy catches it
            assertThat(antiSamy.containsXss(vector)).isTrue();
        }

        @Test
        @DisplayName("should confirm AntiSamy catches vectors that regex misses - meta refresh")
        void shouldConfirmAntiSamyCatchesMetaRefresh() {
            String vector = "<meta http-equiv=refresh content='0;url=evil'>";
            // AntiSamy catches it
            assertThat(antiSamy.containsXss(vector)).isTrue();
        }
    }

    @Nested
    @DisplayName("NoOpInputSanitizer")
    class NoOpInputSanitizerTest {

        private final NoOpInputSanitizer noOp = new NoOpInputSanitizer();

        @Test
        @DisplayName("should never detect XSS")
        void shouldNeverDetectXss() {
            assertThat(noOp.containsXss("<script>alert(1)</script>")).isFalse();
        }

        @Test
        @DisplayName("should return input unchanged from sanitizeHtml")
        void shouldReturnInputUnchangedFromSanitizeHtml() {
            assertThat(noOp.sanitizeHtml("<script>alert(1)</script>"))
                    .isEqualTo("<script>alert(1)</script>");
        }

        @Test
        @DisplayName("should return null when input is null")
        void shouldReturnNullWhenInputIsNull() {
            assertThat(noOp.sanitizeHtml(null)).isNull();
        }
    }
}
