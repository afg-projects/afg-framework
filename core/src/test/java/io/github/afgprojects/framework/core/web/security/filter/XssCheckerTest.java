package io.github.afgprojects.framework.core.web.security.filter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("XssChecker")
class XssCheckerTest {

    @Nested
    @DisplayName("getName")
    class GetName {

        @Test
        @DisplayName("should return XSS")
        void shouldReturnXss() {
            XssChecker checker = new XssChecker();
            assertThat(checker.getName()).isEqualTo("XSS");
        }
    }

    @Nested
    @DisplayName("containsThreat without enhanced sanitizer")
    class ContainsThreatWithoutEnhanced {

        private final XssChecker checker = new XssChecker();

        @Test
        @DisplayName("should detect script tag via InputSanitizer")
        void shouldDetectScriptTag_viaInputSanitizer() {
            assertThat(checker.containsThreat("<script>alert('xss')</script>")).isTrue();
        }

        @Test
        @DisplayName("should detect javascript: protocol via InputSanitizer")
        void shouldDetectJavascriptProtocol_viaInputSanitizer() {
            assertThat(checker.containsThreat("javascript:alert('xss')")).isTrue();
        }

        @Test
        @DisplayName("should detect onclick handler via InputSanitizer")
        void shouldDetectOnclickHandler_viaInputSanitizer() {
            assertThat(checker.containsThreat("<div onclick=\"alert('xss')\">")).isTrue();
        }

        @Test
        @DisplayName("should return false for safe text via InputSanitizer")
        void shouldReturnFalse_forSafeText_viaInputSanitizer() {
            assertThat(checker.containsThreat("Hello World")).isFalse();
        }
    }
}