package io.github.afgprojects.framework.core.web.security.filter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SqlInjectionChecker")
class SqlInjectionCheckerTest {

    @Nested
    @DisplayName("getName")
    class GetName {

        @Test
        @DisplayName("should return SQL_INJECTION")
        void shouldReturnSqlInjection() {
            SqlInjectionChecker checker = new SqlInjectionChecker();
            assertThat(checker.getName()).isEqualTo("SQL_INJECTION");
        }
    }

    @Nested
    @DisplayName("containsThreat without enhanced sanitizer")
    class ContainsThreatWithoutEnhanced {

        private final SqlInjectionChecker checker = new SqlInjectionChecker();

        @Test
        @DisplayName("should detect OR injection via InputSanitizer")
        void shouldDetectOrInjection_viaInputSanitizer() {
            assertThat(checker.containsThreat("' OR 1=1")).isTrue();
        }

        @Test
        @DisplayName("should detect UNION SELECT via InputSanitizer")
        void shouldDetectUnionSelect_viaInputSanitizer() {
            assertThat(checker.containsThreat("UNION SELECT * FROM users")).isTrue();
        }

        @Test
        @DisplayName("should detect DROP TABLE via InputSanitizer")
        void shouldDetectDropTable_viaInputSanitizer() {
            assertThat(checker.containsThreat("DROP TABLE users")).isTrue();
        }

        @Test
        @DisplayName("should return false for safe text via InputSanitizer")
        void shouldReturnFalse_forSafeText_viaInputSanitizer() {
            assertThat(checker.containsThreat("Hello World")).isFalse();
        }
    }
}