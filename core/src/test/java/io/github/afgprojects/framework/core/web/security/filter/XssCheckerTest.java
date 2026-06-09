package io.github.afgprojects.framework.core.web.security.filter;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.web.security.sanitizer.EnhancedInputSanitizer;
import io.github.afgprojects.framework.core.web.security.sanitizer.InputSecurityChecker;
import io.github.afgprojects.framework.core.web.security.sanitizer.NoOpInputSanitizer;

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

        @SuppressWarnings("deprecation")
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

    @Nested
    @DisplayName("containsThreat with InputSecurityChecker")
    class ContainsThreatWithInputSecurityChecker {

        @Test
        @DisplayName("should use InputSecurityChecker when provided")
        void shouldUseInputSecurityChecker_whenProvided() {
            InputSecurityChecker securityChecker = new InputSecurityChecker() {
                @Override
                public boolean containsXss(@Nullable String input) {
                    return input != null && input.contains("<script>");
                }

                @Override
                public @Nullable String sanitizeHtml(@Nullable String input) {
                    return input;
                }
            };
            XssChecker checker = new XssChecker(securityChecker);

            assertThat(checker.containsThreat("<script>alert(1)</script>")).isTrue();
            assertThat(checker.containsThreat("Hello World")).isFalse();
        }

        @Test
        @DisplayName("should fall back to regex when InputSecurityChecker is null")
        void shouldFallBackToRegex_whenInputSecurityCheckerIsNull() {
            XssChecker checker = new XssChecker((InputSecurityChecker) null);

            assertThat(checker.containsThreat("<script>alert('xss')</script>")).isTrue();
            assertThat(checker.containsThreat("Hello World")).isFalse();
        }
    }

    @Nested
    @DisplayName("containsThreat with NoOpInputSanitizer")
    class ContainsThreatWithNoOp {

        @Test
        @DisplayName("should not detect threats when using NoOpInputSanitizer")
        void shouldNotDetectThreats_whenUsingNoOpInputSanitizer() {
            InputSecurityChecker noOp = new NoOpInputSanitizer();
            XssChecker checker = new XssChecker(noOp);

            // NoOp always returns false
            assertThat(checker.containsThreat("<script>alert('xss')</script>")).isFalse();
            assertThat(checker.containsThreat("Hello World")).isFalse();
        }
    }

    @Nested
    @DisplayName("deprecated constructors")
    class DeprecatedConstructors {

        @Test
        @DisplayName("should work with deprecated EnhancedInputSanitizer constructor")
        @SuppressWarnings("deprecation")
        void shouldWorkWithDeprecatedEnhancedInputSanitizerConstructor() {
            io.github.afgprojects.framework.core.config.AfgCoreProperties properties =
                    new io.github.afgprojects.framework.core.config.AfgCoreProperties();
            EnhancedInputSanitizer sanitizer = new EnhancedInputSanitizer(properties);
            XssChecker checker = new XssChecker(sanitizer);

            assertThat(checker.containsThreat("<script>alert(1)</script>")).isTrue();
            assertThat(checker.containsThreat("Hello World")).isFalse();
        }

        @Test
        @DisplayName("should work with deprecated EnhancedInputSanitizer constructor when null")
        @SuppressWarnings("deprecation")
        void shouldWorkWithDeprecatedEnhancedInputSanitizerConstructor_whenNull() {
            XssChecker checker = new XssChecker((EnhancedInputSanitizer) null);

            // Falls back to regex
            assertThat(checker.containsThreat("<script>alert('xss')</script>")).isTrue();
        }
    }
}
